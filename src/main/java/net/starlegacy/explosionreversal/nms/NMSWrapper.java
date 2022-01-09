package net.starlegacy.explosionreversal.nms;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;

import javax.annotation.Nullable;
import java.io.IOException;

public class NMSWrapper {
    @SuppressWarnings("UnstableApiUsage")
    private static byte[] serialize(CompoundTag nbt) throws IOException {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        NbtIo.writeUnnamedTag(nbt, output);

        return output.toByteArray();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static CompoundTag deserialize(byte[] bytes) throws IOException {
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        NbtAccounter readLimiter = new NbtAccounter(bytes.length * 10L);
        return NbtIo.read(input, readLimiter);
    }


    public static byte[] completeGetTileEntity(Block block) throws Exception {
        ServerLevel worldServer = ((CraftWorld) block.getWorld()).getHandle();

        BlockPos blockPosition = new BlockPos(block.getX(), block.getY(), block.getZ());

        BlockEntity tileEntity = worldServer.getBlockEntity(blockPosition);
        if (tileEntity == null) {
            return null;
        }

        CompoundTag nbt = tileEntity.saveWithFullMetadata();

        return serialize(nbt);
    }


    public static void completeSetTileEntity(Block block, byte[] bytes) throws IOException {
        ServerLevel worldServer = ((CraftWorld) block.getWorld()).getHandle();

        BlockPos blockPosition = new BlockPos(block.getX(), block.getY(), block.getZ());

        CompoundTag nbt = deserialize(bytes);

        BlockState blockData = worldServer.getBlockState(blockPosition);

        BlockEntity tileEntity = BlockEntity.loadStatic(blockPosition, blockData, nbt);
    }

    private static net.minecraft.world.entity.Entity getNMSEntity(Entity entity) {
        CraftEntity craftEntity = (CraftEntity) entity;
        return craftEntity.getHandle();
    }

    public static byte[] completeGetEntityData(Entity entity) throws IOException {
        net.minecraft.world.entity.Entity nmsEntity = getNMSEntity(entity);
        CompoundTag nbt = nmsEntity.saveWithoutId(new CompoundTag());
        return serialize(nbt);
    }

    public static void completeRestoreEntityData(Entity entity, byte[] entityData) throws IOException {
        net.minecraft.world.entity.Entity nmsEntity = getNMSEntity(entity);
        CompoundTag nbt = deserialize(entityData);
        nmsEntity.load(nbt);
    }
}
