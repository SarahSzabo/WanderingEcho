/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume;

import java.nio.file.Path;

/**
 * A class that represents an a=object that has a physical presence on the disk.
 * Should be subclassed by all classes that have this property.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public abstract class BTRFSPhysicalLocationItem {

    private final String name;
    private final Path location;

    /**
     * Makes a new subvolume instance with the specified location. May or may
     * not actually exist.
     *
     * @param location The location of the subvolume
     */
    protected BTRFSPhysicalLocationItem(Path location) {
        this.location = location;
        this.name = location.getFileName().toString();
    }

    /**
     * Gets the filename of the location
     *
     * @return The filename of the location
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the path of the location.
     *
     * @return the path of the location
     */
    public Path getLocation() {
        return this.location;
    }

    /**
     * Creates a physical copy of this item on the disk.
     */
    abstract void create();
}
