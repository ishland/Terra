package com.dfsek.terra.bukkit.world;

import org.bukkit.Location;
import org.bukkit.generator.LimitedRegion;

import com.dfsek.terra.api.block.entity.BlockEntity;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.entity.Entity;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.api.world.chunk.generation.ProtoWorld;
import com.dfsek.terra.bukkit.BukkitEntity;
import com.dfsek.terra.bukkit.generator.BukkitChunkGeneratorWrapper;
import com.dfsek.terra.bukkit.world.block.data.BukkitBlockState;
import com.dfsek.terra.bukkit.world.block.state.BukkitBlockEntity;
import com.dfsek.terra.bukkit.world.entity.BukkitEntityType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;


public class BukkitProtoWorld implements ProtoWorld {
    private final LimitedRegion delegate;
    private final BlockState air;
    private static final Logger LOGGER = LoggerFactory.getLogger(BukkitProtoWorld.class);
    
    public BukkitProtoWorld(LimitedRegion delegate, BlockState air) {
        this.delegate = delegate;
        this.air = air;
    }
    
    @Override
    public LimitedRegion getHandle() {
        return delegate;
    }
    
    @Override
    public void setBlockState(int x, int y, int z, BlockState data, boolean physics) {
        access(x, y, z, () -> {
            delegate.setBlockData(x, y, z, BukkitAdapter.adapt(data));
            if(physics) {
                delegate.scheduleBlockUpdate(x, y, z);
            }
        });
    }
    
    @Override
    public long getSeed() {
        return delegate.getWorld().getSeed();
    }
    
    @Override
    public int getMaxHeight() {
        return delegate.getWorld().getMaxHeight();
    }
    
    @Override
    public BlockState getBlockState(int x, int y, int z) {
        return access(x, y, z, () -> BukkitBlockState.newInstance(delegate.getBlockData(x, y, z))).orElse(air);
    }
    
    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        return access(x, y, z, () -> BukkitBlockEntity.newInstance(delegate.getBlockState(x, y, z))).orElse(null);
    }
    
    @Override
    public int getMinHeight() {
        return delegate.getWorld().getMinHeight();
    }
    
    @Override
    public Entity spawnEntity(double x, double y, double z, EntityType entityType) {
        return access((int) x, (int) y, (int) z, () -> new BukkitEntity(
                delegate.spawnEntity(new Location(delegate.getWorld(), x, y, z), ((BukkitEntityType) entityType).getHandle()))).orElse(null);
    }
    
    @Override
    public ChunkGenerator getGenerator() {
        return ((BukkitChunkGeneratorWrapper) delegate.getWorld().getGenerator()).getHandle();
    }
    
    @Override
    public BiomeProvider getBiomeProvider() {
        return ((BukkitChunkGeneratorWrapper) delegate.getWorld().getGenerator()).getPack().getBiomeProvider();
    }
    
    @Override
    public ConfigPack getPack() {
        return ((BukkitChunkGeneratorWrapper) delegate.getWorld().getGenerator()).getPack();
    }
    
    @Override
    public int centerChunkX() {
        return delegate.getCenterChunkX();
    }
    
    @Override
    public int centerChunkZ() {
        return delegate.getCenterChunkZ();
    }
    
    @Override
    public ServerWorld getWorld() {
        return new BukkitServerWorld(delegate.getWorld());
    }
    
    private <T> Optional<T> access(int x, int y, int z, Supplier<T> action) {
        if(delegate.isInRegion(x, y, z)) {
            return Optional.of(action.get());
        } else {
            LOGGER.warn("Detected world access at coordinates out of bounds: ({}, {}, {}) accessed for region [{}, {}]", x, y, z,
                        delegate.getCenterChunkX(), delegate.getCenterChunkZ());
            return Optional.empty();
        }
    }
    
    private void access(int x, int y, int z, Runnable action) {
        if(delegate.isInRegion(x, y, z)) {
            action.run();
        } else {
            LOGGER.warn("Detected world access at coordinates out of bounds: ({}, {}, {}) accessed for region [{}, {}]", x, y, z,
                        delegate.getCenterChunkX(), delegate.getCenterChunkZ());
        }
    }
}
