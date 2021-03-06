/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A utility class for the program.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class EchoUtil {

    /**
     * The temporary directory
     */
    public static final Path TEMP_DIRECTORY;
    private static final Logger logger = Logger.getLogger(EchoUtil.class.getName());

    static {
        try {
            //Make a new temp directory
            TEMP_DIRECTORY = Files.createTempDirectory("Wandering Echo Temporary Directory");
        } catch (IOException ex) {
            Logger.getLogger(EchoUtil.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Unable to create temp directory", ex);
        }
    }

    /**
     * /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param commands The commands to execute
     * @param inheritIO Merge the process streams?
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(List<String> commands, boolean inheritIO) throws IOException {
        return processOP(inheritIO, commands.toArray(new String[4]));
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion. Does not inherit IO.
     *
     * @param directory The directory to be in
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(Path directory, String... commands) throws IOException {
        return processOP(false, null, TEMP_DIRECTORY, commands);
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion. Does not inherit IO.
     *
     * @param commands The commands to execute
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOPNOE(String... commands) {
        try {
            return processOP(true, commands);
        } catch (IOException ex) {
            Logger.getLogger(EchoUtil.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Something went wrong with processOP", ex);
        }
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion. Does not inherit IO.
     *
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(String... commands) throws IOException {
        return processOP(true, commands);
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param inheritIO Should the streams be merged
     * @param redirect The path to direct output from the process to, if null,
     * prints to terminal
     * @param directory The directory to be in
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(boolean inheritIO, Path redirect, Path directory, String... commands) throws IOException {
        ProcessBuilder builder = processOPBuilder(inheritIO, redirect, directory, commands);
        //Print out FFMPEG Command
        logger.info("COMMAND: " + builder.command().stream().collect(Collectors.joining(" ")));
        //Actually do it
        Process proc = builder.start();
        try {
            proc.waitFor(30, TimeUnit.SECONDS);
            if (proc.isAlive()) {
                proc.destroyForcibly();
                return true;
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(EchoUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param inheritIO Should the streams be merged
     * @param redirect The path to direct output from the process to, if null,
     * prints to terminal
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(boolean inheritIO, Path redirect, String... commands) throws IOException {
        return processOP(inheritIO, redirect, TEMP_DIRECTORY, commands);
    }

    /**
     * Launches a new process in the temp directory, and waits for its
     * completion.
     *
     * @param inheritIO Should the streams be merged
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return Whether or not the operation timed out or not
     */
    public static boolean processOP(boolean inheritIO, String... commands) throws IOException {
        return processOP(inheritIO, null, commands);
    }

    /**
     * Launches a new process in the temp directory, and does not wait for its
     * completion.
     *
     * @param inheritIO Should the streams be merged
     * @param commands The commands to execute
     * @return The generated process
     */
    public static Process processOPNoWaitNOE(boolean inheritIO, String... commands) {
        try {
            var builder = processOPBuilder(inheritIO, commands);
            System.out.println("COMMAND:" + Arrays.asList(commands).stream().collect(Collectors.joining(" ")));
            return builder.start();
        } catch (IOException ex) {
            Logger.getLogger(EchoUtil.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Something went wrong in the no wait processOP subroutine", ex);
        }
    }

    /**
     * Launches a new process in the temp directory, and does not wait for its
     * completion.
     *
     * @param inheritIO Should the streams be merged
     * @param commands The commands to execute
     * @throws IOException InterruptedException If something went wrong
     * @return The generated process
     */
    public static Process processOPNoWait(boolean inheritIO, String... commands) throws IOException {
        var builder = processOPBuilder(inheritIO, commands);
        System.out.println("COMMAND:" + Arrays.asList(commands).stream().collect(Collectors.joining(" ")));
        return builder.start();
    }

    /**
     * Gets the process builder with the specified boolean indicating whether IO
     * should be inherited or not, and the commands to execute. This process
     * builder is localised at the temp directory.
     *
     * @param inheritIO If IO should be inherited
     * @param redirect The path to direct output from the process to, if null,
     * prints to terminal
     * @param directory The directory that the process builder should be in
     * @param commands The commands to execute
     * @return The process builder with these properties
     */
    public static ProcessBuilder processOPBuilder(boolean inheritIO, Path redirect, Path directory, String... commands) {
        ProcessBuilder builder = new ProcessBuilder(commands).directory(directory.toFile());
        if (inheritIO) {
            builder = builder.inheritIO();

        }
        if (redirect != null) {
            builder = builder.redirectOutput(redirect.toFile());
        }
        return builder;
    }

    /**
     * Gets the process builder with the specified boolean indicating whether IO
     * should be inherited or not, and the commands to execute. This process
     * builder is localised at the temp directory.
     *
     * @param inheritIO If IO should be inherited
     * @param redirect The path to direct output from the process to, if null,
     * prints to terminal
     * @param commands The commands to execute
     * @return The process builder with these properties
     */
    public static ProcessBuilder processOPBuilder(boolean inheritIO, Path redirect, String... commands) {
        return processOPBuilder(inheritIO, redirect, TEMP_DIRECTORY, commands);
    }

    /**
     * Gets the process builder with the specified boolean indicating whether IO
     * should be inherited or not, and the commands to execute. This process
     * builder is localized at the temp directory.
     *
     * @param inheritIO If IO should be inherited
     * @param commands The commands to execute
     * @return The process builder with these properties
     */
    public static ProcessBuilder processOPBuilder(boolean inheritIO, String... commands) {
        return inheritIO ? new ProcessBuilder(commands)
                .directory(TEMP_DIRECTORY.toFile()).inheritIO()
                : new ProcessBuilder(commands).directory(TEMP_DIRECTORY.toFile());
    }

    /**
     * Helper method, prints the message then System.exit().
     *
     * @param message The message to print
     */
    public static <T> void messageThenExit(T message) {
        logger.info(message.toString());
        System.exit(0);
    }

    /**
     * Helper method, prints the message then System.exit().
     *
     * @param message The message to print
     */
    public static void messageThenExit(String message) {
        logger.info(message);
        System.exit(0);
    }

    /**
     * Gets the scanner from the process's input stream.
     *
     * @param process The process we're getting input from
     * @return The scanner
     */
    public static Scanner getProcessInputScanner(Process process) {
        return new Scanner(process.getInputStream());
    }

    /**
     * Gets the scanner associated with the system input.
     *
     * @return The scanner
     */
    public static Scanner getSystemInputScanner() {
        return new Scanner(System.in);
    }

    private EchoUtil() {
    }

    /**
     * A helper class that formats loggers in the program.
     */
    public static class LoggerFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            if (record.getLevel() == Level.INFO) {
                return record.getMessage() + "\n\n";
            } else {
                return record.getInstant() + ": " + record.getSourceClassName() + " / " + record.getSourceMethodName()
                        + "\n" + record.getMessage();
            }
        }

    }
}
