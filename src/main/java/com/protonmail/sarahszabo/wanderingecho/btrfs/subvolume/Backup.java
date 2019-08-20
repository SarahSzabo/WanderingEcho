/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A class representing an external backup of a subvolume.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class Backup extends BTRFSPhysicalLocationItem<Backup> implements Comparable<ZonedDateTime> {

    @JsonProperty
    private final Snapshot parentSnapshot;
    @JsonProperty
    private final ZonedDateTime creationDate;

    /**
     * Creates a new backup that may or may not exist.
     *
     * @param location The location of the backup
     * @param parentSnapshot This backup's parent that created it
     * @param creationDate This backup's creation date
     */
    @JsonCreator
    public Backup(@JsonProperty(value = "location") Path location,
            @JsonProperty(value = "parentSnapshot") Snapshot parentSnapshot,
            @JsonProperty(value = "creationDate") ZonedDateTime creationDate) {
        super(location);
        this.parentSnapshot = parentSnapshot;
        this.creationDate = creationDate;
    }

    /**
     * This operation is unsupported on {@link Backup} objects. Since they are
     * created by {@link Snapshot} objects.
     *
     * @return The created backup
     */
    @Override
    public Backup create() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Gets this backup's parent snapshot that created it.
     *
     * @return The parent snapshot
     */
    public Snapshot getParentSnapshot() {
        return this.parentSnapshot;
    }

    /**
     * Get this backup's creation date.
     *
     * @return The creation date
     */
    public ZonedDateTime getCreationDate() {
        return this.creationDate;
    }

    @Override
    public int compareTo(ZonedDateTime arg0) {
        return this.creationDate.compareTo(arg0);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.parentSnapshot);
        hash = 29 * hash + Objects.hashCode(this.creationDate);
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
        final Backup other = (Backup) obj;
        if (!Objects.equals(this.parentSnapshot, other.parentSnapshot)) {
            return false;
        }
        if (!Objects.equals(this.creationDate, other.creationDate)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.getName();
    }

}
