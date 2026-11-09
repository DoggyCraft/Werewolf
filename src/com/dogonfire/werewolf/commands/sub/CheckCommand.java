package com.dogonfire.werewolf.commands.sub;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww check <player>: whether an online player is a werewolf */
public class CheckCommand extends SubCommand
{
	public CheckCommand(Werewolf plugin)
	{
		super(plugin, "check", "werewolf.check", false, "/ww check <playername>", LANGUAGESTRING.HelpCheck);
	}

	@Override
	public boolean acceptsArgs(int length)
	{
		return length == 2;
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		Player checkPlayer = this.plugin.getServer().getPlayer(args[1]);
		if (checkPlayer == null)
		{
			return;
		}

		if (Werewolf.getWerewolfManager().isFullWerewolf(checkPlayer.getUniqueId()))
		{
			if (player != null)
			{
				int level = Werewolf.getWerewolfManager().getNumberOfTransformations(checkPlayer.getUniqueId());
				lang().setPlayerName(checkPlayer.getName());
				lang().setType(String.valueOf(level));
				send(player, LANGUAGESTRING.PlayerIsAFullWerewolf, ChatColor.AQUA);
			}
			else
			{
				this.plugin.log(checkPlayer.getName() + " is a full werewolf");
			}
		}
		else if (Werewolf.getWerewolfManager().isInfectedWerewolf(checkPlayer.getUniqueId()))
		{
			if (player != null)
			{
				lang().setPlayerName(checkPlayer.getName());
				send(player, LANGUAGESTRING.PlayerIsAnInfectedWerewolf, ChatColor.AQUA);
			}
			else
			{
				this.plugin.log(checkPlayer.getName() + " is a infected werewolf");
			}
		}
		else if (player != null)
		{
			lang().setPlayerName(checkPlayer.getName());
			send(player, LANGUAGESTRING.PlayerIsNotAWerewolf, ChatColor.AQUA);
		}
		else
		{
			this.plugin.log(checkPlayer.getName() + " is not a werewolf");
		}

		if (player != null)
		{
			this.plugin.log(player.getName() + ": /werewolf check " + checkPlayer.getName());
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, Player player, String[] args)
	{
		return args.length == 2 ? null : super.tabComplete(sender, player, args);
	}
}
