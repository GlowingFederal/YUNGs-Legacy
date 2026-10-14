package com.glowingfederal.yungslegacy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

/** Internal repository bootstrap; not a port of YUNG's API or gameplay mods. */
@Mod(modid = LegacyBootstrap.MODID, name = "YUNG's Legacy Bootstrap", version = BuildVersion.VERSION)
public final class LegacyBootstrap {
    public static final String MODID = "yungslegacybootstrap";

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new BootstrapCommand());
    }
}
