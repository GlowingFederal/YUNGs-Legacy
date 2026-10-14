package com.glowingfederal.yungslegacy;

import com.glowingfederal.yungslegacy.domain.BootstrapInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

final class BootstrapCommand extends CommandBase {
    @Override public String getName() { return "yungslegacy"; }
    @Override public String getUsage(ICommandSender sender) { return "/yungslegacy"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public void execute(net.minecraft.server.MinecraftServer server, ICommandSender sender, String[] args) {
        String text = BootstrapInfo.status();
        sender.sendMessage(new net.minecraft.util.text.TextComponentString(text));
    }
}
