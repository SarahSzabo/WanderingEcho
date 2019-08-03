/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.ui;

import com.protonmail.sarahszabo.wanderingecho.Init;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;

/**
 * A class that represents the UI for this program.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public final class UI {

    private static final BlockingQueue<Path> pathQueue = new ArrayBlockingQueue<>(1);
    private static final BlockingQueue<Optional<String>> textQueue = new ArrayBlockingQueue<>(1);

    private UI() {
    }

    /**
     * Shows a confirmation dialog with a message and title.
     *
     * @param title The title of the dialog
     * @param message The message of the dialog
     * @return The button choice
     */
    public static Optional<ButtonType> showConfirmationDialog(String title, String message) {
        try {
            BlockingQueue<Optional<ButtonType>> queue = new ArrayBlockingQueue<>(1);
            Platform.runLater(() -> {
                try {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle(title);
                    alert.setContentText(message);
                    queue.put(alert.showAndWait());
                } catch (InterruptedException ex) {
                    Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
                    throw new IllegalStateException(ex);
                }
            });
            return queue.take();
        } catch (InterruptedException ex) {
            Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Gets user input based on a title and text in the content area.
     *
     * @param title The title of the dialog
     * @param contextText The text in the content area
     * @return The optional of what happened in the dialog
     */
    public static Optional<String> getUserInput(String title, String contextText) {
        try {
            Platform.runLater(() -> {
                try {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle(title);
                    dialog.setContentText(contextText);
                    textQueue.put(dialog.showAndWait());
                } catch (InterruptedException ex) {
                    Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
                    throw new IllegalStateException(ex);
                }
            });
            return textQueue.take();
        } catch (InterruptedException ex) {
            Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Gets a directory based on the string submitted as the title for the
     * chooser.
     *
     * @param title The title of this chooser
     * @return The directory
     */
    public static Optional<Path> getDirectory(String title) {
        try {
            Platform.runLater(() -> {
                try {
                    var chooser = new DirectoryChooser();
                    chooser.setTitle(Init.PROGRAM_NAME + " " + title);
                    pathQueue.put(chooser.showDialog(null).toPath());
                } catch (InterruptedException ex) {
                    Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
                    throw new IllegalStateException(ex);
                }
            });
            return Optional.of(pathQueue.take());
        } catch (InterruptedException ex) {
            Logger.getLogger(UI.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }
}
