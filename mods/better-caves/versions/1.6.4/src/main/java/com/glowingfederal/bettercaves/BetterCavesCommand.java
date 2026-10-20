package com.glowingfederal.bettercaves;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

final class BetterCavesCommand extends CommandBase {
    // This target exposes raw Comparable; supply the bridge explicitly.
    @Override public int compareTo(Object other) { return super.compareTo((net.minecraft.command.ICommand) other); }
    @Override public String getCommandName() { return "bettercaves"; }
    @Override public String getCommandUsage(ICommandSender sender) { return "/bettercaves"; }
    @Override public java.util.List<String> getCommandAliases() { return java.util.Collections.singletonList("yungslegacy"); }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public void processCommand(ICommandSender sender, String[] args) {
        String text = "YUNG's Better Caves: Better Caves terrain generation installed; config/bettercaves.cfg controls dimensions and carving.";
        sender.sendChatToPlayer(net.minecraft.util.ChatMessageComponent.createFromText(text));
    }
}
