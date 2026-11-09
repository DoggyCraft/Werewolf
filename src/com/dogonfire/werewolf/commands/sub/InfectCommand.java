package com.dogonfire.werewolf.commands.sub;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.CommandUtil;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.api.event.WerewolfInfectionEvent;
import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww infect [player] [clan]: gives yourself or an online player the werewolf infection */
public class InfectCommand extends SubCommand
{
	private static final String SELF_PERMISSION = "werewolf.infectself";
	private static final String OTHER_PERMISSION = "werewolf.infect";

	public InfectCommand(WerewolfContext ctx)
	{
		super(ctx, "infect", SELF_PERMISSION, false, null, null);
	}

	@Override
	public boolean acceptsArgs(int length)
	{
		return length >= 1;
	}

	@Override
	public String permission(String[] args)
	{
		return args.length >= 2 ? OTHER_PERMISSION : SELF_PERMISSION;
	}

	@Override
	public boolean isPlayerOnly(String[] args)
	{
		return args.length == 1;
	}

	@Override
	public boolean isListed(Player player)
	{
		return CommandUtil.hasPermission(ctx, player, OTHER_PERMISSION) || CommandUtil.hasPermission(ctx, player, SELF_PERMISSION);
	}

	@Override
	public List<HelpEntry> help(Player player)
	{
		List<HelpEntry> entries = new ArrayList<HelpEntry>();
		if (CommandUtil.hasPermission(ctx, player, SELF_PERMISSION))
		{
			entries.add(new HelpEntry("/ww infect", LANGUAGESTRING.HelpInfectSelf));
		}
		if (CommandUtil.hasPermission(ctx, player, OTHER_PERMISSION))
		{
			entries.add(new HelpEntry("/ww infect <playername> [clan]", LANGUAGESTRING.HelpInfectOther));
		}
		return entries;
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (args.length == 1)
		{
			infectSelf(player);
		}
		else
		{
			infectOther(player, args);
		}
	}

	private void infectSelf(Player player)
	{
		if (!ctx.getWerewolfManager().isWerewolf(player))
		{
			if (ctx.getWerewolfManager().makeWerewolf(player, false, CommandUtil.randomClan(), WerewolfInfectionEvent.Source.COMMAND))
			{
				player.sendMessage(ChatColor.AQUA + "You now have the werewolf infection!");
			}
			else
			{
				player.sendMessage(ChatColor.RED + "Could not make you a werewolf!");
			}
		}
		else
		{
			player.sendMessage(ChatColor.RED + "You are already a werewolf! Please toggle your status first...");
		}
		this.plugin.log(player.getName() + ": /werewolf infect");
	}

	private void infectOther(Player player, String[] args)
	{
		Player target = this.plugin.getServer().getPlayer(args[1]);
		if (target == null)
		{
			send(player, ChatColor.RED + "Invalid player name...");
			return;
		}

		Clan clan;
		if (args.length >= 3)
		{
			clan = CommandUtil.parseClan(ctx, args[2]);
			if (clan == null)
			{
				lang().setType(String.join(", ", CommandUtil.clanNames(ctx)));
				send(player, LANGUAGESTRING.AdminInvalidClan, ChatColor.RED);
				return;
			}
		}
		else
		{
			clan = CommandUtil.randomClan();
		}

		if (ctx.getWerewolfManager().isWerewolf(target))
		{
			send(player, ChatColor.RED + target.getName() + " already is a werewolf! Please cure them first...");
			return;
		}

		if (!ctx.getWerewolfManager().makeWerewolf(target, false, clan, WerewolfInfectionEvent.Source.COMMAND))
		{
			lang().setPlayerName(target.getName());
			send(player, LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED);
			return;
		}

		send(player, ChatColor.GREEN + target.getName() + " now has the werewolf infection!");
		lang().setPlayerName(CommandUtil.senderName(player));
		target.sendMessage(text(LANGUAGESTRING.Infected, ChatColor.AQUA));

		this.plugin.log(CommandUtil.senderName(player) + ": /werewolf infect " + target.getName() + " " + clan.name());
	}

	@Override
	public List<String> tabComplete(CommandSender sender, Player player, String[] args)
	{
		if (args.length == 2)
		{
			return null;
		}
		List<String> result = new ArrayList<String>();
		if (args.length == 3)
		{
			StringUtil.copyPartialMatches(args[2], CommandUtil.clanNames(ctx), result);
		}
		return result;
	}
}
