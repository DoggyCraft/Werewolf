package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww sethome: the clan alpha sets the clan home */
public class SetHomeCommand extends SubCommand
{
	public SetHomeCommand(WerewolfContext ctx)
	{
		super(ctx, "sethome", "werewolf.sethome", true, "/ww sethome", LANGUAGESTRING.HelpSetHome);
	}

	@Override
	public boolean isAvailable(Player player)
	{
		return this.plugin.getSettings().clans().enabled() && player != null && ctx.getClanManager().isAlpha(player.getUniqueId());
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!this.plugin.getSettings().clans().enabled())
		{
			send(player, ChatColor.RED + "Clans are not enabled.");
			return;
		}
		if (!ctx.getWerewolfManager().isWerewolf(player))
		{
			send(player, LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED);
			return;
		}

		Clan clan = ctx.getWerewolfManager().getWerewolfClan(player.getUniqueId());

		if (!ctx.getClanManager().isAlpha(clan, player.getUniqueId()))
		{
			send(player, LANGUAGESTRING.YouAreNotTheAlphaOfTheClan, ChatColor.RED);
			return;
		}

		ctx.getClanManager().setHomeForClan(clan, player.getLocation());

		send(player, LANGUAGESTRING.ClanSetHome, ChatColor.GREEN);
	}
}
