package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.ClanManager.ClanType;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww sethome: the clan alpha sets the clan home */
public class SetHomeCommand extends SubCommand
{
	public SetHomeCommand(Werewolf plugin)
	{
		super(plugin, "sethome", "werewolf.sethome", true, "/ww sethome", LANGUAGESTRING.HelpSetHome);
	}

	@Override
	public boolean isAvailable(Player player)
	{
		return this.plugin.getSettings().clans().enabled() && player != null && Werewolf.getClanManager().isAlpha(player.getUniqueId());
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!this.plugin.getSettings().clans().enabled())
		{
			send(player, ChatColor.RED + "Clans are not enabled.");
			return;
		}
		if (!Werewolf.getWerewolfManager().isWerewolf(player))
		{
			send(player, LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED);
			return;
		}

		ClanType clan = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());

		if (!Werewolf.getClanManager().isAlpha(clan, player.getUniqueId()))
		{
			send(player, LANGUAGESTRING.YouAreNotTheAlphaOfTheClan, ChatColor.RED);
			return;
		}

		Werewolf.getClanManager().setHomeForClan(clan, player.getLocation());

		send(player, LANGUAGESTRING.ClanSetHome, ChatColor.GREEN);
	}
}
