package com.glowingfederal.bettercaves;

/** Chunk-local terrain boundary; no block/state emulation or world reference in the computation core. */
public interface CaveTerrain {
    int surface(int x, int z);
    boolean ocean(int x, int z);
    float oceanDistance(int x, int z);
    void flattenBedrock();
    void carve(int x, int y, int z, int liquid, boolean flooded);
}
