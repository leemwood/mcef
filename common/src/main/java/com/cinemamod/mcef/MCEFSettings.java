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

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class MCEFSettings {
    private static final Path PATH = Minecraft.getInstance().gameDirectory
            .toPath()
            .resolve("config")
            .resolve("mcef")
            .resolve("mcef.properties");
    private static int deleteRetries = 0;

    private String userAgent;
    private boolean useCache;

    public MCEFSettings() {
        userAgent = null;
        useCache = true;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        saveAsync();
    }

    public boolean isUsingCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
        saveAsync();
    }

    public void saveAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void save() throws IOException {
        File file = PATH.toFile();

        file.getParentFile().mkdirs();

        if (!file.exists()) {
            file.createNewFile();
        }

        Properties properties = new Properties();
        properties.setProperty("user-agent", String.valueOf(userAgent));
        properties.setProperty("use-cache", String.valueOf(useCache));

        try (FileOutputStream output = new FileOutputStream(file)) {
            properties.store(output, null);
        }
    }

    public void load() throws IOException {
        File file = PATH.toFile();

        if (!file.exists()) {
            save();
        }

        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        }

        try {
            userAgent = properties.getProperty("user-agent");
            useCache = Boolean.parseBoolean(properties.getProperty("use-cache"));
        } catch (Exception e) {
            // Delete and re-create the file if there was a parsing error
            if (deleteRetries++ > 20)
                return; // Stop gap
            file.delete();
            save();
        }
    }
}
