/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs.subvolume;

import com.protonmail.sarahszabo.wanderingecho.util.EchoUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class representing a BTRFS snapshot.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class Snapshot extends BTRFSPhysicalLocationItem {

    /**
     * Creates a new snapshot with the specified location. The snapshot may or
     * may not exist.
     *
     * @param location The location of the snapshot
     */
    public Snapshot(Path location) {
        super(location);
    }

    /**
     * Creates a snapshot at the location given to the constructor.
     */
    @Override
    void create() {
        try {
            EchoUtil.processOP(true, "btrfs ");
        } catch (IOException ex) {
            Logger.getLogger(Snapshot.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }
}
