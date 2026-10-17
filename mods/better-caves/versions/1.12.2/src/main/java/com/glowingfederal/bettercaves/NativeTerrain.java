package com.glowingfederal.bettercaves;

import java.util.BitSet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraft.world.biome.Biome;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.util.ResourceLocation;

/** Writes only the current native generation buffer. Biome sampling never requests neighbouring chunks. */
final class NativeTerrain implements CaveTerrain {
    private final CaveConfig config;
    private final CaveController controller;
    private final int chunkX, chunkZ, seaLevel;
    private final int[] surfaces = new int[256];
    private final boolean[] oceans = new boolean[400];
    private final BitSet water = new BitSet(65536), airMask = new BitSet(65536), liquidMask = new BitSet(65536);
    private final ChunkPrimer primer;
    private final IBlockState lava, liquidWater;
    private final Biome[] biomes;

    NativeTerrain(World world, int chunkX, int chunkZ, ChunkPrimer primer, CaveConfig config, CaveController controller, Liquids liquids) {
        this.config = config; this.controller = controller; this.chunkX = chunkX; this.chunkZ = chunkZ;
        this.primer = primer; seaLevel = world.getSeaLevel();
        lava = liquids.lava; liquidWater = liquids.water;
        biomes = world.getBiomeProvider().getBiomes(null, chunkX * 16 - 2, chunkZ * 16 - 2, 20, 20);
        for (int i = 0; i < oceans.length; i++) oceans[i] = BiomeDictionary.hasType(biomes[i], BiomeDictionary.Type.OCEAN);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int surface = 0;
            for (int y = 255; y >= 0; y--) {
                Block block = block(x, y, z);
                if (block.getDefaultState().getMaterial() == Material.WATER) water.set(index(x, y, z));
                if (surface == 0 && block.getDefaultState().getMaterial().blocksMovement()) surface = y;
            }
            surfaces[x | z << 4] = surface;
        }
    }

    static final class Liquids {
        final IBlockState lava, water;
        Liquids(CaveConfig config) {
            lava = resolve(config.lavaBlock, Blocks.LAVA);
            water = resolve(config.waterBlock, Blocks.WATER);
        }
    }

    private static int index(int x, int y, int z) { return x << 12 | z << 8 | y; }
    private Block block(int x, int y, int z) { return y < 0 || y > 255 ? Blocks.AIR : primer.getBlockState(x, y, z).getBlock(); }
    private void set(int x, int y, int z, Block block) { primer.setBlockState(x, y, z, block.getDefaultState()); }
    private void set(int x, int y, int z, IBlockState state) { primer.setBlockState(x, y, z, state); }
    private static IBlockState resolve(String name, Block fallback) {
        try {
            ResourceLocation key = new ResourceLocation(name);
            if (Block.REGISTRY.containsKey(key)) return Block.REGISTRY.getObject(key).getDefaultState();
        } catch (IllegalArgumentException invalidName) {
            // Invalid registry strings have the same safe fallback as missing blocks.
        }
        NativeHooks.LOG.warn("Unknown Better Caves liquid '{}'; using {}", name, fallback.getRegistryName());
        return fallback.getDefaultState();
    }

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
            for (int y = 1; y < 5; y++) if (block(x, y, z) == Blocks.BEDROCK) set(x, y, z, biome(x, z).fillerBlock);
            // Keep the legacy world's bottom boundary at y=0 even for width=0.
            for (int y = 1; y < config.bedrockWidth; y++) set(x, y, z, Blocks.BEDROCK);
        }
    }
    private Biome biome(int x, int z) { return biomes[x + 2 + (z + 2) * 20]; }
    private boolean carvable(Block b, Block above, int x, int z, boolean flooded) {
        if (b == Blocks.BEDROCK || b == Blocks.FARMLAND || above.getDefaultState().getMaterial() == Material.WOOD ||
            b.getDefaultState().getMaterial() == Material.WOOD || b.getDefaultState().getMaterial() == Material.LEAVES) return false;
        if (b == biome(x, z).topBlock.getBlock() || b == biome(x, z).fillerBlock.getBlock()) return true;
        Material m = b.getDefaultState().getMaterial();
        if (m == Material.ROCK || m == Material.CLAY || m == Material.GROUND) return true;
        if (b == Blocks.SNOW || b == Blocks.PACKED_ICE || b == Blocks.MYCELIUM) return true;
        if (flooded && (b == Blocks.AIR || m == Material.WATER || m == Material.LAVA || b == Blocks.SAND || b == Blocks.GRAVEL)) return true;
        return (b == Blocks.SAND || b == Blocks.GRAVEL) && above.getDefaultState().getMaterial() != Material.WATER;
    }
    private boolean waterAdjacent(int x, int y, int z) {
        return (y < 255 && water.get(index(x, y + 1, z))) ||
            (x > 0 && water.get(index(x - 1, y, z))) || (x < 15 && water.get(index(x + 1, y, z))) ||
            (z > 0 && water.get(index(x, y, z - 1))) || (z < 15 && water.get(index(x, y, z + 1)));
    }
    @Override public void carve(int x, int y, int z, int liquid, boolean flooded) {
        if (y < 1 || y > 255 || (flooded && y >= seaLevel) || (liquid == -1 && y <= config.liquidAltitude)) return;
        int bit = x | z << 4 | y << 8;
        if (airMask.get(bit) || liquidMask.get(bit)) return;
        Block b = block(x, y, z), above = block(x, y + 1, z);
        if (!carvable(b, above, x, z, flooded)) return;
        if (!flooded && y > config.liquidAltitude && waterAdjacent(x, y, z)) return;
        (flooded ? liquidMask : airMask).set(bit);
        if (flooded && liquid == 0 && lava.getBlock() == Blocks.LAVA && y == config.liquidAltitude + 1) {
            long hash = CaveController.mix(controller.seed ^ ((long) (chunkX * 16 + x) * 341873128712L) ^ ((long) (chunkZ * 16 + z) * 132897987541L));
            set(x, y, z, (hash & 3) == 0 ? Blocks.MAGMA : Blocks.OBSIDIAN);
        } else if (y <= config.liquidAltitude) set(x, y, z, liquid == 1 ? liquidWater : lava);
        else {
            if (!flooded && b == biome(x, z).topBlock.getBlock() && block(x, y - 1, z) == biome(x, z).fillerBlock.getBlock()) {
                set(x, y - 1, z, biome(x, z).topBlock);
            }
            if (!flooded && above == Blocks.SAND) set(x, y + 1, z, primer.getBlockState(x, y + 1, z).getValue(net.minecraft.block.BlockSand.VARIANT) == net.minecraft.block.BlockSand.EnumType.RED_SAND ? Blocks.RED_SANDSTONE : Blocks.SANDSTONE);
            if (config.replaceGravel && above == Blocks.GRAVEL) set(x, y + 1, z, Blocks.STONE.getStateFromMeta(5));
            if (flooded) set(x, y, z, Blocks.WATER); else set(x, y, z, Blocks.AIR);
        }
    }
}
