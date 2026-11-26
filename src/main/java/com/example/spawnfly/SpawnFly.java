package com.example.spawnfly;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class SpawnFly extends JavaPlugin implements Listener, CommandExecutor {
    private final Set<UUID> enabledPlayers = new HashSet<>();
    private String regionId;
    private String permission;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        regionId = getConfig().getString("region", "spawn").toLowerCase(Locale.ROOT);
        permission = getConfig().getString("permission", "spawnfly.use");

        if (getCommand("fly") == null) {
            getLogger().severe("Command /fly is missing from plugin.yml; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("fly").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SpawnFly enabled. WorldGuard region: " + regionId);
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /fly.");
            return true;
        }

        if (!player.hasPermission(permission)) {
            player.sendMessage(color(getConfig().getString(
                    "messages.no-permission", "&cYou do not have permission to use /fly.")));
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (enabledPlayers.contains(uuid)) {
            revokeFlight(player, getConfig().getString("messages.disabled", "&cFlight disabled."));
            return true;
        }

        if (!isInsideSpawn(player.getLocation())) {
            player.sendMessage(color(getConfig().getString(
                    "messages.outside-region", "&cYou can only enable flight inside spawn.")));
            return true;
        }

        enabledPlayers.add(uuid);
        player.setAllowFlight(true);
        player.sendMessage(color(getConfig().getString(
                "messages.enabled", "&aFlight enabled. It will turn off when you leave spawn.")));
        return true;
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
        if (to == null || !enabledPlayers.contains(player.getUniqueId())) {
            return;
        }

        // WorldGuard region membership is block-based. Avoid querying it for
        // head rotation or movement within the same block.
        if (sameBlock(from, to)) {
            return;
        }

        if (!isInsideSpawn(to)) {
            revokeFlight(player, getConfig().getString(
                    "messages.left-region", "&cFlight disabled because you left spawn."));
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (enabledPlayers.contains(player.getUniqueId())
                && !isInsideSpawn(player.getLocation())) {
            revokeFlight(player, getConfig().getString(
                    "messages.left-region", "&cFlight disabled because you left spawn."));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // PlayerDeathEvent is not present in this server's 26.3 API.
        // Clear the temporary state on respawn regardless of destination so
        // dying inside spawn never silently restores flight.
        Player player = event.getPlayer();
        if (enabledPlayers.contains(player.getUniqueId())) {
            revokeFlight(player, null);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (enabledPlayers.contains(player.getUniqueId())) {
            revokeFlight(player, null);
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
