package cz.kransagen.antifreecam;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BlockMaskService {

    private final AntiFreeCamPlugin plugin;
    private final ConfigManager config;
    private final Map<UUID, Set<BlockPosition>> currentlyMasked = new HashMap<>();

    public BlockMaskService(AntiFreeCamPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updatePlayer(Player player) {
        if (!config.isBlockHideEnabled()) {
            restorePlayer(player);
            return;
        }

        if (config.getHiddenMaterials().isEmpty()) {
            restorePlayer(player);
            return;
        }

        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        int radius = config.getBlockScanHorizontal();
        int vertical = config.getBlockScanVertical();
        int reveal = config.getRevealDistance();
        int revealSq = reveal * reveal;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        Set<BlockPosition> nextMasked = new HashSet<>();
        Set<BlockPosition> oldMasked = currentlyMasked.getOrDefault(player.getUniqueId(), Set.of());

        int minY = Math.max(world.getMinHeight(), cy - vertical);
        int maxY = Math.min(world.getMaxHeight() - 1, cy + vertical);

        for (int x = cx - radius; x <= cx + radius; x++) {
            int dx = x - cx;
            for (int z = cz - radius; z <= cz + radius; z++) {
                int dz = z - cz;
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material original = block.getType();

                    if (!config.getHiddenMaterials().contains(original)) {
                        continue;
                    }

                    int dy = y - cy;
                    int distanceSq = dx * dx + dy * dy + dz * dz;
                    BlockPosition pos = BlockPosition.of(world, x, y, z);

                    if (distanceSq <= revealSq) {
                        player.sendBlockChange(block.getLocation(), original.createBlockData());
                        continue;
                    }

                    Material replacement = replacementForY(world, y);
                    player.sendBlockChange(block.getLocation(), replacement.createBlockData());
                    nextMasked.add(pos);
                }
            }
        }

        for (BlockPosition pos : oldMasked) {
            if (nextMasked.contains(pos)) {
                continue;
            }

            if (!isInsideCurrentScan(world, pos, cx, cy, cz, radius, vertical)) {
                continue;
            }

            World posWorld = plugin.getServer().getWorld(pos.worldId());
            if (posWorld == null) {
                continue;
            }

            Block block = posWorld.getBlockAt(pos.x(), pos.y(), pos.z());
            player.sendBlockChange(block.getLocation(), block.getBlockData());
        }

        currentlyMasked.put(player.getUniqueId(), nextMasked);
    }

    public void applyChunkMask(Player player, Chunk chunk) {
        if (!config.isBlockHideEnabled() || config.getHiddenMaterials().isEmpty()) {
            return;
        }

        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null || !world.getUID().equals(chunk.getWorld().getUID())) {
            return;
        }

        int reveal = config.getRevealDistance();
        int revealSq = reveal * reveal;
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        Set<BlockPosition> maskedForPlayer = currentlyMasked.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());

        int startX = chunk.getX() << 4;
        int startZ = chunk.getZ() << 4;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        for (int x = startX; x < startX + 16; x++) {
            int dx = x - cx;
            for (int z = startZ; z < startZ + 16; z++) {
                int dz = z - cz;
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material original = block.getType();
                    if (!config.getHiddenMaterials().contains(original)) {
                        continue;
                    }

                    int dy = y - cy;
                    int distanceSq = dx * dx + dy * dy + dz * dz;
                    BlockPosition pos = BlockPosition.of(world, x, y, z);

                    if (distanceSq <= revealSq) {
                        player.sendBlockChange(block.getLocation(), original.createBlockData());
                        maskedForPlayer.remove(pos);
                        continue;
                    }

                    Material replacement = replacementForY(world, y);
                    player.sendBlockChange(block.getLocation(), replacement.createBlockData());
                    maskedForPlayer.add(pos);
                }
            }
        }

        if (maskedForPlayer.isEmpty()) {
            currentlyMasked.remove(player.getUniqueId());
        }
    }

    private boolean isInsideCurrentScan(World currentWorld, BlockPosition pos, int cx, int cy, int cz, int radius, int vertical) {
        if (!pos.worldId().equals(currentWorld.getUID())) {
            return false;
        }

        int dx = Math.abs(pos.x() - cx);
        int dy = Math.abs(pos.y() - cy);
        int dz = Math.abs(pos.z() - cz);
        return dx <= radius && dy <= vertical && dz <= radius;
    }

    public void restorePlayer(Player player) {
        Set<BlockPosition> masked = currentlyMasked.remove(player.getUniqueId());
        if (masked == null || masked.isEmpty()) {
            return;
        }

        for (BlockPosition pos : masked) {
            World world = plugin.getServer().getWorld(pos.worldId());
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            player.sendBlockChange(block.getLocation(), block.getBlockData());
        }
    }

    private Material replacementForY(World world, int y) {
        if (isDeepslateLayer(world, y)) {
            return Material.DEEPSLATE;
        }
        return Material.STONE;
    }

    private boolean isDeepslateLayer(World world, int y) {
        int min = world.getMinHeight();
        int max = world.getMaxHeight() - 1;

        if (y <= min + 8) {
            return true;
        }

        if (y >= 0) {
            return false;
        }

        int gradientTop = Math.min(0, max);
        int gradientBottom = min + 8;
        if (gradientTop <= gradientBottom) {
            return true;
        }

        double ratio = (double) (gradientTop - y) / (double) (gradientTop - gradientBottom);
        return ratio >= 0.5;
    }
}
