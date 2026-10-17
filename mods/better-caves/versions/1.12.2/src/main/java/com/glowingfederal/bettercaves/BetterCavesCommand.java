package com.glowingfederal.bettercaves;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

final class BetterCavesCommand extends CommandBase {
    @Override public String getName() { return "bettercaves"; }
    @Override public String getUsage(ICommandSender sender) { return "/bettercaves"; }
    @Override public java.util.List<String> getAliases() { return java.util.Collections.singletonList("yungslegacy"); }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public void execute(net.minecraft.server.MinecraftServer server, ICommandSender sender, String[] args) {
        String text = "YUNG's Better Caves: Better Caves terrain generation installed; config/bettercaves.cfg controls dimensions and carving.";
        sender.sendMessage(new net.minecraft.util.text.TextComponentString(text));
    }
}
