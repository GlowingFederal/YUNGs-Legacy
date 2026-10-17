package com.glowingfederal.yungslegacy;

import com.glowingfederal.yungslegacy.domain.BootstrapInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

final class BootstrapCommand extends CommandBase {
    // Its raw Comparable signature needs an explicit Object comparator in adapters.
    @Override public int compareTo(Object other) {
        return super.compareTo((net.minecraft.command.ICommand) other);
    }
    @Override public String getCommandName() { return "yungslegacy"; }
    @Override public String getCommandUsage(ICommandSender sender) { return "/yungslegacy"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public void processCommand(ICommandSender sender, String[] args) {
        String text = BootstrapInfo.status();
        sender.sendChatToPlayer(net.minecraft.util.ChatMessageComponent.createFromText(text));
    }
}
