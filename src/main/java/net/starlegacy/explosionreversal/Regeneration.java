package net.starlegacy.explosionreversal;

import net.starlegacy.explosionreversal.data.ExplodedBlockData;
import net.starlegacy.explosionreversal.data.ExplodedEntityData;
import net.starlegacy.explosionreversal.nms.NMSWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Regeneration {
    private static final Logger logger = Logger.getLogger(Regeneration.class.getName());

    public static void pulse(ExplosionReversalPlugin plugin) throws IOException {
        regenerateBlocks(plugin, false);
        regenerateEntities(plugin, false);
    }

    public static int regenerateBlocks(ExplosionReversalPlugin plugin, boolean instant) throws IOException {
        long millisecondDelay = (long) (plugin.getSettings().getRegenDelay() * 60L * 1_000L);
        long maxNanos = (long) (plugin.getSettings().getPlacementIntensity() * 1_000_000L);

        long start = System.nanoTime();

        int regenerated = 0;

        for (World world : Bukkit.getWorlds()) {
            List<ExplodedBlockData> blocks = plugin.getWorldData().getBlocks(world);

            for (Iterator<ExplodedBlockData> iterator = blocks.iterator(); iterator.hasNext(); ) {
                ExplodedBlockData data = iterator.next();
                if (!instant) {
                    if (System.nanoTime() - start > maxNanos) { // i.e. taking too long
                        return regenerated;
                    }

                    if (System.currentTimeMillis() - data.getExplodedTime() < millisecondDelay) {
                        continue;
                    }
                }

                iterator.remove();
                regenerateBlock(world, data);

                regenerated++;
            }
        }

        return regenerated;
    }

    private static void regenerateBlock(World world, ExplodedBlockData data) throws IOException {
        Block block = world.getBlockAt(data.getX(), data.getY(), data.getZ());
        BlockData blockData = data.getBlockData();
        block.setBlockData(blockData, false);

        @Nullable byte[] tileData = data.getTileData();
        if (tileData != null) {
            NMSWrapper.completeSetTileEntity(block, tileData);
        }
    }

    public static int regenerateEntities(ExplosionReversalPlugin plugin, boolean instant) throws IOException {
        long millisecondDelay = (long) (plugin.getSettings().getRegenDelay() * 60L * 1_000L);
        int regenerated = 0;

        for (World world : Bukkit.getWorlds()) {
            List<ExplodedEntityData> entities = plugin.getWorldData().getEntities(world);
            for (Iterator<ExplodedEntityData> iterator = entities.iterator(); iterator.hasNext(); ) {
                ExplodedEntityData data = iterator.next();
                if (!instant && System.currentTimeMillis() - data.getExplodedTime() < millisecondDelay) {
                    continue;
                }
                regenerateEntity(world, data);
                iterator.remove();
                regenerated++;
            }
        }

        return regenerated;
    }

    private static void regenerateEntity(World world, ExplodedEntityData data) throws IOException {
        Location location = new Location(world, data.getX(), data.getY(), data.getZ(), data.getPitch(), data.getYaw());

        EntityType entityType = data.getEntityType();

        Entity entity;
        try {
            entity = world.spawnEntity(location, entityType);
        } catch (IllegalArgumentException exception) {
            logger.log(Level.SEVERE, "Failed to regenerate " + entityType
                    + " at " + data.getX() + ", " + data.getY() + ", " + data.getZ(), exception);
            return;
        }

        @Nullable byte[] nmsData = data.getNmsData();
        if (nmsData != null) {
            NMSWrapper.completeRestoreEntityData(entity, nmsData);
        }

        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.setHealth(livingEntity.getHealth());
        }

        if (entity instanceof Painting) {
            Painting painting = (Painting) entity;
            painting.setArt(painting.getArt(), true);
        }
    }
}