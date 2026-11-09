package com.dogonfire.werewolf.commands.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.CommandUtil;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.events.WerewolfCureEvent;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww cure <player>: cures a werewolf, online or offline. Unlike /ww toggle it never turns anyone into a werewolf */
public class CureCommand extends SubCommand
{
	public CureCommand(WerewolfContext ctx)
	{
		super(ctx, "cure", "werewolf.cureother", false, "/ww cure <playername>", LANGUAGESTRING.HelpCureOther);
	}

	@Override
	public boolean acceptsArgs(int length)
	{
		return true;
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (args.length < 2)
		{
			CommandUtil.sendUsage(ctx, player, "/ww cure <player>", LANGUAGESTRING.HelpCureOther);
			return;
		}

		UUID playerId = CommandUtil.findWerewolf(ctx, player, args[1]);
		if (playerId == null)
		{
			return;
		}

		String name = ctx.getWerewolfManager().getPlayerName(playerId);
		lang().setPlayerName(name);

		if (!ctx.getWerewolfManager().unmakeWerewolf(playerId, WerewolfCureEvent.Cause.COMMAND))
		{
			send(player, LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED);
			return;
		}

		send(player, LANGUAGESTRING.AdminCured, ChatColor.GREEN);

		Player target = this.plugin.getServer().getPlayer(playerId);
		if (target != null)
		{
			lang().setPlayerName(CommandUtil.senderName(player));
			target.sendMessage(text(LANGUAGESTRING.UnMadeWerewolf, ChatColor.AQUA));
		}

		this.plugin.log(CommandUtil.senderName(player) + ": /werewolf cure " + name);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, Player player, String[] args)
	{
		List<String> result = new ArrayList<String>();
		if (args.length == 2)
		{
			StringUtil.copyPartialMatches(args[1], CommandUtil.werewolfNames(ctx), result);
		}
		return result;
	}
}
