package com.glowingfederal.yungslegacy;

import com.glowingfederal.yungslegacy.domain.BootstrapInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

final class BootstrapCommand extends CommandBase {
    @Override public String getCommandName() { return "yungslegacy"; }
    @Override public String getCommandUsage(ICommandSender sender) { return "/yungslegacy"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public void processCommand(ICommandSender sender, String[] args) {
        String text = BootstrapInfo.status();
        sender.addChatMessage(new net.minecraft.util.ChatComponentText(text));
    }
}
