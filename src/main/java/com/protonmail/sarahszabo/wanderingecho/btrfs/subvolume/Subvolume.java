/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume;

import java.nio.file.Path;

/**
 * A class representing a subvolume.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class Subvolume extends BTRFSPhysicalLocationItem {

    /**
     * Creates a new subvolume with the specified path.The subvolume may or may
     * not exist at this point.
     *
     * @param location
     */
    public Subvolume(Path location) {
        super(location);
    }

    @Override
    void create() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
