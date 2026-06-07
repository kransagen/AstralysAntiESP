package cz.kransagen.antifreecam;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EntityVisibilityService {

    private final AntiFreeCamPlugin plugin;
    private final ConfigManager config;
    private final Map<UUID, Set<UUID>> hiddenByObserver = new HashMap<>();

    public EntityVisibilityService(AntiFreeCamPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updatePlayer(Player observer) {
        if (!config.isAntiEspEnabled()) {
            restorePlayer(observer);
            return;
        }

        World world = observer.getWorld();
        double radius = config.getAntiEspRadius();
        double radiusSq = config.getAntiEspRadiusSquared();
        double fovThreshold = config.getFovDotThreshold();
        int mobRevealDistance = config.getRevealDistance();
        double mobRevealDistanceSq = (double) mobRevealDistance * mobRevealDistance;

        Location eye = observer.getEyeLocation();
        Vector look = eye.getDirection().normalize();

        Set<UUID> previousHidden = hiddenByObserver.getOrDefault(observer.getUniqueId(), Set.of());
        Set<UUID> nextHidden = new HashSet<>();

        Collection<Entity> nearby = observer.getNearbyEntities(radius, radius, radius);
        for (Entity target : nearby) {
            if (!shouldHandleEntity(target)) {
                continue;
            }

            Location targetLocation = target.getLocation();
            double distanceSq = eye.distanceSquared(targetLocation);
            if (target instanceof Mob) {
                if (distanceSq > radiusSq) {
                    hide(observer, target);
                    nextHidden.add(target.getUniqueId());
                    continue;
                }
                if (distanceSq <= mobRevealDistanceSq) {
                    show(observer, target);
                    continue;
                }

                Vector toTarget = targetLocation.toVector().subtract(eye.toVector());
                if (toTarget.lengthSquared() < 0.0001D) {
                    show(observer, target);
                    continue;
                }

                double dot = look.dot(toTarget.normalize());
                boolean inFov = dot >= fovThreshold;
                if (inFov) {
                    show(observer, target);
                    continue;
                }

                hide(observer, target);
                nextHidden.add(target.getUniqueId());
                continue;
            }

            if (distanceSq > radiusSq) {
                hide(observer, target);
                nextHidden.add(target.getUniqueId());
                continue;
            }

            Vector toTarget = targetLocation.toVector().subtract(eye.toVector());
            if (toTarget.lengthSquared() < 0.0001D) {
                show(observer, target);
                continue;
            }

            double dot = look.dot(toTarget.normalize());
            boolean inFov = dot >= fovThreshold;

            if (inFov) {
                show(observer, target);
                continue;
            }

            hide(observer, target);
            nextHidden.add(target.getUniqueId());
        }

        for (UUID hiddenUuid : previousHidden) {
            if (nextHidden.contains(hiddenUuid)) {
                continue;
            }

            Entity entity = plugin.getServer().getEntity(hiddenUuid);
            if (entity == null || entity.getWorld() != world) {
                continue;
            }

            show(observer, entity);
        }

        if (nextHidden.isEmpty()) {
            hiddenByObserver.remove(observer.getUniqueId());
        } else {
            hiddenByObserver.put(observer.getUniqueId(), nextHidden);
        }
    }

    public void restorePlayer(Player observer) {
        Set<UUID> hidden = hiddenByObserver.remove(observer.getUniqueId());
        if (hidden == null || hidden.isEmpty()) {
            return;
        }

        for (UUID hiddenUuid : hidden) {
            Entity entity = plugin.getServer().getEntity(hiddenUuid);
            if (entity != null) {
                show(observer, entity);
            }
        }
    }

    private boolean shouldHandleEntity(Entity entity) {
        if (entity instanceof Player) {
            return config.isIncludePlayers();
        }
        return config.isIncludeMobs() && entity instanceof Mob;
    }

    private void hide(Player observer, Entity target) {
        if (target.equals(observer)) {
            return;
        }
        observer.hideEntity(plugin, target);
    }

    private void show(Player observer, Entity target) {
        if (target.equals(observer)) {
            return;
        }
        observer.showEntity(plugin, target);
    }
}
