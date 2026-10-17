package com.glowingfederal.bettercaves;

import com.glowingfederal.bettercaves.noise.FastNoise;

/** Better Caves settings only. Native Forge adapters load and then retain one snapshot per world. */
public final class CaveConfig {
    public boolean enabled = true, globalDimensions, flooded = true, ravines = true, floodedRavines = true;
    public boolean flattenBedrock = true, replaceGravel = true, overrideSurface;
    public int[] dimensions = {0}, excludedDimensions = {-1, 1};
    public int bedrockWidth = 1, liquidAltitude = 10;
    public String lavaBlock = "minecraft:lava", waterBlock = "minecraft:water";
    public float caveChance = 100, cavernChance = 25, waterChance = 40;
    public float caveRegionFrequency = .008f, cavernRegionFrequency = .01f, waterRegionFrequency = .004f;
    public boolean surfaceCaves = true;
    public int surfaceBottom = 40, surfaceTop = 128, surfaceDensity = 17;
    public int vanillaBottom = 8, vanillaTop = 128, vanillaDensity = 14, vanillaPriority;
    public final Shape cubic = new Shape(80, 10, 5, 1.6f, .95f, .03f, "CubicFractal");
    public final Shape simplex = new Shape(80, 5, 2.2f, .9f, .82f, .025f, "SimplexFractal");
    public final Shape liquid = new Shape(35, 10, 1.3f, .7f, .6f, .03f, "SimplexFractal");
    public final Shape floored = new Shape(35, 10, 1.3f, .7f, .6f, .028f, "SimplexFractal");

    public CaveConfig() { simplex.openSimplex = true; simplex.adjustOne = .95f; simplex.adjustTwo = .5f; }

    public boolean enabledIn(int dimension) {
        if (!enabled) return false;
        for (int excluded : excludedDimensions) if (excluded == dimension) return false;
        if (globalDimensions) return true;
        for (int allowed : dimensions) if (allowed == dimension) return true;
        return false;
    }

    public static final class Shape {
        public int bottom = 1, top, priority, cutoff = 15, octaves = 1, generators = 2;
        public float vertical, horizontal, threshold, frequency, gain = .3f, adjustOne = .9f, adjustTwo = .9f;
        public boolean openSimplex, adjust = true;
        public FastNoise.NoiseType noiseType;
        public FastNoise.FractalType fractal = FastNoise.FractalType.RigidMulti;
        public FastNoise.CellularDistanceFunction cellularDistance = FastNoise.CellularDistanceFunction.Euclidean;
        public FastNoise.CellularReturnType cellularReturn = FastNoise.CellularReturnType.CellValue;

        Shape(int top, int priority, float vertical, float horizontal, float threshold, float frequency, String type) {
            this.top = top; this.priority = priority; this.vertical = vertical; this.horizontal = horizontal;
            this.threshold = threshold; this.frequency = frequency; this.noiseType = FastNoise.NoiseType.valueOf(type);
        }
    }
}
