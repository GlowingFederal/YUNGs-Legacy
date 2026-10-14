package com.glowingfederal.yungslegacy.domain;

/** Portable setup-phase status; no Minecraft or loader dependencies. */
public final class BootstrapInfo {
    private BootstrapInfo() {}

    public static String status() {
        return "YUNG's Legacy: multiversion bootstrap only; no mods have been ported yet.";
    }
}
