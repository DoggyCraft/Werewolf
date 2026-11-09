package com.dogonfire.werewolf.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/**
 * One command, either a /werewolf subcommand or a standalone command like /growl.
 * The executor checks console use and permission before calling {@link #execute}.
 * Argument arrays always include the subcommand name at index 0.
 */
public abstract class SubCommand
{
	public record HelpEntry(String usage, LANGUAGESTRING key)
	{
	}

	protected final Werewolf plugin;
	private final String name;
	private final String permission;
	private final boolean playerOnly;
	private final String usage;
	private final LANGUAGESTRING helpKey;

	/**
	 * @param permission node checked before running, or null for none. Ops and the console always pass.
	 * @param playerOnly the console gets a message instead of running the command
	 * @param usage usage text shown in /ww help, or null to leave the command out of the help listing
	 * @param helpKey language string for the help line, with $Type set to the usage
	 */
	protected SubCommand(Werewolf plugin, String name, String permission, boolean playerOnly, String usage, LANGUAGESTRING helpKey)
	{
		this.plugin = plugin;
		this.name = name;
		this.permission = permission;
		this.playerOnly = playerOnly;
		this.usage = usage;
		this.helpKey = helpKey;
	}

	public String name()
	{
		return this.name;
	}

	public List<String> aliases()
	{
		return List.of();
	}

	public boolean matches(String label)
	{
		if (this.name.equalsIgnoreCase(label))
		{
			return true;
		}
		for (String alias : aliases())
		{
			if (alias.equalsIgnoreCase(label))
			{
				return true;
			}
		}
		return false;
	}

	/** Whether this command handles the given total number of arguments (including its own name) */
	public boolean acceptsArgs(int length)
	{
		return length == 1;
	}

	public String permission(String[] args)
	{
		return this.permission;
	}

	public boolean isPlayerOnly(String[] args)
	{
		return this.playerOnly;
	}

	public boolean isPermitted(Player player, String[] args)
	{
		return CommandUtil.hasPermission(player, permission(args));
	}

	/** Whether the command makes sense right now for this player (feature enabled, clan alpha, ...). Only affects help and tab completion */
	public boolean isAvailable(Player player)
	{
		return true;
	}

	/** Whether the command is offered in first-argument tab completion */
	public boolean isListed(Player player)
	{
		String[] args = { this.name };
		return isAvailable(player) && (player != null || !isPlayerOnly(args)) && isPermitted(player, args);
	}

	protected String usage()
	{
		return this.usage;
	}

	/** Lines this command adds to /ww help for a player */
	public List<HelpEntry> help(Player player)
	{
		if (usage() == null || !isAvailable(player) || !CommandUtil.hasPermission(player, this.permission))
		{
			return List.of();
		}
		return List.of(new HelpEntry(usage(), this.helpKey));
	}

	/** @param player the sender as a player, or null for the console */
	public abstract void execute(CommandSender sender, Player player, String[] args);

	/** Suggestions for args[args.length - 1] (args.length >= 2). Null means online player names */
	public List<String> tabComplete(CommandSender sender, Player player, String[] args)
	{
		return new ArrayList<String>();
	}

	protected static LanguageManager lang()
	{
		return Werewolf.getLanguageManager();
	}

	protected static String text(LANGUAGESTRING key, ChatColor color)
	{
		return lang().getLanguageString(key, color);
	}

	/** Sends to the player, or logs when player is null */
	protected void send(Player player, String message)
	{
		this.plugin.sendInfo(player, message);
	}

	protected void send(Player player, LANGUAGESTRING key, ChatColor color)
	{
		this.plugin.sendInfo(player, text(key, color));
	}
}
