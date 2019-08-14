/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume;

import java.nio.file.Path;

/**
 * A class representing an external backup of a subvolume.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class Backup extends BTRFSPhysicalLocationItem<Backup> {

    /**
     * Creates a new backup that may or may not exist.
     *
     * @param location The location of the backup
     */
    public Backup(Path location) {
        super(location);
    }

    /**
     * This operation is unsupported on {@link Backup} objects.
     */
    @Override
    public Backup create() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
