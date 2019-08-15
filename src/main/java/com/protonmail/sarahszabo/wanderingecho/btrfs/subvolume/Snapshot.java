/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume;

import com.protonmail.sarahszabo.wanderingecho.btrfs.BTRFS;
import com.protonmail.sarahszabo.wanderingecho.util.EchoUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class representing a BTRFS snapshot.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class Snapshot extends BTRFSPhysicalLocationItem<Snapshot> {

    /**
     * The date time formatter for snapshot dates.
     */
    public static final DateTimeFormatter SNAPSHOT_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ssz");

    //DateTimeFormatter.ofPattern("dd-mm-yyyy_HH-mm-ss_z");
    /**
     * Gets the current date using the Snapshot formatter as a string.
     *
     * @return The date in string format
     */
    private static String getCurrentDateString() {
        return ZonedDateTime.now().format(SNAPSHOT_FORMAT);
    }

    private final Path of;
    private final String fullFileName;
    private final ZonedDateTime creationDate;

    /**
     * Constructs a new snapshot from an already existing snapshot on the disk.
     *
     * @param existing The already existing snapshot on the disk
     */
    public Snapshot(Path existing) {
        super(existing, existing.getFileName().toString().split(BTRFS.SNAPSHOT_SEPARATOR)[0]);
        this.of = null;
        this.fullFileName = existing.toString();
        this.creationDate = ZonedDateTime.parse(existing.getFileName().toString().split(BTRFS.SNAPSHOT_SEPARATOR)[1], SNAPSHOT_FORMAT);
    }

    /**
     * Creates a new snapshot with the specified location.The snapshot may or
     * may not exist.
     *
     * @param of The subvolume to take a snapshot of
     * @param location The location to place the snapshot
     */
    public Snapshot(Path of, Path location) {
        super(location.resolve(of.getFileName() + BTRFS.SNAPSHOT_SEPARATOR + getCurrentDateString()), of.getFileName().toString());
        this.of = of;
        //The filename on the disk: /media/disk/SUBVOL___TIME
        this.fullFileName = super.getLocation().toString();
        this.creationDate = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.of("Z"));
    }

    /**
     * Creates a snapshot at the location given to the constructor.
     *
     * @return The created snapshot
     */
    @Override
    public Snapshot create() {
        try {
            System.out.println(getLocation().resolve(
                    getName() + "___" + getCurrentDateString()));
            //Command: btrfs subvolume snapshot "thing to snapshot" "place to put snapshot"
            EchoUtil.processOP(true, "btrfs", "subvolume", "snapshot", "-r", this.of.toString(), this.fullFileName);
            return new Snapshot(super.getLocation());
        } catch (IOException ex) {
            Logger.getLogger(Snapshot.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Generates a new bash script to backup the file to the external drive
     * since process builder can't handle pipe characters.
     *
     * @param parent The parent of this snapshot, may be null
     * @param location The location to back up to
     * @return The location of the new script file
     */
    private Path generateBashBackupScript(Snapshot parent, Path location) throws IOException {
        //Command: btrfs send "SUBVOLUME" | btrfs recieve "LOCATION"
        var text = "btrfs send " + (parent == null ? "" : "-p \"" + parent.getFullFileName() + "\" ")
                + "\"" + this.fullFileName + "\" | btrfs receive \"" + location + "\"";
        var scriptLocation = Files.createTempFile("Wandering Echo Temporary Backup Script for " + getName(), ".sh");
        Files.write(scriptLocation, text.getBytes());
        return scriptLocation;
    }

    /**
     * Backs up this snapshot to a certain location.
     *
     * @param parent The parent to use, may be null
     * @param location The location to send the backup to
     * @return The backup object
     */
    public Backup backup(Snapshot parent, Path location) {
        try {
            //Make bash backup script
            var scriptPath = generateBashBackupScript(parent, location);
            //Execute backup script
            EchoUtil.processOP("bash", scriptPath.toString());
            return new Backup(location);
        } catch (IOException ex) {
            Logger.getLogger(Snapshot.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public String toString() {
        return this.fullFileName;
    }

    /**
     * Getter for the full file name of this snapshot as it will appear on the
     * disk.
     *
     * @return The full filename
     */
    public String getFullFileName() {
        return this.fullFileName;
    }

    /**
     * Gets the creation date of this snapshot. NOTE: the creation date may be
     * {@link ZonedDateTime}'s max value if we haven't called the create method.
     * This means that the snapshot doesn't exist yet.
     *
     * @return The creation date
     */
    public ZonedDateTime getCreationDate() {
        return this.creationDate;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.fullFileName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Snapshot other = (Snapshot) obj;
        if (!Objects.equals(this.fullFileName, other.fullFileName)) {
            return false;
        }
        return true;
    }

}
