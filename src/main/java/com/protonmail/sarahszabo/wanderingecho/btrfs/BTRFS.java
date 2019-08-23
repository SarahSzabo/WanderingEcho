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
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Backup;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Snapshot;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Subvolume;
import com.protonmail.sarahszabo.wanderingecho.ui.UI;
import static com.protonmail.sarahszabo.wanderingecho.util.EchoUtil.*;
import com.protonmail.sarahszabo.wanderingecho.util.PathDeserializer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.concurrent.Callable;
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
    private static Path BACKUP_FOLDER;

    /**
     * The path to the configuration folder for system configuration files
     */
    private static final Path CONFIGURATION_FOLDER = Paths.get(System.getProperty("user.home"), ".Wandering Echo")
            .resolve("System Configuration");

    /**
     * The folder that stores files of our snapshot objects that share the same
     * filename as the ones in the snapshots folders.
     */
    private static final Path SNAPSHOT_ASSOCIATION_FOLDER = CONFIGURATION_FOLDER.resolve("Snapshot Association Folder");

    /**
     * The BTRFS config file.
     */
    private static final Path BTRFS_CONFIG_FILE = CONFIGURATION_FOLDER.resolve("BTRFS Config.json");

    /**
     * A list of subvolumes to snapshot.
     */
    private static SubvolumeList SUBVOLUME_LIST;

    /**
     * A map of the backups by path. Give a path on the disk, get a backup.
     */
    private static BackupMap backupMap;

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
            .newCachedThreadPool(new BasicThreadFactory.Builder().namingPattern("Wandering Echo BTRFS Task Thread %d").build());

    /**
     * Class wide logger.
     */
    private static final Logger LOG = Logger.getLogger(BTRFS.class.getName());

    /**
     * The BTRFS instance
     */
    private static BTRFS INSTANCE;

    static {
        //Register JDK8 time & date mapping modules for correct serialization
        MAPPER.findAndRegisterModules();
        //Indentation is nice :)
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        //Enable Path Deserialization
        SimpleModule module = new SimpleModule("Path Deserializer Module");
        module.addKeyDeserializer(Path.class, new PathDeserializer());
        MAPPER.registerModule(module);
        //Check for root
        try ( var scanner = getProcessInputScanner(processOPNoWait(false, "whoami"))) {
            while (scanner.hasNext()) {
                if (!scanner.next().equalsIgnoreCase("root")) {
                    messageThenExit("We aren't root!, Run using sudo for root level");
                } else {
                    LOG.info("We're root");
                }
            }
            //Create snapshots and mounting folders
            Files.createDirectories(ROOT_SNAPSHOT_FOLDER);
            Files.createDirectories(SNAPSHOT_ASSOCIATION_FOLDER);

        } catch (IOException ex) {
            Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        //Add unmount thread
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                //Unmount to be nice
                unmountRootFilesystem();
            } catch (IOException ex) {
                Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException("Couldn't Unmount Filesystem!");
            }
        }, "Wandering Echo Root Filesystem Unmounter Thread"));
        //Add thread to save final config file state
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                //Only do the operation if the config file exists (We might have called system reset
                if (Files.exists(BTRFS_CONFIG_FILE)) {
                    var newSubvolumeList = new SubvolumeList();
                    SUBVOLUME_LIST.stream().filter(sub -> !sub.getName().equals("@")
                            && !sub.getName().equalsIgnoreCase("@home")).forEach(sub -> newSubvolumeList.add(sub));
                    MAPPER.writeValue(BTRFS_CONFIG_FILE.toFile(), new BTRFSConfig(newSubvolumeList, BACKUP_FOLDER, backupMap));
                }
            } catch (IOException ex) {
                Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException("Couldn't write config file as a part of the shutdown hook", ex);
            }
        }, "Wandering Echo BTRFS Config Writer Thread"));
    }

    /**
     * Adds a backup to the internal backup registry.
     *
     * @param backup The backup to add
     */
    public static void addBackup(Backup backup) {
        backupMap.put(backup.getParentSnapshot().getParentSubvolume().getLocation(), backup);
    }

    /**
     * Deletes the BTRFS configuration file to reset the configuration.
     *
     * @throws IOException Is the delete failed
     */
    public static void resetConfiguration() throws IOException {
        INSTANCE = null;
        Files.deleteIfExists(BTRFS_CONFIG_FILE);
        LOG.info("System Configuration Reset");
    }

    /**
     * Checks the configuration before allowing access to subroutines. No
     * exceptions will be thrown.
     */
    private static boolean isConfigured() {
        return INSTANCE != null;

    }

    /**
     * Checks the configuration before allowing access to subroutines.
     *
     * @throws NotConfiguredExeption If we're not configured
     */
    private static void checkConfiguration() {
        if (INSTANCE == null) {
            throw new NotConfiguredExeption("System not Configured, call getInstance() before using methods");
        }
    }

    /**
     * Gets the instance of BTRFS. Also runs set up routines.
     *
     * @return
     * @throws java.io.IOException If something went wrong
     */
    public static BTRFS getInstance() throws IOException {
        //We're null, run initialization routine
        if (INSTANCE == null) {
            //If config doesn't exist, make a new one
            if (Files.notExists(BTRFS_CONFIG_FILE)) {
                Files.createDirectories(CONFIGURATION_FOLDER);
                Files.createFile(BTRFS_CONFIG_FILE);
                //Ask User Which Subvolumes they Want to Save
                boolean moreSubvolumes = true;
                //Initialize subvolume list
                SUBVOLUME_LIST = new SubvolumeList();
                backupMap = new BackupMap();
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
                        new BTRFSConfig(SUBVOLUME_LIST, BACKUP_FOLDER, backupMap));
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
                backupMap = config.getBackupMap();
                LOG.info("Subvolume List Detected: " + SUBVOLUME_LIST);
            }
            BTRFS.mountRootFilesystem();
            //Add BTRFS System & User Subvolumes
            SUBVOLUME_LIST.add(new Subvolume(BTRFS.MOUNTING_FOLDER.resolve("@")));
            SUBVOLUME_LIST.add(new Subvolume(BTRFS.MOUNTING_FOLDER.resolve("@home")));
            //Initialize BTRFS
            INSTANCE = new BTRFS();
        }
        return INSTANCE;
    }

    /**
     * Sends backups over SSH.
     */
    public static void sendBackupsOverSSH() {
        //Ensure we're initialized
        checkConfiguration();
        //Get sorted set of backup objects sorted by date
        var backupValuesSortedByDate = new TreeSet<Backup>(backupMap.values());
        //Since all snapshots are backed up at the same time, the results are the top 3
        var backupStream = backupValuesSortedByDate.stream().limit(3);
        var scriptPathList = new ArrayList<Path>(SUBVOLUME_LIST.size());
        //Record backup script file locations for parallel send
        backupStream.forEach(backup -> {
            try {
                //Generate backup script for each backup since we can't do it directly
                var scriptPath = Files.createTempFile("Wandering Echo Temporary SSH Script for " + backup, ".sh");
                Files.write(scriptPath, ("btrfs send " + backup.getLocation().toString() + " | SSH SciLab0 "
                        + stringInQuotes("btrfs receive /media/sarah/SENTINEL")).getBytes());
                scriptPathList.add(scriptPath);
                //TODO: Make a general version of this before mainline release
            } catch (IOException ex) {
                Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException("Something went wrong in the SSH back stream block", ex);
            }
        });
        //Send backups
        scriptPathList.parallelStream().forEach(scriptPath -> processOPNoWaitNOE(true, "bash " + scriptPath));
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
     * Gets a snapshot from the snapshot association folder. Doesn't throw an
     * exception if an I/O error occurred.
     *
     * @param existingSnapshot The existing BTRFS snapshot on the disk
     * @return The snapshot object of the BTRFS snapshot
     */
    public static Snapshot getStoredSnapshotNOE(Path existingSnapshot) {
        //Ensure we're initialized
        checkConfiguration();
        try {
            return MAPPER.readValue(SNAPSHOT_ASSOCIATION_FOLDER.resolve(existingSnapshot.getFileName()).toFile(),
                    Snapshot.class);
        } catch (IOException ex) {
            Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Unable to resolve snapshot from BTRFS snapshot", ex);
        }
    }

    /**
     * Gets a snapshot from the snapshot association folder.
     *
     * @param existingSnapshot The existing BTRFS snapshot on the disk
     * @return The snapshot object of the BTRFS snapshot
     * @throws IOException If something went wrong
     */
    public static Snapshot getStoredSnapshot(Path existingSnapshot) throws IOException {
        //Ensure we're initialized
        checkConfiguration();
        return MAPPER.readValue(SNAPSHOT_ASSOCIATION_FOLDER.resolve(existingSnapshot.getFileName() + ".JSON").toFile(),
                Snapshot.class);
    }

    /**
     * Saves a snapshot to the snapshot association folder for associating the
     * BTRFS snapshot with our Snapshot objects.
     *
     * @param snapshot The snapshot to save
     * @throws IOException If something went wrong
     */
    public static void saveSnapshot(Snapshot snapshot) throws IOException {
        //Ensure we're initialized
        checkConfiguration();
        MAPPER.writeValue(SNAPSHOT_ASSOCIATION_FOLDER.resolve(snapshot.getLocation().getFileName() + ".JSON").toFile(), snapshot);
    }

    /**
     * Purges the entire snapshot cache.
     *
     * @param alsoBackups If true, also purges the backup cache
     * @throws IOException If something happened
     */
    public static void purgeSnapshots(boolean alsoBackups) throws IOException {
        //Ensure we're initialized
        checkConfiguration();
        var scriptPath = Files.createTempFile("Wandering Echo Snapshot Delete Script", ".sh");
        var string = "btrfs subvolume delete -C ";
        //Make the rest of the delete string SNAPFOLDER1/* SNAPFOLDER2/*
        string += SUBVOLUME_LIST.stream().map((subvolume) -> {
            try {
                return BTRFS.configureSnapshotFilesystem(subvolume).toString() + "/*";
            } catch (IOException ex) {
                Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException("Couldn't get snapshot file system", ex);
            }
        }).distinct().collect(Collectors.joining(" ")) + (alsoBackups ? " " + BACKUP_FOLDER + "/*" : "");
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
            var snapshotList = pathStream.parallel().map(BTRFS::getStoredSnapshotNOE).filter(snap -> snap.getName()
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
        checkConfiguration();
        //Do Snapshots
        var snapshots = new ArrayList<Snapshot>(SUBVOLUME_LIST.size());
        SUBVOLUME_LIST.stream().parallel().map(subvolume -> subvolume.snapshot()).forEach(snapshot -> {
            snapshots.add(snapshot);
            snapshot.create();
        });
        var callableList = new ArrayList<Callable<Backup>>(3);
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
                    parent = BTRFS.getStoredSnapshotNOE(UI.getDirectory("Choose an Already Existing Snapshot",
                            ROOT_SNAPSHOT_FOLDER).orElse(null));
                }
            }
            //Don' need to find parent, null is ok
            final var localParent = parent;
            //Add to List of Tasks to Launch
            callableList.add(() -> snapshot.backup(localParent, BACKUP_FOLDER));
        });
        try {
            //Execute List of Tasks
            executor.invokeAll(callableList);
        } catch (InterruptedException ex) {
            Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("We've been interrupted while waiting for the BTRFS backup tasks to complete!", ex);
        }
    }

    /**
     * Configures a subvolume's drive for snapshots and returns the directory of
     * the snapshots folder.Creates the snapshots directory on the drive.
     *
     * @param subvolume The folder on the hard drive to configure
     * @return The location of the Snapshots folder
     * @throws java.io.IOException
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
     * Unmounts the root filesystem. Does nothing if not mounted.
     *
     * @throws IOException If something went wrong
     */
    public static void unmountRootFilesystem() throws IOException {
        //Should we ever need it, unmounting by UUID:
        /*$ findmnt -rn -S UUID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx -o TARGET
        /mnt/mountpoint*/
        //OR: umount /dev/disk/by-uuid/$UUI
        //Check if filesystem mounted, if so, unmount
        if (ROOT_FILESYSTEM_MOUNTED.get()) {
            processOP("umount", MOUNTING_FOLDER.toString());
            LOG.info("Filesystem Unmounted!");
        }
    }

    /**
     * Mounts the root filesystem or does nothing if already mounted
     *
     * @throws java.io.IOException If something went wrong
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
        //Ensure we're initialized
        checkConfiguration();
        return new ArrayList<>(SUBVOLUME_LIST);
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

        @JsonProperty
        private final SubvolumeList subvolumes;
        @JsonProperty
        private final Path typicalBackupLocation;
        @JsonProperty
        private final BackupMap backupMap;

        @JsonCreator
        BTRFSConfig(@JsonProperty(value = "subvolumes") SubvolumeList subvolumes,
                @JsonProperty(value = "typicalBackupLocation") Path typicalBackupLocation,
                @JsonProperty(value = "backupMap") BackupMap backupMap) {
            this.subvolumes = subvolumes;
            this.typicalBackupLocation = typicalBackupLocation;
            this.backupMap = backupMap;
        }

        /**
         * Gets the previous subvolume list.
         *
         * @return The list
         */
        public SubvolumeList getSubvolumes() {
            return this.subvolumes;
        }

        /**
         * Gets the previous external backup location for the program.
         *
         * @return The location
         */
        public Path getTypicalBackupLocation() {
            return this.typicalBackupLocation;
        }

        /**
         * Gets the previous backup map
         *
         * @return The map
         */
        public BackupMap getBackupMap() {
            return this.backupMap;
        }

    }

    /**
     * Wrapper class for Jackson serialization.
     */
    private static class BackupMap extends HashMap<Path, Backup> {

        private static final long serialVersionUID = 1L;
    }

    /**
     * Wrapper class for Jackson serialization.
     */
    private static class SubvolumeList extends ArrayList<Subvolume> {

        private static final long serialVersionUID = 1L;
    }
}
