/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(Snapshot.class.getName());

    //DateTimeFormatter.ofPattern("dd-mm-yyyy_HH-mm-ss_z");
    /**
     * Gets the current date using the Snapshot formatter as a string.
     *
     * @return The date in string format
     */
    private static String getCurrentDateString() {
        return ZonedDateTime.now().format(SNAPSHOT_FORMAT);
    }

    @JsonProperty
    private final Path of;
    @JsonIgnore
    private final String fullFileName;
    @JsonProperty
    private final ZonedDateTime creationDate;
    @JsonProperty
    private final Subvolume parentSubvolume;

    /**
     * Copy constructor that sets the date appropriatly.
     *
     * @param snapshot The snapshot to copy from
     */
    private Snapshot(Snapshot snapshot) {
        super(snapshot.getLocation(), snapshot.getName());
        this.of = snapshot.of;
        this.fullFileName = snapshot.fullFileName;
        this.parentSubvolume = snapshot.parentSubvolume;
        this.creationDate = ZonedDateTime.now();
    }

    /**
     * Creates a new snapshot with the specified location.The snapshot may or
     * may not exist.
     *
     * @param of The subvolume to take a snapshot of
     * @param location The location to place the snapshot
     * @param parent The parent subvolume
     */
    @JsonCreator
    public Snapshot(@JsonProperty(value = "of") Path of, @JsonProperty(value = "location") Path location,
            @JsonProperty(value = "parentSubvolume") Subvolume parent) {
        super(location.resolve(of.getFileName() + BTRFS.SNAPSHOT_SEPARATOR + getCurrentDateString()), of.getFileName().toString());
        this.of = of;
        //The filename on the disk: /media/disk/SUBVOL___TIME
        this.fullFileName = super.getLocation().toString();
        this.parentSubvolume = Objects.requireNonNull(parent);
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
            //Command: btrfs subvolume snapshot "thing to snapshot" "place to put snapshot"
            EchoUtil.processOP(true, "btrfs", "subvolume", "snapshot", "-r", this.of.toString(), this.fullFileName);
            var snapshotWithTimestamp = new Snapshot(this);
            BTRFS.saveSnapshot(snapshotWithTimestamp);
            return snapshotWithTimestamp;
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
            var backup = new Backup(location, this, ZonedDateTime.now());
            //Execute backup script
            EchoUtil.processOP("bash", scriptPath.toString());
            return backup;
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

    /**
     * Gets the subvolume that this snapshot is of.
     *
     * @return The subvolume that this snapshot is an image of
     */
    public Subvolume getParentSubvolume() {
        return this.parentSubvolume;
    }
}
