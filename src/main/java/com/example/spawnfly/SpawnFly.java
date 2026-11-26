package com.example.spawnfly;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class SpawnFly extends JavaPlugin implements Listener {
    private final Set<UUID> enabledPlayers = new HashSet<>();
    private String regionId;
    private String permission;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        regionId = getConfig().getString("region", "spawn").toLowerCase(Locale.ROOT);
        permission = getConfig().getString("permission", "spawnfly.use");

        getServer().getPluginManager().registerEvents(this, this);
        for (Player player : getServer().getOnlinePlayers()) {
            syncFlight(player, player.getLocation(), true);
        }
        getLogger().info("SpawnFly enabled. Automatic flight region: " + regionId);
    }

    @Override
    public void onDisable() {
        for (UUID uuid : Set.copyOf(enabledPlayers)) {
            Player player = getServer().getPlayer(uuid);
            if (player != null) {
                revokeFlight(player, null);
            }
        }
        enabledPlayers.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        checkExit(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        checkExit(event.getPlayer(), event.getFrom(), event.getTo());
    }

    private void checkExit(Player player, Location from, Location to) {
        if (to == null || sameBlock(from, to)) {
            return;
        }
        syncFlight(player, to, true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        syncFlight(event.getPlayer(), event.getPlayer().getLocation(), true);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        syncFlight(player, player.getLocation(), true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // Apply the region rule to the respawn destination. Flight is never
        // restored outside spawn, but is automatically enabled if the player
        // respawns inside spawn and has the required permission.
        syncFlight(event.getPlayer(), event.getRespawnLocation(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (enabledPlayers.contains(player.getUniqueId())) {
            revokeFlight(player, null);
        }
    }

    private void syncFlight(Player player, Location location, boolean sendMessages) {
        UUID uuid = player.getUniqueId();
        boolean shouldFly = player.hasPermission(permission) && isInsideSpawn(location);

        if (shouldFly) {
            if (enabledPlayers.add(uuid)) {
                player.setAllowFlight(true);
                if (sendMessages) {
                    player.sendMessage(color(getConfig().getString(
                            "messages.enabled", "&aSpawn flight enabled automatically.")));
                }
            } else if (!player.getAllowFlight()) {
                // Reassert the permission while the player remains in spawn.
                player.setAllowFlight(true);
            }
            return;
        }

        if (enabledPlayers.contains(uuid)) {
            revokeFlight(player, sendMessages ? getConfig().getString(
                    "messages.left-region", "&cSpawn flight disabled.") : null);
        }
    }

    private boolean isInsideSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        try {
            var query = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .createQuery();

            var applicable = query.getApplicableRegions(BukkitAdapter.adapt(location));
            for (ProtectedRegion region : applicable.getRegions()) {
                if (region.getId().equalsIgnoreCase(regionId)) {
                    return true;
                }
            }
        } catch (RuntimeException exception) {
            getLogger().warning("WorldGuard region query failed; denying spawn flight: "
                    + exception.getMessage());
        }
        return false;
    }

    private void revokeFlight(Player player, String message) {
        enabledPlayers.remove(player.getUniqueId());
        player.setFlying(false);
        player.setAllowFlight(false);
        if (message != null && !message.isBlank()) {
            player.sendMessage(color(message));
        }
    }

    private boolean sameBlock(Location a, Location b) {
        return a != null && b != null
                && a.getWorld() != null && a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private String color(String message) {
        return message.replace('&', '§');
    }
}
