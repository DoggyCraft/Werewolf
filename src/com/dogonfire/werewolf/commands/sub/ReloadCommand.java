package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.SubCommand;

/** /ww reload: reloads config.yml */
public class ReloadCommand extends SubCommand
{
	public ReloadCommand(Werewolf plugin)
	{
		super(plugin, "reload", "werewolf.reload", false, null, null);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		this.plugin.reloadSettings();
		if (player == null)
		{
			this.plugin.log(this.plugin.getDescription().getFullName() + ": Reloaded configuration.");
		}
		else
		{
			player.sendMessage(ChatColor.YELLOW + this.plugin.getDescription().getFullName() + ": " + ChatColor.WHITE + "Reloaded configuration.");
		}
	}
}
