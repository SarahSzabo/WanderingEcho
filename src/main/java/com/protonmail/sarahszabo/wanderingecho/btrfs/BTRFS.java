/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Backup;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Snapshot;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Subvolume;
import com.protonmail.sarahszabo.wanderingecho.ui.UI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.protonmail.sarahszabo.wanderingecho.util.EchoUtil.*;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final Path CONFIGURATION_FOLDER = Paths.get("System Configuration");

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
     * Class wide logger.
     */
    private static final Logger LOG = Logger.getLogger(BTRFS.class.getName());

    static {
        try (var scanner = getProcessInputScanner(processOPNoWait(false, "whoami"))) {
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
                //TODO: Make subvol list not = null
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
    }

    /**
     * Turns all available snapshots (Including the @ and @home subvolumes into
     * backups to a location of the user's choosing.
     */
    public static void commenceBackupOperation() {
        //Ask if User wants to take a snapshot/backup of the system
//TODO: implement this
//System.out.println("Would you like to create a new snapshot/backup?");
        //var scanner = new Scanner(System.in);
        //var response = scanner.nextLine();
        if (true/*response.equalsIgnoreCase("Yes")*/) {
            //Do backup
            var list = BTRFS.getSubvolumeList();
            BTRFS.mountRootFilesystem();
            //Add BTRFS System & User Subvolumes
            list.add(new Subvolume(BTRFS.MOUNTING_FOLDER.resolve("@")));
            list.add(new Subvolume(BTRFS.MOUNTING_FOLDER.resolve("@home")));
            //Do Snapshots
            var snapshots = new ArrayList<Snapshot>(list.size());
            list.stream().parallel().map(subvolume -> subvolume.snapshot()).forEach(snapshot -> {
                snapshots.add(snapshot);
                snapshot.create();
            });
            snapshots.stream().parallel().forEach(snapshot -> snapshot.backup(BACKUP_FOLDER));
        }
    }

    /**
     * Configures a subvolume's drive for snapshots and returns the directory of
     * the snapshots folder. Creates the snapshots directory on the drive.
     *
     * @param subvolume The folder on the hard drive to configure
     * @return The location of the Snapshots folder
     */
    public static Path configureSnapshotFilesystem(Subvolume subvolume) {
        //We want to get the parent because this command has a strange output ON the subvolume itself
        //Command: df --output=target /media/sarah/drive/Snapshots
        try (var scanner = getProcessInputScanner(processOPNoWait(false, "df", "--output=target",
                subvolume.getLocation().getParent().toString()))) {
            //Throw out first line, not helpful information, the path is on the second
            scanner.nextLine();
            Path localFilesystemRoot = Paths.get(scanner.nextLine());
            return Files.createDirectories(localFilesystemRoot.resolve("Snapshots"));
        } catch (IOException ex) {
            Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Mounts the root filesystem or does nothing if already mounted
     */
    public static void mountRootFilesystem() {
        if (!ROOT_FILESYSTEM_MOUNTED.get()) {
            try {
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
                System.out.println("Filesystem Mounted!");
            } catch (IOException ex) {
                Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException(ex);
            }
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
