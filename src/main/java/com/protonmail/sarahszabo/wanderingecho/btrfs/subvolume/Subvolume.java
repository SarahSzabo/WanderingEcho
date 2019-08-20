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
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class representing a subvolume.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class Subvolume extends BTRFSPhysicalLocationItem<Subvolume> {

    @JsonIgnore
    private final Path snapshotFolder;

    /**
     * Creates a new subvolume with the specified path.The subvolume may or may
     * not exist at this point.
     *
     * @param location The location of this subvolume
     */
    @JsonCreator
    public Subvolume(@JsonProperty(value = "location") Path location) {
        super(location, location.getFileName().toString());
        try {
            this.snapshotFolder = BTRFS.configureSnapshotFilesystem(this);
        } catch (IOException ex) {
            Logger.getLogger(Subvolume.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("Couldn't construct subvolume, problem with getting snapshot folder", ex);
        }
    }

    @Override
    public Subvolume create() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Takes a snapshot of this subvolume.
     *
     * @return The created snapshot
     */
    public Snapshot snapshot() {
        try {
            return new Snapshot(getLocation(), BTRFS.configureSnapshotFilesystem(this), this);
        } catch (IOException ex) {
            Logger.getLogger(Subvolume.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.snapshotFolder);
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
        final Subvolume other = (Subvolume) obj;
        if (!Objects.equals(this.snapshotFolder, other.snapshotFolder)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Subvolume Location: " + getLocation();
    }

    /**
     * Gets the folder that snapshots are stored in.
     *
     * @return The folder that this subvolume's snapshots are stored in
     */
    public Path getSnapshotFolder() {
        return snapshotFolder;
    }
}
