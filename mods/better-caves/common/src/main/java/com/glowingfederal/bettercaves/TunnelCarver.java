package com.glowingfederal.bettercaves;

import java.util.Random;

/** Reference Better Caves surface/vanilla tunnel geometry, isolated from native terrain types. LGPL-3.0. */
final class TunnelCarver {
    private final long seed;
    private final CaveConfig config;
    private final boolean surface;
    private static final float[] SIN = new float[65536];
    static { for (int i = 0; i < SIN.length; i++) SIN[i] = (float) Math.sin(i * Math.PI * 2 / 65536); }

    TunnelCarver(long seed, CaveConfig config, boolean surface) { this.seed = seed; this.config = config; this.surface = surface; }
    private static float sin(float n) { return SIN[(int) (n * 10430.378f) & 65535]; }
    private static float cos(float n) { return SIN[(int) (n * 10430.378f + 16384f) & 65535]; }

    void carve(CaveTerrain t, CaveController controller, int cx, int cz) {
        int bottom = surface ? config.surfaceBottom : config.vanillaBottom;
        int top = surface ? config.surfaceTop : config.vanillaTop;
        int density = surface ? config.surfaceDensity : config.vanillaDensity;
        if (top <= bottom || density == 0) return;
        Random random = new Random(seed);
        long a = random.nextLong(), b = random.nextLong();
        for (int sx = cx - 8; sx <= cx + 8; sx++) for (int sz = cz - 8; sz <= cz + 8; sz++) {
            random.setSeed(sx * a ^ sz * b ^ seed);
            int attempts = random.nextInt(random.nextInt(random.nextInt(15) + 1) + 1);
            if (random.nextInt(100) > density) attempts = 0;
            for (int i = 0; i < attempts; i++) {
                double x = sx * 16 + random.nextInt(16), y = random.nextInt(top - bottom) + bottom, z = sz * 16 + random.nextInt(16);
                int calls = 1;
                if (!surface && random.nextInt(4) == 0) {
                    tunnel(t, controller, cx, cz, random.nextLong(), x, y, z, 1 + random.nextFloat() * 6, 0, 0, -1, -1, .5);
                    calls += random.nextInt(4);
                }
                for (int j = 0; j < calls; j++) {
                    float yaw = random.nextFloat() * ((float) Math.PI * 2), pitch = (random.nextFloat() - .5f) * 2 / 8;
                    float width = random.nextFloat() * 2 + random.nextFloat();
                    if (!surface && random.nextInt(10) == 0) width *= random.nextFloat() * random.nextFloat() * 3 + 1;
                    tunnel(t, controller, cx, cz, random.nextLong(), x, y, z, width, yaw, pitch, 0, 0, 1);
                }
            }
        }
    }

    private void tunnel(CaveTerrain t, CaveController c, int cx, int cz, long tunnelSeed, double x, double y, double z,
                        float width, float yaw, float pitch, int step, int end, double height) {
        Random r = new Random(tunnelSeed);
        double centerX = cx * 16 + 8, centerZ = cz * 16 + 8;
        float yawDelta = 0, pitchDelta = 0;
        if (end <= 0) end = 112 - r.nextInt(28);
        boolean room = step == -1;
        if (room) step = end / 2;
        int branch = r.nextInt(end / 2) + end / 4;
        for (; step < end; step++) {
            double radius = 1.5 + sin(step * (float) Math.PI / end) * width, verticalRadius = radius * height;
            x += cos(yaw) * cos(pitch); y += sin(pitch); z += sin(yaw) * cos(pitch);
            pitch *= r.nextInt(6) == 0 ? .92f : .7f;
            pitch += pitchDelta * .1f; yaw += yawDelta * .1f;
            pitchDelta *= .9f; yawDelta *= .75f;
            pitchDelta += (r.nextFloat() - r.nextFloat()) * r.nextFloat() * 2;
            yawDelta += (r.nextFloat() - r.nextFloat()) * r.nextFloat() * 4;
            if (!room && step == branch && width > 1) {
                tunnel(t, c, cx, cz, r.nextLong(), x, y, z, r.nextFloat() * .5f + .5f, yaw - (float) Math.PI / 2, pitch / 3, step, end, 1);
                tunnel(t, c, cx, cz, r.nextLong(), x, y, z, r.nextFloat() * .5f + .5f, yaw + (float) Math.PI / 2, pitch / 3, step, end, 1);
                return;
            }
            if (!room && r.nextInt(4) == 0) continue;
            double dx = x - centerX, dz = z - centerZ, remaining = end - step, reach = width + 18;
            if (dx * dx + dz * dz - remaining * remaining > reach * reach) return;
            if (x < centerX - 16 - radius * 2 || x > centerX + 16 + radius * 2 || z < centerZ - 16 - radius * 2 || z > centerZ + 16 + radius * 2) continue;
            int minX = Math.max(0, (int) Math.floor(x - radius) - cx * 16 - 1), maxX = Math.min(16, (int) Math.floor(x + radius) - cx * 16 + 1);
            int minZ = Math.max(0, (int) Math.floor(z - radius) - cz * 16 - 1), maxZ = Math.min(16, (int) Math.floor(z + radius) - cz * 16 + 1);
            int minY = Math.max(1, (int) Math.floor(y - verticalRadius) - 1), maxY = Math.min(248, (int) Math.floor(y + verticalRadius) + 1);
            for (int lx = minX; lx < maxX; lx++) for (int lz = minZ; lz < maxZ; lz++) {
                if (!surface && c.caveTypeAt(cx * 16 + lx, cz * 16 + lz) != 2) continue;
                boolean flooded = config.flooded && t.ocean(lx, lz);
                if (flooded && t.oceanDistance(lx, lz) <= .25f) continue;
                double nx = (cx * 16 + lx + .5 - x) / radius, nz = (cz * 16 + lz + .5 - z) / radius;
                if (nx * nx + nz * nz >= 1) continue;
                int liquid = c.liquidAt(cx * 16 + lx, cz * 16 + lz);
                for (int ly = maxY; ly > minY; ly--) {
                    double ny = (ly - .5 - y) / verticalRadius;
                    if (ny > -.7 && nx * nx + ny * ny + nz * nz < 1 && (ly > config.liquidAltitude || liquid != -1)) t.carve(lx, ly, lz, liquid, flooded);
                }
            }
            if (room) break;
        }
    }
}
