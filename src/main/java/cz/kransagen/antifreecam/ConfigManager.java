package cz.kransagen.antifreecam;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ConfigManager {

    private final AntiFreeCamPlugin plugin;

    private boolean blockHideEnabled;
    private int revealDistance;
    private Set<Material> hiddenMaterials;

    private boolean antiEspEnabled;
    private double antiEspRadius;
    private double antiEspRadiusSquared;
    private double fovDegrees;
    private double fovDotThreshold;
    private boolean includePlayers;
    private boolean includeMobs;

    private int updateTicks;
    private int blockScanHorizontal;
    private int blockScanVertical;

    public ConfigManager(AntiFreeCamPlugin plugin) {
        this.plugin = plugin;
        this.hiddenMaterials = EnumSet.noneOf(Material.class);
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.blockHideEnabled = config.getBoolean("block-hide.enabled", true);
        this.revealDistance = Math.max(1, config.getInt("block-hide.reveal-distance", 20));

        Set<Material> parsedMaterials = EnumSet.noneOf(Material.class);
        List<String> names = config.getStringList("block-hide.hidden-materials");
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material == null || !material.isBlock()) {
                plugin.getLogger().warning("Unknown block material in config: " + name);
                continue;
            }
            parsedMaterials.add(material);
        }
        this.hiddenMaterials = parsedMaterials;

        this.antiEspEnabled = config.getBoolean("anti-esp.enabled", true);
        this.antiEspRadius = Math.max(1.0, config.getDouble("anti-esp.radius", 50.0));
        this.antiEspRadiusSquared = antiEspRadius * antiEspRadius;
        this.fovDegrees = Math.min(179.0, Math.max(1.0, config.getDouble("anti-esp.fov-degrees", 110.0)));
        this.fovDotThreshold = Math.cos(Math.toRadians(fovDegrees / 2.0));
        this.includePlayers = config.getBoolean("anti-esp.include-players", true);
        this.includeMobs = config.getBoolean("anti-esp.include-mobs", true);

        this.updateTicks = Math.max(1, config.getInt("performance.update-ticks", 10));
        this.blockScanHorizontal = Math.max(revealDistance, config.getInt("performance.block-scan-horizontal", 24));
        this.blockScanVertical = Math.max(1, config.getInt("performance.block-scan-vertical", 16));
    }

    public boolean isBlockHideEnabled() {
        return blockHideEnabled;
    }

    public int getRevealDistance() {
        return revealDistance;
    }

    public Set<Material> getHiddenMaterials() {
        return hiddenMaterials;
    }

    public boolean isAntiEspEnabled() {
        return antiEspEnabled;
    }

    public double getAntiEspRadiusSquared() {
        return antiEspRadiusSquared;
    }

    public double getAntiEspRadius() {
        return antiEspRadius;
    }

    public double getFovDotThreshold() {
        return fovDotThreshold;
    }

    public boolean isIncludePlayers() {
        return includePlayers;
    }

    public boolean isIncludeMobs() {
        return includeMobs;
    }

    public int getUpdateTicks() {
        return updateTicks;
    }

    public int getBlockScanHorizontal() {
        return blockScanHorizontal;
    }

    public int getBlockScanVertical() {
        return blockScanVertical;
    }
}
