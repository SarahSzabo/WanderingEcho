/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs;

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
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.control.ButtonType;

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
    private static final Path SNAPSHOT_FOLDER = MOUNTING_FOLDER.resolve("Snapshots");

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
    private static final List<Subvolume> SNAPSHOT_LIST = null;

    /**
     * A value representing whether or not the root file-system is mounted or
     * not
     */
    private static final AtomicBoolean ROOT_FILESYSTEM_MOUNTED = new AtomicBoolean(false);
    private static final Logger LOG = Logger.getLogger(BTRFS.class.getName());

    static {
        //TODO Make JSON/YAML configurations for folders
        try {
            //Create snapshots and mounting folders
            Files.createDirectories(SNAPSHOT_FOLDER);
            //If config doesn't exist, make a new one
            if (Files.notExists(BTRFS_CONFIG_FILE)) {
                Files.createDirectories(CONFIGURATION_FOLDER);
                Files.createFile(BTRFS_CONFIG_FILE);
                //TODO: Make subvol list not = null
                //Ask User Which Subvolumes they Want to Save
                List<Path> list = new ArrayList<>(10);
                boolean moreSubvolumes = true;
                do {
                    //Only add to path list if we have something
                    UI.getDirectory("Choose a Subvoume for Backing Up, but not @ or @home").ifPresent(path -> list.add(path));
                    //Don't continue is any match the close/no buttons
                    moreSubvolumes = !UI.showConfirmationDialog("Confirmation: More Snapshots?",
                            "Are there there more directories to snapshot?").stream().anyMatch(button
                                    -> button == ButtonType.CANCEL || button == ButtonType.CLOSE || button == ButtonType.NO);

                } while (moreSubvolumes);
                //Mount Filesystem
                mountRootFilesystem();
            } else {
                //TODO: When we read from the file
            }
        } catch (IOException ex) {
            Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Mounts the root filesystem or does nothing if already mounted
     */
    public static void mountRootFilesystem() {
        if (!ROOT_FILESYSTEM_MOUNTED.get()) {
            try {
                //Command: findmnt / -o UUID
                var scanner = new Scanner(System.in);
                processOP(true, "findmnt", "/", "-o", "UUID");
                String rootUUID = scanner.nextLine();
                LOG.fine("Mounting Scanner Root ID = " + rootUUID);
                //Mount by UUID
                processOP(true, "mount", "-U ", rootUUID, MOUNTING_FOLDER.toAbsolutePath().toString());
                System.out.println("Filesystem Mounted!");
            } catch (IOException ex) {
                Logger.getLogger(BTRFS.class.getName()).log(Level.SEVERE, null, ex);
                throw new IllegalStateException(ex);
            }
        }
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

    public Subvolume createSubvolume(String name) {
        throw new UnsupportedOperationException("Not yet supported");
    }

    public boolean deleteSubvolume(Subvolume subvolume) {
        throw new UnsupportedOperationException("Not yet supported");
    }

    public Snapshot createSnapshot() {
        throw new UnsupportedOperationException("Not yet supported");
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
