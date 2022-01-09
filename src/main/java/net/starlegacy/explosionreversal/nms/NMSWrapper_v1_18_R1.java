package net.starlegacy.explosionreversal.nms;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

public class NMSWrapper_v1_18_R1 implements NMSWrapper {
    @SuppressWarnings("UnstableApiUsage")
    private byte[] serialize(CompoundTag nbt) throws IOException {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        NBTCompressedStreamTools.a(nbt, output);

        return output.toByteArray();
    }

    @SuppressWarnings("UnstableApiUsage")
    private CompoundTag deserialize(byte[] bytes) throws IOException {
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        NBTReadLimiter readLimiter = new NBTReadLimiter(bytes.length * 10);
        return NBTCompressedStreamTools.a(input, readLimiter);
    }

    // separate method for graceful failure on version incompatibility
    @Nullable
    public byte[] getTileEntity(Block block) {
        try {
            return completeGetTileEntity(block);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private byte[] completeGetTileEntity(Block block) throws Exception {
        WorldServer worldServer = ((CraftWorld) block.getWorld()).getHandle();

        BlockPos blockPosition = new BlockPos(block.getX(), block.getY(), block.getZ());

        TileEntity tileEntity = worldServer.getTileEntity(blockPosition);
        if (tileEntity == null) {
            return null;
        }

        CompoundTag nbt = tileEntity.save(new CompoundTag());

        return serialize(nbt);
    }

    public void setTileEntity(Block block, byte[] bytes) {
        try {
            completeSetTileEntity(block, bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void completeSetTileEntity(Block block, byte[] bytes) throws IOException {
        WorldServer worldServer = ((CraftWorld) block.getWorld()).getHandle();

        BlockPos blockPosition = new BlockPos(block.getX(), block.getY(), block.getZ());

        CompoundTag nbt = deserialize(bytes);

        IBlockData blockData = worldServer.getType(blockPosition);

        TileEntity tileEntity = Objects.requireNonNull(TileEntity.create(blockData, nbt));
        tileEntity.setPosition(blockPosition);

        worldServer.removeTileEntity(blockPosition);
        worldServer.setTileEntity(blockPosition, tileEntity);
    }

    private net.minecraft.server.v1_16_R3.Entity getNMSEntity(Entity entity) {
        CraftEntity craftEntity = (CraftEntity) entity;
        return craftEntity.getHandle();
    }

    @Nullable
    public byte[] getEntityData(Entity entity) {
        try {
            return completeGetEntityData(entity);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private byte[] completeGetEntityData(Entity entity) throws IOException {
        net.minecraft.server.v1_18_R1.Entity nmsEntity = getNMSEntity(entity);
        CompoundTag nbt = nmsEntity.save(new CompoundTag());
        return serialize(nbt);
    }

    public void restoreEntityData(Entity entity, byte[] entityData) {
        try {
            completeRestoreEntityData(entity, entityData);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void completeRestoreEntityData(Entity entity, byte[] entityData) throws IOException {
        net.minecraft.server.v1_18_R1.Entity nmsEntity = getNMSEntity(entity);
        CompoundTag nbt = deserialize(entityData);
        nmsEntity.load(nbt);
    }
}
