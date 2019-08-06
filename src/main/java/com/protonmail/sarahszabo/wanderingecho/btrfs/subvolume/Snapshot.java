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

    private final Path of;
    private final String fileName;

    /**
     * Creates a new snapshot with the specified location.The snapshot may or
     * may not exist.
     *
     * @param of The subvolume to take a snapshot of
     * @param location The location to place the snapshot
     */
    public Snapshot(Path of, Path location) {
        super(location, of.getFileName().toString());
        this.of = of;
        //The filename on the disk: /media/disk/SUBVOL___TIME
        this.fileName = location.resolve(of.getFileName() + "___" + EchoUtil.getBTRFSStorageString()).toString();
    }

    /**
     * Creates a snapshot at the location given to the constructor.
     */
    @Override
    public void create() {
        try {
            System.out.println(getLocation().resolve(
                    getName() + "___" + EchoUtil.getBTRFSStorageString()).toString());
            //Command: btrfs subvolume snapshot "thing to snapshot" "place to put snapshot"
            EchoUtil.processOP(true, "btrfs", "subvolume", "snapshot", "-r", this.of.toString(), this.fileName);
        } catch (IOException ex) {
            Logger.getLogger(Snapshot.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Backs up this snapshot to a certain location.
     *
     * @param location The location to send the backup to
     * @return The backup object
     */
    public Backup backup(Path location) {
        try {
            //Command: btrfs send "SUBVOLUME" | btrfs recieve "LOCATION"
            EchoUtil.processOP(true, "btrfs", "send", this.fileName, "|", "btrfs", "receive", location.toString());
            return new Backup(location);
        } catch (IOException ex) {
            Logger.getLogger(Snapshot.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Backs up this snapshot using a parent snapshot to a certain location
     *
     * @param parent The parent of this snapshot
     * @param location The location to send this backup to
     * @return The backup object
     */
    public Backup backup(Snapshot parent, Path location) {
        try {
            //Command: btrfs send -p "PARENT" "SUBVOLUME" | btrfs recieve "LOCATION"
            EchoUtil.processOP(true, "btrfs", "send", "-p", parent.toString(),
                    getLocation().toString(), "|", "btrfs", "recieve", location.toString());
            return new Backup(location);
        } catch (IOException ex) {
            Logger.getLogger(Snapshot.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }
}
