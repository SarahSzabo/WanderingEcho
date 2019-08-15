/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Snapshot;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Subvolume;
import com.protonmail.sarahszabo.wanderingecho.ui.UI;
import static com.protonmail.sarahszabo.wanderingecho.util.EchoUtil.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.scene.control.ButtonType;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

/**
 * The master BTRFS class that represents what is possible using the BTRFS
 * utility. A utility class that is useful in automating a lot of the work that
 * the filesystem does.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class BTRFS {

    /**
     * The folder that @ and @home mounts to.
     */
    private static final Path MOUNTING_FOLDER = Paths.get("/media/Wandering_Echo");

    /**
     * The folder that snapshots are stored in.
     */
    private static final Path ROOT_SNAPSHOT_FOLDER = MOUNTING_FOLDER.resolve("Snapshots");

    /**
     * The typical backup folder for backups.
     */
    private static final Path BACKUP_FOLDER;

    /**
     * The path to the configuration folder for system configuration files
     */
    private static final Path CONFIGURATION_FOLDER = Paths.get(System.getProperty("user.home"), ".Wandering Echo")
            .resolve("System Configuration");

    /**
     * The BTRFS config file.
     */
    private static final Path BTRFS_CONFIG_FILE = CONFIGURATION_FOLDER.resolve("BTRFS Config.json");

    /**
     * A list of subvolumes to snapshot.
     */
    private static final SubvolumeList SUBVOLUME_LIST;

    /**
     * A value representing whether or not the root file-system is mounted or
     * not
     */
    private static final AtomicBoolean ROOT_FILESYSTEM_MOUNTED = new AtomicBoolean(false);

    /**
     * The object mapper for Jackson JSON operations.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The separator to be used in filenames to seperate the subvolume and date.
     */
    public static final String SNAPSHOT_SEPARATOR = "___";

    private static final ExecutorService executor = Executors
            .newCachedThreadPool(new BasicThreadFactory.Builder().daemon(true).namingPattern("Wandering Echo BTRFS Task Thread %d").build());

    /**
     * Class wide logger.
     */
    private static final Logger LOG = Logger.getLogger(BTRFS.class.getName());

    static {
        MAPPER.findAndRegisterModules();
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        try ( var scanner = getProcessInputScanner(processOPNoWait(false, "whoami"))) {
            while (scanner.hasNext()) {
                if (!scanner.next().equalsIgnoreCase("root")) {
                    messageThenExit("We aren't root!, Run using sudo for root level");
                }
            }
            //Create snapshots and mounting folders
            Files.createDirectories(ROOT_SNAPSHOT_FOLDER);
            //If config doesn't exist, make a new one
            if (Files.notExists(BTRFS_CONFIG_FILE)) {
                Files.createDirectories(CONFIGURATION_FOLDER);
                Files.createFile(BTRFS_CONFIG_FILE);
                //Ask User Which Subvolumes they Want to Save
                boolean moreSubvolumes = true;
                //Initialize subvolume list
                SUBVOLUME_LIST = new SubvolumeList();
                LOG.info("Subvolume List not Detected, Generating a New List");
                do {
                    //Only add to path list if we have something
                    UI.getDirectory("Choose a Subvoume for Backing Up, but not @ or @home").ifPresent(path
                            -> SUBVOLUME_LIST.add(new Subvolume(path)));
                    //Don't continue is any match the close/no buttons
                    moreSubvolumes = !UI.showConfirmationDialog("Confirmation: More Snapshots?",
                            "Are there there more directories to snapshot?").stream().anyMatch(button
                                    -> button == ButtonType.CANCEL || button == ButtonType.CLOSE || button == ButtonType.NO);

                } while (moreSubvolumes);
                BACKUP_FOLDER = UI.getDirectory("Select a Directory for Backups to Appear In").get();
                //Write Config File
                MAPPER.writeValue(BTRFS_CONFIG_FILE.toFile(),
                        new BTRFSConfig(SUBVOLUME_LIST, BACKUP_FOLDER));
                /*System.out.println("\n\n");
                MAPPER.writeValue(System.out, SUBVOLUME_LIST);
                System.out.println("\n\n\n");
                var s = MAPPER.writeValueAsString(SUBVOLUME_LIST);
                List<Subvolume> list = MAPPER.readValue(s, new TypeReference<List<Subvolume>>() {
                });*/
            } else {
                var config = MAPPER.readValue(BTRFS_CONFIG_FILE.toFile(), BTRFSConfig.class);
                SUBVOLUME_LIST = config.getSubvolumes();
                BACKUP_FOLDER = config.getTypicalBackupLocation();
                LOG.info("Subvolume List Detected: " + SUBVOLUME_LIST);
            }
        } catch (IOException ex) {
            Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        try {
            BTRFS.mountRootFilesystem();
        } catch (IOException ex) {
            Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Filesystem couldn't be mounted", ex);
        }
        //Add BTRFS System & User Subvolumes
        SUBVOLUME_LIST.add(new Subvolume(BTRFS.MOUNTING_FOLDER.resolve("@")));
        SUBVOLUME_LIST.add(new Subvolume(BTRFS.MOUNTING_FOLDER.resolve("@home")));
    }

    /**
     * Gets the string that you send, but in quotes. Example: s -> "s". Uses
     * quote literals.
     *
     * @param str The input string as an object
     * @return The string in quotes
     */
    public static String stringInQuotes(Object str) {
        return "\"" + str + "\"";
    }

    /**
     * Purges the entire snapshot cache.
     *
     * @param alsoBackups If true, also purges the backup cache
     * @throws IOException If something happened
     */
    public static void purgeSnapshots(boolean alsoBackups) throws IOException {
        var scriptPath = Files.createTempFile("Wandering Echo Subvolume Delete Script", ".sh");
        var string = "btrfs subvolume delete ";
        //Make the rest of the delete string SNAPFOLDER1/* SNAPFOLDER2/*
        string += SUBVOLUME_LIST.stream().map((subvolume) -> {
            try {
                return stringInQuotes(BTRFS.configureSnapshotFilesystem(subvolume).toString() + "/*");
            } catch (IOException ex) {
                Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException("Couldn't get snapshot file system", ex);
            }
            //TODO: Every subvolume should know where its snapshot folder is.
        }).collect(Collectors.joining(" ")) + (alsoBackups ? " " + stringInQuotes(BACKUP_FOLDER + "/*") : "");
        Files.write(scriptPath, string.getBytes(), StandardOpenOption.CREATE);
        processOP(true, "bash", scriptPath.toString());
    }

    /**
     * Gets the previous snapshot of a certain subvolume.
     *
     * @param snapshot The snapshot to find the earlier version of
     * @return The earlier version
     */
    private static Snapshot detectPreviousBackupOf(Snapshot snapshot) {
        try {
            var pathStream = Files.list(ROOT_SNAPSHOT_FOLDER);
            //Get all dates after the triple underscore & filter by name (if we're looking at @, only get @ subvolumes),
            //also ifnore selected subvolume's snapshot
            var snapshotList = pathStream.parallel().map(Snapshot::new).filter(snap -> snap.getName()
                    .equalsIgnoreCase(snapshot.getName()) && !snap.equals(snapshot)).collect(Collectors.toList());
            //Sort by earliest creation date, we want the latest
            snapshotList.sort((snap0, snap1) -> snap0.getCreationDate().compareTo(snap1.getCreationDate()));
            //Get the latest creation date
            return snapshotList.isEmpty() ? null : snapshotList.get(snapshotList.size() - 1);
        } catch (IOException ex) {
            Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Turns all available snapshots (Including the @ and @home subvolumes into
     * backups to a location of the user's choosing.
     *
     * @throws java.io.IOException If something happened
     */
    public static void commenceBackupOperation() throws IOException {
        //Do Snapshots
        var snapshots = new ArrayList<Snapshot>(SUBVOLUME_LIST.size());
        SUBVOLUME_LIST.stream().parallel().map(subvolume -> subvolume.snapshot()).forEach(snapshot -> {
            snapshots.add(snapshot);
            snapshot.create();
        });
        snapshots.stream().forEach(snapshot -> {
            Snapshot parent = null;
            //Ask User if there is a parent or not
            var scanner = getSystemInputScanner();
            LOG.info("Is there a parent for the snapshot of " + snapshot.getFullFileName() + " ? (Yes / Y / No / N)");
            var response = scanner.nextLine();
            if (response.equalsIgnoreCase("Yes") || response.equalsIgnoreCase("Y")) {
                //Guess Parent
                var guess = detectPreviousBackupOf(snapshot);
                LOG.info("Is this parent correct?" + guess.getFullFileName());
                response = scanner.nextLine();
                if (response.equalsIgnoreCase("Yes") || response.equalsIgnoreCase("Y")) {
                    parent = guess;
                } else {
                    parent = new Snapshot(UI.getDirectory("Choose an Already Existing Snapshot",
                            ROOT_SNAPSHOT_FOLDER).orElse(null));
                }
            }
            //Don' need to find parent, null is ok
            final var localParent = parent;
            executor.submit(() -> snapshot.backup(localParent, BACKUP_FOLDER));
        });
    }

    /**
     * Configures a subvolume's drive for snapshots and returns the directory of
     * the snapshots folder. Creates the snapshots directory on the drive.
     *
     * @param subvolume The folder on the hard drive to configure
     * @return The location of the Snapshots folder
     */
    public static Path configureSnapshotFilesystem(Subvolume subvolume) throws IOException {
        //We want to get the parent because this command has a strange output ON the subvolume itself
        //Command: df --output=target /media/sarah/drive/Snapshots
        try ( var scanner = getProcessInputScanner(processOPNoWait(false, "df", "--output=target",
                subvolume.getLocation().getParent().toString()))) {
            //Throw out first line, not helpful information, the path is on the second
            scanner.nextLine();
            Path localFilesystemRoot = Paths.get(scanner.nextLine());
            return Files.createDirectories(localFilesystemRoot.resolve("Snapshots"));
        }
    }

    /**
     * Mounts the root filesystem or does nothing if already mounted
     */
    public static void mountRootFilesystem() throws IOException {
        if (!ROOT_FILESYSTEM_MOUNTED.get()) {
            //Command: findmnt / -o UUID
            var process = processOPNoWait(false, "findmnt", "/", "-o", "UUID");
            var scanner = new Scanner(process.getInputStream());
            //What we want is on second line
            scanner.nextLine();
            String rootUUID = scanner.nextLine();
            LOG.info("Mounting Scanner Root ID = " + rootUUID);
            //Mount by UUID
            process = processOPNoWait(false, "mount", "-U", rootUUID, MOUNTING_FOLDER.toString());
            scanner = new Scanner(process.getErrorStream());
            while (scanner.hasNextLine()) {
                LOG.info(scanner.nextLine());
            }
            LOG.info("Filesystem Mounted!");
        }
    }

    /**
     * Gets the subvolume list that was saved earlier.
     *
     * @return the list of subvolumes saved that we are to snapshot
     */
    public static List<Subvolume> getSubvolumeList() {
        return new ArrayList<>(SUBVOLUME_LIST);
    }

    /**
     * Gets the instance of BTRFS.
     *
     * @return The instance
     */
    public static BTRFS getInstance() {
        return BTRFSHolder.INSTANCE;
    }

    /**
     * No public instances.
     */
    private BTRFS() {
    }

    /**
     * A helper class that is serialized and saves the system state.
     */
    private static class BTRFSConfig {

        @JsonProperty(value = "subvolumes")
        private final SubvolumeList subvolumes;
        @JsonProperty(value = "typicalBackupLocation")
        private final Path typicalBackupLocation;

        @JsonCreator
        public BTRFSConfig(@JsonProperty(value = "subvolumes") SubvolumeList subvolumes,
                @JsonProperty(value = "typicalBackupLocation") Path typicalBackupLocation) {
            this.subvolumes = subvolumes;
            this.typicalBackupLocation = typicalBackupLocation;
        }

        public SubvolumeList getSubvolumes() {
            return this.subvolumes;
        }

        public Path getTypicalBackupLocation() {
            return this.typicalBackupLocation;
        }

    }

    /**
     * Wrapper class for Jackson serialization.
     */
    private static class SubvolumeList extends ArrayList<Subvolume> {

        private static final long serialVersionUID = 1L;
    }

    /**
     * Holder class for the instance.
     */
    private static class BTRFSHolder {

        /**
         * Only instance
         */
        private static final BTRFS INSTANCE = new BTRFS();
    }
}
