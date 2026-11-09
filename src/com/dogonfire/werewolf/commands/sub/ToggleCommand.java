package com.dogonfire.werewolf.commands.sub;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.CommandUtil;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.events.WerewolfCureEvent;
import com.dogonfire.werewolf.events.WerewolfInfectionEvent;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww toggle [player]: makes yourself or an online player a full werewolf in a random clan, or cures them */
public class ToggleCommand extends SubCommand
{
	private static final String SELF_PERMISSION = "werewolf.togglewerewolfself";
	private static final String OTHER_PERMISSION = "werewolf.togglewerewolf";

	public ToggleCommand(WerewolfContext ctx)
	{
		super(ctx, "toggle", SELF_PERMISSION, false, null, null);
	}

	@Override
	public boolean acceptsArgs(int length)
	{
		return length == 1 || length == 2;
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
		if (CommandUtil.hasPermission(ctx, player, OTHER_PERMISSION))
		{
			entries.add(new HelpEntry("/ww toggle <playername>", LANGUAGESTRING.HelpToggleOther));
		}
		if (CommandUtil.hasPermission(ctx, player, SELF_PERMISSION))
		{
			entries.add(new HelpEntry("/ww toggle", LANGUAGESTRING.HelpToggleSelf));
		}
		return entries;
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (args.length == 1)
		{
			toggleSelf(player);
		}
		else
		{
			toggleOther(player, args[1]);
		}
	}

	private void toggleSelf(Player player)
	{
		if (!ctx.getWerewolfManager().isWerewolf(player))
		{
			if (!ctx.getWerewolfManager().makeWerewolf(player, true, CommandUtil.randomClan(), WerewolfInfectionEvent.Source.COMMAND))
			{
				lang().setPlayerName(player.getName());
				send(player, LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED);
				return;
			}

			ctx.getWerewolfManager().setInfectedThisNight(player.getUniqueId(), player.getName(), false);

			send(player, LANGUAGESTRING.NowAWerewolf, ChatColor.AQUA);
		}
		else
		{
			if (!ctx.getWerewolfManager().unmakeWerewolf(player.getUniqueId(), WerewolfCureEvent.Cause.COMMAND))
			{
				lang().setPlayerName(player.getName());
				send(player, LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED);
				return;
			}
			send(player, LANGUAGESTRING.NotAWerewolfAnymore, ChatColor.AQUA);
		}

		this.plugin.log(player.getName() + ": /werewolf toggleself");
	}

	private void toggleOther(Player player, String name)
	{
		Player target = this.plugin.getServer().getPlayer(name);
		if (target == null)
		{
			send(player, ChatColor.RED + "Invalid player name...");
			return;
		}

		if (!ctx.getWerewolfManager().isWerewolf(target))
		{
			if (!ctx.getWerewolfManager().makeWerewolf(target, true, CommandUtil.randomClan(), WerewolfInfectionEvent.Source.COMMAND))
			{
				lang().setPlayerName(target.getName());
				send(player, LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED);
				return;
			}
			ctx.getWerewolfManager().setInfectedThisNight(target.getUniqueId(), target.getName(), false);
			if (player != null)
			{
				player.sendMessage(ChatColor.AQUA + target.getName() + " is now a werewolf! ");
			}
			else
			{
				this.plugin.log(target.getName() + " is now a werewolf! ");
			}
			lang().setPlayerName(CommandUtil.senderName(player));
			target.sendMessage(text(LANGUAGESTRING.MadeWerewolf, ChatColor.AQUA));
		}
		else
		{
			if (!ctx.getWerewolfManager().unmakeWerewolf(target.getUniqueId(), WerewolfCureEvent.Cause.COMMAND))
			{
				lang().setPlayerName(target.getName());
				send(player, LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED);
				return;
			}
			if (player != null)
			{
				player.sendMessage(ChatColor.AQUA + target.getName() + " is no longer a werewolf ...");
			}
			else
			{
				this.plugin.log(target.getName() + " is no longer a werewolf ...");
			}
			lang().setPlayerName(CommandUtil.senderName(player));
			target.sendMessage(text(LANGUAGESTRING.UnMadeWerewolf, ChatColor.AQUA));
		}

		if (player != null)
		{
			this.plugin.log(player.getName() + ": /werewolf toggle " + target.getName());
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, Player player, String[] args)
	{
		return args.length == 2 ? null : super.tabComplete(sender, player, args);
	}
}
