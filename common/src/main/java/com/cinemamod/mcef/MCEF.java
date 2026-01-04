/*
 *     MCEF (Minecraft Chromium Embedded Framework)
 *     Copyright (C) 2023 CinemaMod Group
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */

package com.cinemamod.mcef;

import com.cinemamod.mcef.listeners.MCEFInitListener;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * An API to create Chromium web browsers in Minecraft. Uses
 * a modified version of java-cef (Java Chromium Embedded Framework).
 */
public final class MCEF {
    public static final Logger LOGGER = LoggerFactory.getLogger("MCEF");
    private static MCEFSettings settings;

    private static boolean initialized = false;

    public static void scheduleForInit(MCEFInitListener listener) {
        if (isInitialized()) {
            listener.onInit(true);
        } else {
            // Since we no longer have a downloader, we can try to initialize immediately
            if (initialize()) {
                listener.onInit(true);
            } else {
                listener.onInit(false);
            }
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * Get access to various settings for MCEF.
     * @return Returns the existing {@link MCEFSettings} or creates a new {@link MCEFSettings} and loads from disk (blocking)
     */
    public static MCEFSettings getSettings() {
        if (settings == null) {
            settings = new MCEFSettings();
            try {
                settings.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return settings;
    }

    /**
     * This gets called by mod entry points.
     * This should not be called by anything else.
     */
    public static boolean initialize() {
        if (initialized) return true;

        MCEFPlatform platform = MCEFPlatform.getPlatform();
        MCEF.getLogger().info("Initializing MCEF on " + platform.getNormalizedName() + "...");

        if (!platform.isAndroid()) {
            MCEF.getLogger().warn("MCEF download behavior has been removed. Desktop platforms are no longer supported by automatic setup.");
            MCEF.getLogger().warn("Only Android platform is supported for now via native WebView bridge.");
            return false;
        }

        MCEF.getLogger().info("Android platform detected, using native WebView bridge");
        // Android initialization is currently handled via the delegating MCEFBrowser
        // which will instantiate AndroidMCEFBrowser when needed.
        initialized = true;
        return true;
    }

    /**
     * Creates a new Chromium web browser with some starting URL. Can set it to be transparent rendering.
     * @return the {@link MCEFBrowser} web browser instance, or null if MCEF is not initialized
     */
    public static MCEFBrowser createBrowser(String url, boolean transparent) {
        if (!isInitialized()) {
            return null;
        }
        if (MCEFPlatform.getPlatform().isAndroid()) {
            return new MCEFBrowser(new AndroidMCEFBrowser(url, transparent));
        }
        return new MCEFBrowser(url, transparent);
    }

    /**
     * Creates a new Chromium web browser with some starting URL.
     * @return the {@link MCEFBrowser} web browser instance, or null if MCEF is not initialized
     */
    public static MCEFBrowser createBrowser(String url) {
        return createBrowser(url, false);
    }

    /**
     * Creates a new Chromium web browser with some starting URL, width, and height.
     * Can set it to be transparent rendering.
     * @return the {@link MCEFBrowser} web browser instance, or null if MCEF is not initialized
     */
    public static MCEFBrowser createBrowser(String url, boolean transparent, int width, int height) {
        MCEFBrowser browser = createBrowser(url, transparent);
        if (browser != null) {
            browser.resize(width, height);
        }
        return browser;
    }

    /**
     * Check if MCEF is initialized.
     * @return true if MCEF is initialized correctly, false if not
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Request a shutdown of MCEF/CEF. Nothing will happen if not initialized.
     */
    public static void shutdown() {
        if (isInitialized()) {
            initialized = false;
        }
    }
}
