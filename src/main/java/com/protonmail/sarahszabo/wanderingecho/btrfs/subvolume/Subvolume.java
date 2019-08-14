/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.protonmail.sarahszabo.wanderingecho.btrfs.BTRFS;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class representing a subvolume.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class Subvolume extends BTRFSPhysicalLocationItem<Subvolume> {

    /**
     * Creates a new subvolume with the specified path.The subvolume may or may
     * not exist at this point.
     *
     * @param location
     */
    @JsonCreator
    public Subvolume(@JsonProperty(value = "location") Path location) {
        super(location, location.getFileName().toString());
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
            return new Snapshot(getLocation(), BTRFS.configureSnapshotFilesystem(this));
        } catch (IOException ex) {
            Logger.getLogger(Subvolume.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public String toString() {
        return "Subvolume Location: " + getLocation();
    }
}
