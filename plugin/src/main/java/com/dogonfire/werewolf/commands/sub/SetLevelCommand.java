package com.dogonfire.werewolf.commands.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.api.event.WerewolfLevelChangeEvent;
import com.dogonfire.werewolf.commands.CommandUtil;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww setlevel <player> <level> and /ww addlevel <player> <amount> */
public class SetLevelCommand extends SubCommand
{
	private static final List<String> AMOUNTS = List.of("1", "5", "10", "20", "50");

	private final boolean add;

	public SetLevelCommand(WerewolfContext ctx, boolean add)
	{
		super(ctx,
				add ? "addlevel" : "setlevel",
				add ? "werewolf.addlevel" : "werewolf.setlevel",
				false,
				add ? "/ww addlevel <playername> <amount>" : "/ww setlevel <playername> <level>",
				add ? LANGUAGESTRING.HelpAddLevel : LANGUAGESTRING.HelpSetLevel);
		this.add = add;
	}

	@Override
	public boolean acceptsArgs(int length)
	{
		return true;
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (args.length < 3)
		{
			if (this.add)
			{
				CommandUtil.sendUsage(ctx, player, "/ww addlevel <player> <amount>", LANGUAGESTRING.HelpAddLevel);
			}
			else
			{
				CommandUtil.sendUsage(ctx, player, "/ww setlevel <player> <level>", LANGUAGESTRING.HelpSetLevel);
			}
			return;
		}

		Integer amount = null;
		try
		{
			amount = Integer.valueOf(args[2]);
		}
		catch (NumberFormatException ignored)
		{
		}
		// Levels can't go below 0; addlevel may take some away
		if (amount == null || (!this.add && amount < 0))
		{
			lang().setType(args[2]);
			send(player, LANGUAGESTRING.AdminInvalidNumber, ChatColor.RED);
			return;
		}

		UUID playerId = CommandUtil.findWerewolf(ctx, player, args[1]);
		if (playerId == null)
		{
			return;
		}

		int level = this.add ? ctx.getWerewolfManager().getNumberOfTransformations(playerId) + amount : amount;
		boolean changed = ctx.getWerewolfManager().setLevel(playerId, level, WerewolfLevelChangeEvent.Cause.COMMAND);

		String name = ctx.getWerewolfManager().getPlayerName(playerId);
		lang().setPlayerName(name);
		if (!changed)
		{
			send(player, LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED);
			return;
		}

		lang().setAmount("" + ctx.getWerewolfManager().getNumberOfTransformations(playerId));
		send(player, LANGUAGESTRING.AdminLevelSet, ChatColor.GREEN);

		this.plugin.log(CommandUtil.senderName(player) + ": /werewolf " + args[0] + " " + name + " " + args[2]);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, Player player, String[] args)
	{
		List<String> result = new ArrayList<String>();
		if (args.length == 2)
		{
			StringUtil.copyPartialMatches(args[1], CommandUtil.werewolfNames(ctx), result);
		}
		else if (args.length == 3)
		{
			StringUtil.copyPartialMatches(args[2], AMOUNTS, result);
		}
		return result;
	}
}
