/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho;

import com.protonmail.sarahszabo.wanderingecho.btrfs.BTRFS;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Snapshot;
import com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume.Subvolume;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        BTRFS.commenceBackupOperation();
        //Shutdown JFX Platform & Exit Gracefully
        Platform.exit();
        System.exit(0);
    }
    private static final Logger LOG = Logger.getLogger(Init.class.getName());

}
