package com.glowingfederal.bettercaves;

import com.glowingfederal.bettercaves.noise.FastNoise;
import com.glowingfederal.bettercaves.noise.NoiseGen;
import com.glowingfederal.bettercaves.noise.NoiseUtils;

/** Better Caves region and column algorithms adapted from the retained 1.16 source (LGPL-3.0). */
public final class CaveController {
    public final long seed;
    private final CaveConfig config;
    private final FastNoise caves, caverns, liquids;
    private final NoiseGen[] noise = new NoiseGen[4];
    private final CaveConfig.Shape[] shapes;
    private final Range[] cavernRanges;
    private final float waterThreshold;
    private final float cubicEnd, simplexEnd, caveEnd;

    public CaveController(long seed, CaveConfig config) {
        this.seed = seed; this.config = config;
        caves = sampler((int) seed + 222, config.caveRegionFrequency);
        caves.SetNoiseType(FastNoise.NoiseType.Cellular);
        caves.SetCellularDistanceFunction(FastNoise.CellularDistanceFunction.Natural);
        caverns = sampler((int) seed + 333, config.cavernRegionFrequency);
        liquids = sampler((int) seed + 444, config.waterRegionFrequency);
        waterThreshold = offset(-1, config.waterChance / 100);
        shapes = new CaveConfig.Shape[]{config.cubic, config.simplex, config.liquid, config.floored};
        for (int i = 0; i < noise.length; i++) noise[i] = new NoiseGen(seed, shapes[i]);
        int cavePriority = config.cubic.priority + config.simplex.priority + config.vanillaPriority;
        float caveLength = cavePriority == 0 ? 0 : config.caveChance / (50f * cavePriority);
        cubicEnd = -1 + config.cubic.priority * caveLength;
        simplexEnd = cubicEnd + config.simplex.priority * caveLength;
        caveEnd = simplexEnd + config.vanillaPriority * caveLength;
        int total = config.liquid.priority + config.floored.priority;
        cavernRanges = new Range[2];
        float start = -1;
        for (int i = 0; i < 2; i++) if (shapes[i + 2].priority > 0 && total > 0) {
            float end = offset(start, shapes[i + 2].priority * config.cavernChance / (100f * total));
            cavernRanges[i] = new Range(start, end);
            start = offset(end, 1 - config.cavernChance / 100);
        }
    }

    private static FastNoise sampler(int seed, float frequency) {
        FastNoise n = new FastNoise(seed); n.SetFrequency(frequency); return n;
    }

    private static float offset(float base, float percent) {
        if (percent <= 0) return base;
        if (percent >= 1) return 1;
        return Math.min(1, NoiseUtils.simplexNoiseOffsetByPercent(base, percent));
    }

    /** -1 is an uncarved buffer separating water and lava; 0 lava, 1 water. */
    public int liquidAt(int x, int z) {
        if (config.waterChance <= 0) return 0;
        if (config.waterChance >= 100) return 1;
        float n = liquids.GetNoise(x, z);
        float border = .04f + (mix(seed ^ ((long) x * 341873128712L) ^ ((long) z * 132897987541L)) >>> 40) * (1f / 16777216f) * .01f;
        return n < waterThreshold - border ? 1 : n < waterThreshold + border ? -1 : 0;
    }

    public static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    public int caveTypeAt(int x, int z) {
        float value = caves.GetNoise(x, z);
        return value < cubicEnd ? 0 : value < simplexEnd ? 1 : value < caveEnd ? 2 : -1;
    }

