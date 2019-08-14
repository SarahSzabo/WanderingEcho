/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho;

import com.protonmail.sarahszabo.wanderingecho.btrfs.BTRFS;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Snapshot;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Subvolume;
import com.protonmail.sarahszabo.wanderingecho.util.EchoUtil;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javafx.application.Platform;

/**
 * The Class that holds the main method.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class Init {

    /**
     * The name of the program.
     */
    public static final String PROGRAM_NAME = "Wandering Echo";
    /**
     * The version number of the program.
     */
    public static final String PROGRAM_VERSION = "0.1α";

    /**
     * The version number and name of the program.
     */
    public static final String PROGRAM_FULL_NAME = PROGRAM_NAME + " " + PROGRAM_VERSION;

    private static final Logger LOG = Logger.getLogger(Init.class.getName());

    static {
        //Set global logger configuration
        Arrays.asList(LogManager.getLogManager().getLogger("").getHandlers()).stream()
                .parallel().forEach(handler -> {
                    handler.setFormatter(new EchoUtil.LoggerFormatter());
                });
    }

    /**
     * Prints the generic error message for the input command not being
     * recognized and then shuts the program down.
     */
    private static void printGenericCommandNotRecognized() {
        EchoUtil.messageThenExit("Command not recognized, shutting down.\n\n"
                + "OPTIONS: Backup, Delete_Cache");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            if (args.length >= 1) {
                //We're Backing Up Data
                if (args[0].equalsIgnoreCase("Backup")) {
                    BTRFS.commenceBackupOperation();
                } //Delete the System Cache, and Possibly Backups as Well
                else if (args[0].equalsIgnoreCase("Delete_Cache")) {
                    if (args.length >= 2) {
                        //Get boolean value from string
                        boolean value = Boolean.parseBoolean(args[1]);
                        BTRFS.purgeSnapshots(value);
                    } else {
                        EchoUtil.messageThenExit("COMMAND FORMAT: Delete_Cache "
                                + "(true / false value here for deleting backups as well)");
                    }
                } //User Input Incorrect Print Commands
                else {
                    printGenericCommandNotRecognized();
                }
            } //User Input Incorrect Print Commands
            else {
                printGenericCommandNotRecognized();
            }
            //Shutdown JFX Platform & Exit Gracefully
            Platform.exit();
            System.exit(0);
            //TODO: Test Delete Snapshots/Backups
            //TODO: Send over Network
            //TODO: Add Check for if Snapshots are over 7 days old, never delete last snapshot
        } catch (IOException e) {
            LOG.severe(e.toString());
            throw new IllegalStateException(e);
        }
    }

}
