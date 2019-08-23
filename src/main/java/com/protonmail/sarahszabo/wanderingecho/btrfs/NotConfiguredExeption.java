/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.protonmail.sarahszabo.wanderingecho.btrfs;

/**
 * An exception representing an error in configuration.
 *
 * @author Sarah Szabo <SarahSzabo@Protonmail.com>
 */
public class NotConfiguredExeption extends IllegalStateException {

    /**
     * Creates a new instance of <code>NotConfiguredExeption</code> without
     * detail message.
     */
    public NotConfiguredExeption() {
    }

    /**
     * Constructs an instance of <code>NotConfiguredExeption</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public NotConfiguredExeption(String msg) {
        super(msg);
    }
}
