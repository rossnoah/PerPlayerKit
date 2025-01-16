/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;

public class UpdateChecker {

    private Plugin plugin;
    private String url = "https://hangar.papermc.io/api/v1/projects/PerPlayerKit/latestrelease";
    private String spigotDownloadUrl = "https://www.spigotmc.org/resources/perplayerkit.121437/";
    private String modrinthDownloadUrl = "https://modrinth.com/plugin/perplayerkit";
    private String hangarDownloadUrl = "https://hangar.papermc.io/noah32/PerPlayerKit";

    private Boolean updateAvailableCache = null;
    private String latestVersionCache = null;

    public UpdateChecker(Plugin plugin) {
        this.plugin = plugin;
    }

    private String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }

    private String getLatestVersion() {
        if (latestVersionCache != null) {
            return latestVersionCache; // Return cached latest version
        }

        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Call call = client.newCall(request);
            Response response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                if (!responseBody.isEmpty() && responseBody.matches("\\d+(\\.\\d+)*")) {
                    latestVersionCache = responseBody; // Cache the latest version
                    return latestVersionCache;
                } else {
                    plugin.getLogger().warning("Received invalid version format from update server: " + responseBody);
                }
            } else {
                plugin.getLogger().warning("Failed to fetch the latest version. HTTP Status: " + response.code());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("IOException occurred while fetching the latest version: " + e.getMessage());
        }

        plugin.getLogger().warning("Using fallback version: 1.0.0");
        latestVersionCache = "1.0.0"; // Cache fallback version
        return latestVersionCache;
    }

    public boolean checkForUpdate() {
        if (updateAvailableCache != null) {
            return updateAvailableCache; // Return cached result if already checked
        }

        String currentVersion = getCurrentVersion();
        String latestVersion = getLatestVersion();

        updateAvailableCache = isSemanticallyNewer(currentVersion, latestVersion);
        return updateAvailableCache;
    }

    public void printStartupStatus() {
        if (checkForUpdate()) {
            String currentVersion = getCurrentVersion();
            String latestVersion = getLatestVersion();

            plugin.getLogger().info("A new version of PerPlayerKit is available! You are running version " + currentVersion + " and the latest version is " + latestVersion);
            plugin.getLogger().info("Download the latest version at:");
            plugin.getLogger().info("Spigot: " + spigotDownloadUrl);
            plugin.getLogger().info("Modrinth: " + modrinthDownloadUrl);
            plugin.getLogger().info("PaperMC: " + hangarDownloadUrl);
        } else {
            plugin.getLogger().info("You are running the latest version of PerPlayerKit");
        }
    }

    public void sendUpdateMessage(Player player) {
        if (checkForUpdate()) {
            String currentVersion = getCurrentVersion();
            String latestVersion = getLatestVersion();

            player.sendMessage("A new version of PerPlayerKit is available! You are running version " + currentVersion + " and the latest version is " + latestVersion);
        }
    }

    private boolean isSemanticallyNewer(String currentVersion, String newVersion) {
        String[] currentVersionSplit = currentVersion.split("\\.");
        String[] newVersionSplit = newVersion.split("\\.");

        for (int i = 0; i < Math.min(currentVersionSplit.length, newVersionSplit.length); i++) {
            int currentNumber = Integer.parseInt(currentVersionSplit[i]);
            int newNumber = Integer.parseInt(newVersionSplit[i]);

            if (currentNumber < newNumber) {
                return true;
            } else if (currentNumber > newNumber) {
                return false;
            }
            // Loop continues if numbers are equal
        }

        return newVersionSplit.length > currentVersionSplit.length;
    }
}

