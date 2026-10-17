package com.glowingfederal.bettercaves;

import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.World;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenRavine;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraftforge.event.terraingen.ChunkProviderEvent;

/** Forge's native map-generator replacement hook; no bytecode interception is necessary. */
public final class NativeHooks {
    static final Logger LOG = LogManager.getLogger("YungsLegacy/BetterCaves");
    private final File directory;
    // Weak world identity keys; cached values contain settings/noise only, never the world.
    private final Map<World, State> worlds = new WeakHashMap<World, State>();
    // The 1.7.10 carving signature omits metadata. Capture the matching native buffer before surface replacement.
    // Keys are weak, values have no reference back to block arrays or worlds. Remove after the ravine phase.
    private final Map<Block[], byte[]> metadata = new WeakHashMap<Block[], byte[]>();
    @SubscribeEvent public void surface(ChunkProviderEvent.ReplaceBiomeBlocks event) {
        if (event.chunkProvider.getClass() == ChunkProviderGenerate.class && event.blockArray != null &&
            event.blockArray.length == 65536 && event.metaArray != null && event.metaArray.length == 65536) {
            metadata.put(event.blockArray, event.metaArray);
        }
    }
    public NativeHooks(File directory) { this.directory = directory; }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void init(InitMapGenEvent event) {
        MapGenBase original = event.newGen;
        if (event.type == InitMapGenEvent.EventType.CAVE && original.getClass() == MapGenCaves.class) {
            event.newGen = new Caves(this, original);
        } else if (event.type == InitMapGenEvent.EventType.RAVINE && original.getClass() == MapGenRavine.class) {
            event.newGen = new Ravines(this, original);
        }
        // Other mods' custom map generators retain ownership. Unsupported providers delegate unchanged below.
    }
    @SubscribeEvent public void unload(WorldEvent.Unload event) {
        worlds.remove(event.world);
        metadata.clear();
    }
    private State state(World world) {
        State state = worlds.get(world);
        if (state == null || state.seed != world.getSeed()) {
            CaveConfig config;
            try { config = CaveConfigLoader.load(directory, world.provider.dimensionId); }
            catch (RuntimeException failure) {
                LOG.error("Better Caves configuration failed; retaining vanilla generation", failure);
                config = new CaveConfig(); config.enabled = false;
            }
            state = new State(world.getSeed(), config, config.enabledIn(world.provider.dimensionId));
            worlds.put(world, state);
            LOG.info("Better Caves {} in dimension {} with seed {}", state.controller == null ? "disabled (vanilla fallback)" : "enabled", world.provider.dimensionId, world.getSeed());
        }
        return state;
    }
    private static final class State {
        final long seed;
        final CaveConfig config;
        final CaveController controller;
        final NativeTerrain.Liquids liquids;
        State(long seed, CaveConfig config, boolean enabled) {
            this.seed = seed; this.config = config;
            liquids = new NativeTerrain.Liquids(config);
            controller = enabled ? new CaveController(seed, config) : null;
        }
    }

    private static final class Caves extends MapGenBase {
        final NativeHooks owner;
        final MapGenBase fallback;
        Caves(NativeHooks owner, MapGenBase fallback) { this.owner = owner; this.fallback = fallback; }
        @Override public void func_151539_a(IChunkProvider provider, World world, int x, int z, Block[] buffer) {
            State state = owner.state(world);
            if (provider.getClass() != ChunkProviderGenerate.class || buffer == null || buffer.length != 65536 || state.controller == null) { fallback.func_151539_a(provider, world, x, z, buffer); return; }
            byte[] meta = owner.metadata.get(buffer);
            if (meta == null) { fallback.func_151539_a(provider, world, x, z, buffer); return; }
            NativeTerrain terrain = new NativeTerrain(world, x, z, buffer, meta, state.config, state.controller, state.liquids);
            state.controller.carve(terrain, x, z);
        }
    }

    private static final class Ravines extends MapGenRavine {
        final NativeHooks owner;
        final MapGenBase fallback;
        private NativeTerrain terrain;
        private State active;
        Ravines(NativeHooks owner, MapGenBase fallback) { this.owner = owner; this.fallback = fallback; }
        @Override public void func_151539_a(IChunkProvider provider, World world, int x, int z, Block[] buffer) {
            try {
                State state = owner.state(world);
                if (provider.getClass() != ChunkProviderGenerate.class || buffer == null || buffer.length != 65536 || state.controller == null) { fallback.func_151539_a(provider, world, x, z, buffer); return; }
                byte[] meta = owner.metadata.get(buffer);
                if (meta == null) { fallback.func_151539_a(provider, world, x, z, buffer); return; }
                if (!state.config.ravines) return;
                active = state;
                terrain = new NativeTerrain(world, x, z, buffer, meta, state.config, state.controller, state.liquids);
                super.func_151539_a(provider, world, x, z, buffer);
            } finally {
                terrain = null; active = null; this.worldObj = null;
                owner.metadata.remove(buffer);
            }
        }
        @Override protected boolean isOceanBlock(Block[] buffer, int index, int x, int y, int z, int chunkX, int chunkZ) {
            return active == null || !active.config.floodedRavines ? super.isOceanBlock(buffer, index, x, y, z, chunkX, chunkZ) : false;
        }
        @Override protected void digBlock(Block[] buffer, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
            boolean flooded = active.config.floodedRavines && terrain.ocean(x, z);
            if (active.config.floodedRavines && terrain.oceanDistance(x, z) <= .25f) return;
            terrain.carve(x, y, z, active.controller.liquidAt(chunkX * 16 + x, chunkZ * 16 + z), flooded);
        }
    }
}