    public void carve(CaveTerrain terrain, int chunkX, int chunkZ) {
        if (config.flattenBedrock) terrain.flattenBedrock();
        // Global coordinates and a fixed four-block lattice make this independent of neighbour availability.
        for (int sx = 0; sx < 16; sx += 4) for (int sz = 0; sz < 16; sz += 4) {
            double[][][][][] cubes = new double[4][][][][];
            for (int x = sx; x < sx + 4; x++) for (int z = sz; z < sz + 4; z++) {
                int gx = chunkX * 16 + x, gz = chunkZ * 16 + z;
                boolean flooded = config.flooded && terrain.ocean(x, z);
                float coast = config.flooded ? terrain.oceanDistance(x, z) : 1;
                int liquid = liquidAt(gx, gz), cave = caveTypeAt(gx, gz);
                for (int i = 0; i < 4; i++) {
                    float smooth = 1;
                    if (i < 2) { if (cave != i || (flooded && coast <= .25f)) continue; }
                    else {
                        Range range = cavernRanges[i - 2];
                        float value = caverns.GetNoise(gx, gz);
                        if (range == null || !range.contains(value) || coast <= .25f) continue;
                        smooth = range.smooth(value) * coast;
                    }
                    CaveConfig.Shape shape = shapes[i];
                    int top = config.overrideSurface ? shape.top : Math.min(shape.top, terrain.surface(x, z));
                    if (top < shape.bottom) continue;
                    if (cubes[i] == null) cubes[i] = noise[i].cube(chunkX * 16 + sx, chunkZ * 16 + sz, shape.bottom, shape.top);
                    double[][] column = cubes[i][x - sx][z - sz];
                    if (i < 2) carveCave(terrain, x, z, top, column, shape, liquid, flooded);
                    else carveCavern(terrain, x, z, top, column, shape, i == 3, smooth, liquid, flooded);
                }
            }
        }
        if (config.vanillaPriority > 0) new TunnelCarver(seed, config, false).carve(terrain, this, chunkX, chunkZ);
        if (config.surfaceCaves) new TunnelCarver(seed, config, true).carve(terrain, this, chunkX, chunkZ);
    }

    private void carveCave(CaveTerrain t, int x, int z, int top, double[][] column, CaveConfig.Shape s, int liquid, boolean flooded) {
        float[] thresholds = new float[top - s.bottom + 1];
        int boundary = Math.max(1, top - s.cutoff);
        for (int y = s.bottom; y <= top; y++) thresholds[y - s.bottom] = s.threshold *
            (y >= boundary && top > boundary ? 1 + .3f * (y - boundary) / (top - boundary) : 1);
        if (s.adjust) for (int y = top; y >= s.bottom; y--) {
            int index = y - s.bottom;
            if (!above(column[index], thresholds[index])) continue;
            for (int dy = 1; dy <= 2 && y + dy <= top; dy++) {
                float factor = dy == 1 ? s.adjustOne : s.adjustTwo;
                for (int i = 0; i < s.generators; i++) column[index + dy][i] = (1 - factor) * column[index + dy][i] + factor * column[index][i];
            }
        }
        for (int y = top; y >= s.bottom; y--) {
            if (y <= config.liquidAltitude && liquid == -1) break;
            if (above(column[y - s.bottom], thresholds[y - s.bottom])) t.carve(x, y, z, liquid, flooded);
        }
    }

    private static boolean above(double[] values, float threshold) {
        for (double n : values) if (n < threshold) return false;
        return true;
    }

    private void carveCavern(CaveTerrain t, int x, int z, int top, double[][] column, CaveConfig.Shape s, boolean floored, float smooth, int liquid, boolean flooded) {
        top -= 2;
        int upper = Math.max(1, top - 6);
        int lower = floored ? (s.bottom < config.liquidAltitude ? config.liquidAltitude + 8 : s.bottom + 7) : s.bottom + 3;
        for (int y = top; y >= s.bottom; y--) {
            if (y <= config.liquidAltitude && liquid == -1) break;
            double n = 1;
            for (double value : column[y - s.bottom]) n *= value;
            float threshold = s.threshold * smooth;
            if (y >= upper && top > upper) threshold *= (float) (top - y) / (top - upper);
            if (y < lower) threshold *= (float) (y - s.bottom) / (lower - s.bottom);
            if (n < threshold) t.carve(x, y, z, liquid, flooded);
        }
    }

    private static final class Range {
        final float bottom, top, smoothBottom, smoothTop;
        Range(float bottom, float top) {
            this.bottom = bottom; this.top = top;
            float length = ((top >= 1 ? 1 : NoiseUtils.noiseToCDF(top)) - (bottom <= -1 ? 0 : NoiseUtils.noiseToCDF(bottom))) * .3f;
            smoothBottom = offset(bottom, length);
            smoothTop = NoiseUtils.simplexNoiseNegativeOffsetByPercent(top, length);
        }
        boolean contains(float n) { return bottom <= n && n < top; }
        float smooth(float n) {
            if (n <= smoothBottom && smoothBottom > bottom) return (n - bottom) / (smoothBottom - bottom);
            if (n >= smoothTop && top > smoothTop) return (top - n) / (top - smoothTop);
            return 1;
        }
    }
}
