package cz.kransagen.antifreecam;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public record BlockPosition(UUID worldId, int x, int y, int z) {

    public static BlockPosition from(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalStateException("Location has no world");
        }
        return new BlockPosition(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static BlockPosition of(World world, int x, int y, int z) {
        return new BlockPosition(world.getUID(), x, y, z);
    }
}
