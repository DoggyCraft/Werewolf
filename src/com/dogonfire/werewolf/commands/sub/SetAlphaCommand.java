package com.dogonfire.werewolf.commands.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.CommandUtil;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.ClanManager.ClanType;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww setalpha <player>: makes a werewolf the alpha of their own clan */
public class SetAlphaCommand extends SubCommand
{
	public SetAlphaCommand(Werewolf plugin)
	{
		super(plugin, "setalpha", "werewolf.setalpha", false, "/ww setalpha <playername>", LANGUAGESTRING.HelpSetAlpha);
	}

	@Override
	public boolean acceptsArgs(int length)
	{
		return true;
	}

	@Override
	public boolean isAvailable(Player player)
	{
		return this.plugin.getSettings().clans().enabled();
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!this.plugin.getSettings().clans().enabled())
		{
			send(player, ChatColor.RED + "Clans are not enabled.");
			return;
		}

		if (args.length < 2)
		{
			CommandUtil.sendUsage(this.plugin, player, "/ww setalpha <player>", LANGUAGESTRING.HelpSetAlpha);
			return;
		}

		UUID playerId = CommandUtil.findWerewolf(this.plugin, player, args[1]);
		if (playerId == null)
		{
			return;
		}

		String name = Werewolf.getWerewolfManager().getPlayerName(playerId);
		ClanType clan = Werewolf.getWerewolfManager().getWerewolfClan(playerId);
		lang().setPlayerName(name);

		if (!Werewolf.getClanManager().assignAlphaInClan(clan, playerId))
		{
			send(player, LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED);
			return;
		}

		Werewolf.getWerewolfManager().sendMessageToClan(clan, text(LANGUAGESTRING.NewClanAlpha, ChatColor.WHITE));

		lang().setPlayerName(name);
		lang().setType(Werewolf.getClanManager().getClanName(clan));
		send(player, LANGUAGESTRING.AdminAlphaSet, ChatColor.GREEN);

		this.plugin.log(CommandUtil.senderName(player) + ": /werewolf setalpha " + name);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, Player player, String[] args)
	{
		List<String> result = new ArrayList<String>();
		if (args.length == 2)
		{
			StringUtil.copyPartialMatches(args[1], CommandUtil.werewolfNames(this.plugin), result);
		}
		return result;
	}
}
