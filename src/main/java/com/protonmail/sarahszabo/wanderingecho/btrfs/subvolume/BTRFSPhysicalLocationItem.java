/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.nio.file.Path;

/**
 * A class that represents an a=object that has a physical presence on the disk.
 * Should be subclassed by all classes that have this property.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "BTRFSPhysicalLocationItem")
@JsonSubTypes({
    @Type(name = "Snapshot", value = Snapshot.class),
    @Type(name = "Backup", value = Backup.class),
    @Type(name = "Subvolume", value = Subvolume.class)})
public abstract class BTRFSPhysicalLocationItem {

    private final String name;
    @JsonProperty
    private final Path location;

    /**
     * Makes a new subvolume instance with the specified location. May or may
     * not actually exist.
     *
     * @param location The location of the subvolume
     */
    @JsonCreator
    protected BTRFSPhysicalLocationItem(@JsonProperty(value = "location") Path location) {
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
