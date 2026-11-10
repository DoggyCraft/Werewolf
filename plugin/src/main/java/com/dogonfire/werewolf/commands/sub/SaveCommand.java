package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;

/** /ww save: writes pending data file changes now */
public class SaveCommand extends SubCommand
{
	public SaveCommand(WerewolfContext ctx)
	{
		super(ctx, "save", "werewolf.save", false, null, null);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		ctx.getStorage().flush();
		if (player == null)
		{
			this.plugin.log(this.plugin.getDescription().getFullName() + ": Saved configuration(s).");
		}
		else
		{
			player.sendMessage(ChatColor.YELLOW + this.plugin.getDescription().getFullName() + ": " + ChatColor.WHITE + "Saved configuration(s).");
		}
	}
}
