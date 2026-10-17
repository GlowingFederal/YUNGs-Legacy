package com.glowingfederal.bettercaves.noise;

import com.glowingfederal.bettercaves.CaveConfig.Shape;

/** Reference seed offsets, ridged fractals and four-column interpolation, with primitive coordinates. */
public final class NoiseGen {
    private final Shape shape;
    private final FastNoise[] fast;
    private final OpenSimplex2S[] simplex;

    public NoiseGen(long seed, Shape shape) {
        this.shape = shape;
        fast = new FastNoise[shape.generators];
        simplex = new OpenSimplex2S[shape.generators];
        for (int i = 0; i < fast.length; i++) {
            if (shape.openSimplex) simplex[i] = new OpenSimplex2S(seed + 1111L * (i + 1));
            else {
                FastNoise n = new FastNoise((int) seed + 1111 * (i + 1));
                n.SetNoiseType(shape.noiseType); n.SetFractalType(shape.fractal);
                n.SetFractalOctaves(shape.octaves); n.SetFractalGain(shape.gain); n.SetFrequency(shape.frequency);
                n.SetCellularDistanceFunction(shape.cellularDistance); n.SetCellularReturnType(shape.cellularReturn);
                if (shape.cellularReturn == FastNoise.CellularReturnType.NoiseLookup) n.SetCellularNoiseLookup(new FastNoise((int) seed + 777));
                fast[i] = n;
            }
        }
    }

    public double sample(int generator, int x, int y, int z) {
        float sx = x * shape.horizontal, sy = y * shape.vertical, sz = z * shape.horizontal;
        if (!shape.openSimplex) return fast[generator].GetNoise(sx, sy, sz);
        // The reference OpenSimplex wrapper uses unnormalised ridged composition.
        sx *= shape.frequency; sy *= shape.frequency; sz *= shape.frequency;
        float amplitude = 1;
        float value = 1 - (float) Math.abs(simplex[generator].noise3_XZBeforeY(sx, sy, sz));
        for (int octave = 1; octave < shape.octaves; octave++) {
            sx *= 2; sy *= 2; sz *= 2; amplitude *= shape.gain;
            value -= (1 - (float) Math.abs(simplex[generator].noise3_XZBeforeY(sx, sy, sz))) * amplitude;
        }
        return value;
    }

    public double[][][][] cube(int x, int z, int bottom, int top) {
        double[][][][] cube = new double[4][4][top - bottom + 1][shape.generators];
        for (int y = bottom; y <= top; y++) for (int i = 0; i < shape.generators; i++) {
            double a = sample(i, x, y, z), b = sample(i, x + 3, y, z);
            double c = sample(i, x, y, z + 3), d = sample(i, x + 3, y, z + 3);
            for (int dx = 0; dx < 4; dx++) for (int dz = 0; dz < 4; dz++) {
                float fx = dx / 3f, fz = dz / 3f;
                cube[dx][dz][y - bottom][i] = (a * (1 - fx) + b * fx) * (1 - fz) + (c * (1 - fx) + d * fx) * fz;
            }
        }
        return cube;
    }
}
