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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.chunk.ChunkPrimer;

/** Forge's native map-generator replacement hook; no bytecode interception is necessary. */
public final class NativeHooks {
    static final Logger LOG = LogManager.getLogger("YungsLegacy/BetterCaves");
    private final File directory;
    // Weak world identity keys; cached values contain settings/noise only, never the world.
    private final Map<World, State> worlds = new WeakHashMap<World, State>();
    public NativeHooks(File directory) { this.directory = directory; }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void init(InitMapGenEvent event) {
        MapGenBase original = event.getNewGen();
        if (event.getType() == InitMapGenEvent.EventType.CAVE && original.getClass() == MapGenCaves.class) {
            event.setNewGen(new Caves(this, original));
        } else if (event.getType() == InitMapGenEvent.EventType.RAVINE && original.getClass() == MapGenRavine.class) {
            event.setNewGen(new Ravines(this, original));
        }
        // Other mods' custom map generators retain ownership. Unsupported providers delegate unchanged below.
    }
    @SubscribeEvent public void unload(WorldEvent.Unload event) {
        worlds.remove(event.getWorld());

    }
    private State state(World world) {
        State state = worlds.get(world);
        if (state == null || state.seed != world.getSeed()) {
            CaveConfig config;
            try { config = CaveConfigLoader.load(directory, world.provider.getDimension()); }
            catch (RuntimeException failure) {
                LOG.error("Better Caves configuration failed; retaining vanilla generation", failure);
                config = new CaveConfig(); config.enabled = false;
            }
            state = new State(world.getSeed(), config, config.enabledIn(world.provider.getDimension()));
            worlds.put(world, state);
            LOG.info("Better Caves {} in dimension {} with seed {}", state.controller == null ? "disabled (vanilla fallback)" : "enabled", world.provider.getDimension(), world.getSeed());
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
        @Override public void generate(World world, int x, int z, ChunkPrimer buffer) {
            State state = owner.state(world);
            if (!(world instanceof WorldServer) || ((WorldServer) world).getChunkProvider().chunkGenerator.getClass() != ChunkGeneratorOverworld.class || buffer == null || state.controller == null) { fallback.generate(world, x, z, buffer); return; }

            NativeTerrain terrain = new NativeTerrain(world, x, z, buffer, state.config, state.controller, state.liquids);
            state.controller.carve(terrain, x, z);
        }
    }

    private static final class Ravines extends MapGenRavine {
        final NativeHooks owner;
        final MapGenBase fallback;
        private NativeTerrain terrain;
        private State active;
        Ravines(NativeHooks owner, MapGenBase fallback) { this.owner = owner; this.fallback = fallback; }
        @Override public void generate(World world, int x, int z, ChunkPrimer buffer) {
            try {
                State state = owner.state(world);
                if (!(world instanceof WorldServer) || ((WorldServer) world).getChunkProvider().chunkGenerator.getClass() != ChunkGeneratorOverworld.class || buffer == null || state.controller == null) { fallback.generate(world, x, z, buffer); return; }

                if (!state.config.ravines) return;
                active = state;
                terrain = new NativeTerrain(world, x, z, buffer, state.config, state.controller, state.liquids);
                super.generate(world, x, z, buffer);
            } finally {
                terrain = null; active = null; this.world = null;

            }
        }
        @Override protected boolean isOceanBlock(ChunkPrimer buffer, int x, int y, int z, int chunkX, int chunkZ) {
            return active == null || !active.config.floodedRavines ? super.isOceanBlock(buffer, x, y, z, chunkX, chunkZ) : false;
        }
        @Override protected void digBlock(ChunkPrimer buffer, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
            boolean flooded = active.config.floodedRavines && terrain.ocean(x, z);
            if (active.config.floodedRavines && terrain.oceanDistance(x, z) <= .25f) return;
            terrain.carve(x, y, z, active.controller.liquidAt(chunkX * 16 + x, chunkZ * 16 + z), flooded);
        }
    }
}
