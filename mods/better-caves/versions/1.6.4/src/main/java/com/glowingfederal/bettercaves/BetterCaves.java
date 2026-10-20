package com.glowingfederal.bettercaves;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

/** Better Caves entry point; Better Caves is wired into Forge terrain generation. */
@Mod(modid = BetterCaves.MODID, name = "YUNG's Better Caves", version = BuildVersion.VERSION)
public final class BetterCaves {
    public static final String MODID = "bettercaves";

    @Mod.EventHandler
    public void preInit(cpw.mods.fml.common.event.FMLPreInitializationEvent event) {
        com.glowingfederal.bettercaves.NativeHooks hooks = new com.glowingfederal.bettercaves.NativeHooks(event.getModConfigurationDirectory());
        net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.register(hooks);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(hooks);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new BetterCavesCommand());
    }
}
