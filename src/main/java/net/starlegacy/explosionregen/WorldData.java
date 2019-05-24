package net.starlegacy.explosionregen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Files;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class WorldData {
    private LoadingCache<World, List<ExplodedBlockData>> explodedBlocks = CacheBuilder.newBuilder()
            .weakKeys()
            .build(CacheLoader.from(this::load));

    private File getFile(World world) {
        return new File(world.getWorldFolder(), "data/explosion_regen.dat");
    }

    public List<ExplodedBlockData> get(World world) {
        return explodedBlocks.getUnchecked(world);
    }

    private List<ExplodedBlockData> load(World world) {
        File file = getFile(world);

        if (!file.exists()) {
            return new LinkedList<>();
        }

        boolean errorOccured = false;

        List<ExplodedBlockData> blocks = new LinkedList<>();

        try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {
            int paletteSize = input.readInt();
            List<BlockData> palette = new ArrayList<>();
            for (int i = 0; i < paletteSize; i++) {
                byte[] blockDataBytes = new byte[input.readInt()];
                input.read(blockDataBytes);
                String blockDataString = new String(blockDataBytes);
                palette.add(Bukkit.createBlockData(blockDataString));
            }

            int blockCount = input.readInt();
            for (int i = 0; i < blockCount; i++) {
                try {
                    int x = input.readInt();
                    int y = input.readInt();
                    int z = input.readInt();

                    long explodedTime = input.readLong();

                    BlockData blockData = palette.get(input.readInt());

                    byte[] tileEntityData = null;

                    if (input.readBoolean()) {
                        int tileEntitySize = input.readInt();
                        tileEntityData = new byte[tileEntitySize];
                        input.read(tileEntityData);
                    }

                    blocks.add(new ExplodedBlockData(x, y, z, explodedTime, blockData, tileEntityData));
                } catch (Exception exception) {
                    // if something goes wrong with a block, skip it but print the error,
                    // and flag that we need to save a backup copy of this file,
                    // in case data recovery is necessary later
                    // do this so that we avoid losing other blocks if only some blocks are corrupt
                    exception.printStackTrace();
                    errorOccured = true;
                }
            }

            return blocks;
        } catch (Exception e) {
            errorOccured = true;
            e.printStackTrace();
            return blocks;
        } finally {
            if (errorOccured) {
                file.renameTo(new File(file.getAbsolutePath() + "_broken_" + System.currentTimeMillis() % 1000));
                save(world);
            }
        }
    }

    public void save(World world) {

        File file = getFile(world);
        file.getParentFile().mkdirs();

        File tmpFile = new File(file.getAbsolutePath() + "_tmp");

        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(tmpFile))) {
            List<ExplodedBlockData> data = get(world);

            List<BlockData> palette = data.stream().map(ExplodedBlockData::getBlockData).distinct().collect(Collectors.toList());
            Map<BlockData, Integer> values = new Object2IntOpenHashMap<>(palette.size());

            output.writeInt(palette.size());

            for (int i = 0; i < palette.size(); i++) {
                BlockData value = palette.get(i);
                String valueString = value.getAsString(true);
                output.writeInt(valueString.length());
                output.writeBytes(valueString);
                values.put(value, i);
            }

            output.writeInt(data.size());

            for (ExplodedBlockData block : data) {
                output.writeInt(block.getX());
                output.writeInt(block.getY());
                output.writeInt(block.getZ());

                output.writeLong(block.getExplodedTime());

                output.writeInt(values.get(block.getBlockData()));

                byte[] tileData = block.getTileData();
                if (tileData == null) {
                    output.writeBoolean(false);
                } else {
                    output.writeBoolean(true);
                    output.writeInt(tileData.length);
                    output.write(tileData);
                }
            }

            Files.move(tmpFile, file);
        } catch (IOException e) {
            e.printStackTrace();
            tmpFile.delete();
        }
    }

    public void addAll(World world, Collection<ExplodedBlockData> explodedBlockData) {
        get(world).addAll(explodedBlockData);
    }
}
