package cz.kransagen.antifreecam;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerSessionListener implements Listener {

    private final AntiFreeCamPlugin plugin;

    public PlayerSessionListener(AntiFreeCamPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.updatePlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.clearPlayer(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        plugin.updatePlayer(event.getPlayer());
    }

    @EventHandler
    public void onChunkLoad(PlayerChunkLoadEvent event) {
        plugin.updatePlayerChunk(event.getPlayer(), event.getChunk());
    }
}
