package com.glowingfederal.bettercaves;

import java.io.File;
import net.minecraftforge.common.Configuration;
import com.glowingfederal.bettercaves.noise.FastNoise;

/** Native Forge .cfg loading. Dimension overrides inherit the effective global settings. */
final class CaveConfigLoader {
    private CaveConfigLoader() {}
    static CaveConfig load(File directory, int dimension) {
        CaveConfig settings = new CaveConfig();
        read(new File(directory, "bettercaves.cfg"), settings);
        File override = new File(new File(directory, "bettercaves"), "dimension-" + dimension + ".cfg");
        if (override.isFile()) read(override, settings);
        return settings;
    }
    private static void read(File file, CaveConfig c) {
        Configuration f = new Configuration(file);
        f.load();
        c.enabled = bool(f, "general", "enabled", c.enabled);
        c.globalDimensions = bool(f, "general", "globalDimensions", c.globalDimensions);
        c.dimensions = f.get("general", "dimensions", c.dimensions, "Numeric dimension whitelist. Default overworld only.").getIntList();
        c.excludedDimensions = f.get("general", "excludedDimensions", c.excludedDimensions, "Exclusions take precedence over the whitelist and globalDimensions.").getIntList();
        c.flooded = bool(f, "general", "floodedUnderground", c.flooded);
        c.ravines = bool(f, "general", "ravines", c.ravines);
        c.floodedRavines = bool(f, "general", "floodedRavines", c.floodedRavines);
        c.overrideSurface = bool(f, "general", "overrideSurfaceDetection", c.overrideSurface);
        c.replaceGravel = bool(f, "general", "replaceFloatingGravel", c.replaceGravel);
        c.flattenBedrock = bool(f, "general", "flattenBedrock", c.flattenBedrock);
        c.bedrockWidth = integer(f, "general", "bedrockWidth", c.bedrockWidth, 0, 256);
        c.liquidAltitude = integer(f, "general", "liquidAltitude", c.liquidAltitude, 0, 255);
        c.lavaBlock = f.get("general", "lavaBlock", c.lavaBlock).getString();
        c.waterBlock = f.get("general", "waterBlock", c.waterBlock).getString();
        c.caveChance = number(f, "regions", "caveSpawnChance", c.caveChance, 0, 100);
        c.cavernChance = number(f, "regions", "cavernSpawnChance", c.cavernChance, 0, 100);
        c.waterChance = number(f, "regions", "waterSpawnChance", c.waterChance, 0, 100);
        c.caveRegionFrequency = number(f, "regions", "caveFrequency", c.caveRegionFrequency, 0, .05f);
        c.cavernRegionFrequency = number(f, "regions", "cavernFrequency", c.cavernRegionFrequency, 0, .05f);
        c.waterRegionFrequency = number(f, "regions", "waterFrequency", c.waterRegionFrequency, 0, .05f);
        c.surfaceCaves = bool(f, "surface", "enabled", c.surfaceCaves);
        c.surfaceBottom = integer(f, "surface", "bottom", c.surfaceBottom, 0, 255);
        c.surfaceTop = integer(f, "surface", "top", c.surfaceTop, c.surfaceBottom, 255);
        c.surfaceDensity = integer(f, "surface", "density", c.surfaceDensity, 0, 100);
        c.vanillaPriority = integer(f, "vanilla", "priority", c.vanillaPriority, 0, 10);
        c.vanillaBottom = integer(f, "vanilla", "bottom", c.vanillaBottom, 0, 255);
        c.vanillaTop = integer(f, "vanilla", "top", c.vanillaTop, c.vanillaBottom, 255);
        c.vanillaDensity = integer(f, "vanilla", "density", c.vanillaDensity, 0, 100);
        shape(f, "cubic", c.cubic); shape(f, "simplex", c.simplex);
        shape(f, "liquidCavern", c.liquid); shape(f, "flooredCavern", c.floored);
        if (f.hasChanged()) f.save();
    }
    private static void shape(Configuration f, String category, CaveConfig.Shape s) {
        s.bottom = integer(f, category, "bottom", s.bottom, 0, 255);
        s.top = integer(f, category, "top", s.top, s.bottom, 255);
        s.priority = integer(f, category, "priority", s.priority, 0, 10);
        s.cutoff = integer(f, category, "surfaceCutoff", s.cutoff, 0, 255);
        s.vertical = number(f, category, "verticalCompression", s.vertical, 0, 100);
        s.horizontal = number(f, category, "horizontalCompression", s.horizontal, 0, 100);
        s.threshold = number(f, category, "threshold", s.threshold, -1, 1);
        s.octaves = integer(f, category, "octaves", s.octaves, 1, 10);
        s.generators = integer(f, category, "generators", s.generators, 1, 8);
        s.gain = number(f, category, "gain", s.gain, 0, 1);
        s.frequency = number(f, category, "frequency", s.frequency, 0, 1);
        s.adjust = bool(f, category, "verticalAdjustment", s.adjust);
        s.adjustOne = number(f, category, "adjustOne", s.adjustOne, 0, 1);
        s.adjustTwo = number(f, category, "adjustTwo", s.adjustTwo, 0, 1);
        s.openSimplex = bool(f, category, "openSimplex", s.openSimplex);
        s.noiseType = choice(f, category, "noiseType", s.noiseType, FastNoise.NoiseType.class);
        s.fractal = choice(f, category, "fractal", s.fractal, FastNoise.FractalType.class);
        s.cellularDistance = choice(f, category, "cellularDistance", s.cellularDistance, FastNoise.CellularDistanceFunction.class);
        s.cellularReturn = choice(f, category, "cellularReturn", s.cellularReturn, FastNoise.CellularReturnType.class);
    }
    private static <T extends Enum<T>> T choice(Configuration f, String category, String key, T value, Class<T> type) {
        String name = f.get(category, key, value.name(), "Case-sensitive enum; FastNoise options apply when openSimplex=false.").getString();
        try { return Enum.valueOf(type, name); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid Better Caves " + category + "." + key + ": " + name, e); }
    }
    private static boolean bool(Configuration f, String category, String key, boolean value) {
        return f.get(category, key, value, "Takes effect after world restart.").getBoolean(value);
    }
    private static int integer(Configuration f, String category, String key, int value, int min, int max) {
        return Math.max(min, Math.min(max, f.get(category, key, value, "Takes effect after world restart.").getInt(value)));
    }
    private static float number(Configuration f, String category, String key, float value, float min, float max) {
        double n = f.get(category, key, (double) value, "Takes effect after world restart.").getDouble(value);
        if (Double.isNaN(n) || Double.isInfinite(n)) throw new IllegalArgumentException("Non-finite Better Caves " + category + "." + key);
        return (float) Math.max(min, Math.min(max, n));
    }
}
