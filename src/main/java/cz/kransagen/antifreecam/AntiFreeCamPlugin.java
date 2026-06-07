package cz.kransagen.antifreecam;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class AntiFreeCamPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private BlockMaskService blockMaskService;
    private EntityVisibilityService entityVisibilityService;
    private BukkitTask updateTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.blockMaskService = new BlockMaskService(this, configManager);
        this.entityVisibilityService = new EntityVisibilityService(this, configManager);

        Bukkit.getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        startUpdateTask();
        getLogger().info("AntiFreeCam_AntiESP enabled.");
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            blockMaskService.restorePlayer(player);
            entityVisibilityService.restorePlayer(player);
        }

        getLogger().info("AntiFreeCam_AntiESP disabled.");
    }

    public void updatePlayer(Player player) {
        blockMaskService.updatePlayer(player);
        entityVisibilityService.updatePlayer(player);
    }

    public void clearPlayer(Player player) {
        blockMaskService.restorePlayer(player);
        entityVisibilityService.restorePlayer(player);
    }

    public void updatePlayerChunk(Player player, Chunk chunk) {
        blockMaskService.applyChunkMask(player, chunk);
    }

    private void startUpdateTask() {
        long interval = Math.max(1L, configManager.getUpdateTicks());
        this.updateTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayer(player);
            }
        }, 20L, interval);
    }
}
