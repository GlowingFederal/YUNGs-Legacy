package com.glowingfederal.bettercaves;

import java.util.BitSet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraft.world.biome.BiomeGenBase;

/** Writes only the current native generation buffer. Biome sampling never requests neighbouring chunks. */
final class NativeTerrain implements CaveTerrain {
    private final CaveConfig config;
    private final CaveController controller;
    private final int chunkX, chunkZ, seaLevel;
    private final int[] surfaces = new int[256];
    private final boolean[] oceans = new boolean[400];
    private final BitSet water = new BitSet(32768), airMask = new BitSet(32768), liquidMask = new BitSet(32768);
    private final byte[] blocks;
    private final Block lava, liquidWater;
    private final BiomeGenBase[] biomes;

    NativeTerrain(World world, int chunkX, int chunkZ, byte[] blocks, CaveConfig config, CaveController controller, Liquids liquids) {
        this.config = config; this.controller = controller; this.chunkX = chunkX; this.chunkZ = chunkZ;
        this.blocks = blocks; seaLevel = 63;
        lava = liquids.lava; liquidWater = liquids.water;
        biomes = world.getWorldChunkManager().loadBlockGeneratorData(null, chunkX * 16 - 2, chunkZ * 16 - 2, 20, 20);
        for (int i = 0; i < oceans.length; i++) oceans[i] = isOcean(biomes[i]);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int surface = 0;
            for (int y = 127; y >= 0; y--) {
                Block block = block(x, y, z);
                if (material(block) == Material.water) water.set(index(x, y, z));
                if (surface == 0 && material(block).blocksMovement()) surface = y;
            }
            surfaces[x | z << 4] = surface;
        }
    }

    // Forge 1.6.4 has WATER, not OCEAN; rivers must remain dry.
    private static boolean isOcean(BiomeGenBase biome) {
        return biome instanceof net.minecraft.world.biome.BiomeGenOcean ||
            (biome.minHeight < -.5f && BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.WATER));
    }

    static final class Liquids {
        final Block lava, water;
        Liquids(CaveConfig config) {
            lava = resolve(config.lavaBlock, Block.lavaStill);
            water = resolve(config.waterBlock, Block.waterStill);
        }
    }

    private static int index(int x, int y, int z) { return x << 11 | z << 7 | y; }
    private Block block(int x, int y, int z) {
        if (y < 0 || y > 127) return null;
        return Block.blocksList[blocks[index(x, y, z)] & 255];
    }
    // The native provider constructs Chunk from byte IDs only: every generated block has metadata zero.
    private void set(int x, int y, int z, Block block) {
        blocks[index(x, y, z)] = (byte) (block == null ? 0 : block.blockID);
    }
    private static Block resolve(String name, Block fallback) {
        Block block = null;
        if ("minecraft:lava".equals(name)) block = Block.lavaStill;
        else if ("minecraft:water".equals(name)) block = Block.waterStill;
        else if ("minecraft:flowing_lava".equals(name)) block = Block.lavaMoving;
        else if ("minecraft:flowing_water".equals(name)) block = Block.waterMoving;
        else if ("minecraft:air".equals(name)) return null;
        else {
            int separator = name.indexOf(':');
            if (separator > 0) block = GameRegistry.findBlock(name.substring(0, separator), name.substring(separator + 1));
        }
        if (block != null && block.blockID > 0 && block.blockID <= 255) return block;
        NativeHooks.LOG.warning("Unknown or unrepresentable Better Caves liquid '" + name + "'; using block ID " + fallback.blockID);
        return fallback;
    }
    private static Material material(Block block) { return block == null ? Material.air : block.blockMaterial; }
    private static Block biomeBlock(byte id) { return Block.blocksList[id & 255]; }

    @Override public int surface(int x, int z) { return surfaces[x | z << 4]; }
    @Override public boolean ocean(int x, int z) { return oceans[x + 2 + (z + 2) * 20]; }
    @Override public float oceanDistance(int x, int z) {
        boolean ocean = ocean(x, z);
        int distance = 4;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (ocean(x + dx, z + dz) != ocean) distance = Math.min(distance, Math.abs(dx) + Math.abs(dz));
        }
        return distance / 4f;
    }
    @Override public void flattenBedrock() {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            for (int y = 1; y < 5; y++) if (block(x, y, z) == Block.bedrock) set(x, y, z, biomeBlock(biome(x, z).fillerBlock));
            // Keep the legacy world's bottom boundary at y=0 even for width=0.
            for (int y = 1; y < Math.min(128, config.bedrockWidth); y++) set(x, y, z, Block.bedrock);
        }
    }
    private BiomeGenBase biome(int x, int z) { return biomes[x + 2 + (z + 2) * 20]; }
    private boolean carvable(Block b, Block above, int x, int z, boolean flooded) {
        if (b == Block.bedrock || b == Block.tilledField || material(above) == Material.wood ||
            material(b) == Material.wood || material(b) == Material.leaves) return false;
        if (b == biomeBlock(biome(x, z).topBlock) || b == biomeBlock(biome(x, z).fillerBlock)) return true;
        Material m = material(b);
        if (m == Material.rock || m == Material.clay || m == Material.ground) return true;
        if (b == Block.blockSnow || b == Block.mycelium) return true;
        if (flooded && (b == null || m == Material.water || m == Material.lava || b == Block.sand || b == Block.gravel)) return true;
        return (b == Block.sand || b == Block.gravel) && material(above) != Material.water;
    }
    private boolean waterAdjacent(int x, int y, int z) {
        return (y < 127 && water.get(index(x, y + 1, z))) ||
            (x > 0 && water.get(index(x - 1, y, z))) || (x < 15 && water.get(index(x + 1, y, z))) ||
            (z > 0 && water.get(index(x, y, z - 1))) || (z < 15 && water.get(index(x, y, z + 1)));
    }
    @Override public void carve(int x, int y, int z, int liquid, boolean flooded) {
        if (y < 1 || y > 127 || (flooded && y >= seaLevel) || (liquid == -1 && y <= config.liquidAltitude)) return;
        int bit = x | z << 4 | y << 8;
        if (airMask.get(bit) || liquidMask.get(bit)) return;
        Block b = block(x, y, z), above = block(x, y + 1, z);
        if (!carvable(b, above, x, z, flooded)) return;
        if (!flooded && y > config.liquidAltitude && waterAdjacent(x, y, z)) return;
        (flooded ? liquidMask : airMask).set(bit);
        if (flooded && liquid == 0 && lava == Block.lavaStill && y == config.liquidAltitude + 1) {
            set(x, y, z, Block.obsidian); // No magma block exists on 1.6.4.
        } else if (y <= config.liquidAltitude) set(x, y, z, liquid == 1 ? liquidWater : lava);
        else {
            if (!flooded && b == biomeBlock(biome(x, z).topBlock) && block(x, y - 1, z) == biomeBlock(biome(x, z).fillerBlock)) {
                set(x, y - 1, z, biomeBlock(biome(x, z).topBlock));
            }
            if (!flooded && above == Block.sand) set(x, y + 1, z, Block.sandStone); // 1.6.4 has no red sandstone.
            if (config.replaceGravel && above == Block.gravel) set(x, y + 1, z, Block.stone); // Native equivalent of the absent andesite.
            if (flooded) set(x, y, z, Block.waterStill); else set(x, y, z, null);
        }
    }
}
