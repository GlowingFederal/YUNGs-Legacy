package com.glowingfederal.bettercaves;

import com.glowingfederal.bettercaves.noise.NoiseGen;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Checks shared generation decisions; native terrain and server acceptance are separate. */
public class CaveDeterminismTest {
    private static final long SEED = -582048527119L;
    private static final int[][] CHUNKS = {{0, 0}, {1, 0}, {-1, 0}, {0, -1}, {-1, -1}, {-2, -1}, {27, -43}};

    private static final class Decisions implements CaveTerrain {
        final byte[] blocks = new byte[65536];
        final int cx, cz;
        Decisions(int cx, int cz) { this.cx = cx; this.cz = cz; }
        public int surface(int x, int z) { return 78; }
        public boolean ocean(int x, int z) { return cx * 16 + x < 0; }
        public float oceanDistance(int x, int z) { return 1; }
        public void flattenBedrock() { blocks[0] = 7; }
        public void carve(int x, int y, int z, int liquid, boolean flooded) {
            assertTrue(x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 1 && y <= 255);
            blocks[x << 12 | z << 8 | y] |= (byte) (1 << (liquid + 1) | (flooded ? 8 : 16));
        }
    }
    private static byte[] generate(CaveController controller, int x, int z) {
        Decisions terrain = new Decisions(x, z);
        controller.carve(terrain, x, z);
        return terrain.blocks;
    }
    @Test public void neighboursAreIndependentOfGenerationOrderAndControllerRecreation() {
        CaveConfig config = new CaveConfig();
        config.vanillaPriority = 3;
        CaveController forward = new CaveController(SEED, config);
        byte[][] expected = new byte[CHUNKS.length][];
        for (int i = 0; i < CHUNKS.length; i++) expected[i] = generate(forward, CHUNKS[i][0], CHUNKS[i][1]);
        CaveController recreated = new CaveController(SEED, newConfiguredSnapshot());
        for (int i = CHUNKS.length - 1; i >= 0; i--) {
            assertArrayEquals(expected[i], generate(recreated, CHUNKS[i][0], CHUNKS[i][1]));
            assertArrayEquals(expected[i], generate(forward, CHUNKS[i][0], CHUNKS[i][1]));
        }
    }
    private static CaveConfig newConfiguredSnapshot() {
        CaveConfig c = new CaveConfig(); c.vanillaPriority = 3; return c;
    }
    @Test public void freshWorldSeedsRemainDistinctAndStable() {
        byte[] first = generate(new CaveController(SEED, new CaveConfig()), -1, 0);
        assertFalse(Arrays.equals(first, generate(new CaveController(SEED + 1, new CaveConfig()), -1, 0)));
        assertArrayEquals(first, generate(new CaveController(SEED, new CaveConfig()), -1, 0));
    }
    @Test public void latticeCornersUseGlobalCoordinatesAcrossSignedChunkEdges() {
        CaveConfig config = new CaveConfig();
        for (CaveConfig.Shape shape : new CaveConfig.Shape[]{config.cubic, config.simplex, config.liquid, config.floored}) {
            NoiseGen noise = new NoiseGen(SEED, shape);
            for (int start : new int[]{-32, -20, -16, -4, 0, 12, 16}) {
                double[][][][] cube = noise.cube(start, -16, 1, 8);
                for (int y = 1; y <= 8; y++) for (int g = 0; g < shape.generators; g++) {
                    assertEquals(noise.sample(g, start, y, -16), cube[0][0][y - 1][g], 0);
                    assertEquals(noise.sample(g, start + 3, y, -13), cube[3][3][y - 1][g], 0);
                }
            }
        }
    }
    @Test public void liquidRegionExtremesAndBufferAreStable() {
        CaveConfig config = new CaveConfig();
        CaveController normal = new CaveController(SEED, config);
        CaveController recreated = new CaveController(SEED, new CaveConfig());
        boolean[] seen = new boolean[3];
        for (int x = -2048; x <= 2048; x += 7) {
            int liquid = normal.liquidAt(x, -x);
            assertEquals(liquid, recreated.liquidAt(x, -x)); seen[liquid + 1] = true;
        }
        assertTrue(seen[0] && seen[1] && seen[2]);
        config.waterChance = 0; assertEquals(0, new CaveController(SEED, config).liquidAt(-16, 15));
        config.waterChance = 100; assertEquals(1, new CaveController(SEED, config).liquidAt(-16, 15));
    }
    @Test public void dimensionExclusionsOverrideGlobalEnablement() {
        CaveConfig config = new CaveConfig();
        assertTrue(config.enabledIn(0)); assertFalse(config.enabledIn(-1)); assertFalse(config.enabledIn(1));
        assertFalse(config.enabledIn(-42));
        config.dimensions = new int[]{0, -42}; assertTrue(config.enabledIn(-42));
        config.globalDimensions = true; assertTrue(config.enabledIn(42)); assertFalse(config.enabledIn(-1));
        config.enabled = false; assertFalse(config.enabledIn(0)); assertFalse(config.enabledIn(42));
    }
}
