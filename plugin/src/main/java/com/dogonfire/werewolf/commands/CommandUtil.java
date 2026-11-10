package com.dogonfire.werewolf.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** Helpers shared by the command classes */
public final class CommandUtil
{
	public static final String CONSOLE = "CONSOLE";

	private CommandUtil()
	{
	}

	/** The console and ops always pass; a null node means no permission is needed */
	public static boolean hasPermission(WerewolfContext ctx, Player player, String node)
	{
		return node == null || player == null || player.isOp() || ctx.getPermissionsManager().hasPermission(player, node);
	}

	public static String senderName(Player player)
	{
		return player == null ? CONSOLE : player.getName();
	}

	public static void sendNoPermission(WerewolfContext ctx, Player player)
	{
		ctx.getPlugin().sendInfo(player, ctx.getLanguageManager().getLanguageString(LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
	}

	public static void sendUsage(WerewolfContext ctx, Player player, String usage, LANGUAGESTRING help)
	{
		ctx.getLanguageManager().setType(usage);
		ctx.getPlugin().sendInfo(player, ctx.getLanguageManager().getLanguageString(help, ChatColor.RED));
	}

	public static boolean hasEmptyHand(Player player)
	{
		ItemStack hand = player.getInventory().getItemInMainHand();
		return hand == null || hand.getType() == Material.AIR;
	}

	public static Clan randomClan()
	{
		Clan[] clans = Clan.values();
		return clans[ThreadLocalRandom.current().nextInt(clans.length)];
	}

	/** Matches a clan by enum name or display name, ignoring case, colours and spaces */
	public static Clan parseClan(WerewolfContext ctx, String name)
	{
		String wanted = ChatColor.stripColor(name).replace(" ", "");
		for (Clan clan : Clan.values())
		{
			String clanName = ctx.getClanManager().getClanName(clan);
			if (clan.name().equalsIgnoreCase(wanted) || (clanName != null && ChatColor.stripColor(clanName).replace(" ", "").equalsIgnoreCase(wanted)))
			{
				return clan;
			}
		}
		return null;
	}

	/** Clan names as {@link #parseClan} accepts them, for messages and tab completion */
	public static List<String> clanNames(WerewolfContext ctx)
	{
		List<String> names = new ArrayList<String>();
		for (Clan clan : Clan.values())
		{
			String clanName = ctx.getClanManager().getClanName(clan);
			names.add(clanName != null ? ChatColor.stripColor(clanName).replace(" ", "") : clan.name());
		}
		return names;
	}

	/** Names of all werewolves, including offline ones */
	public static Set<String> werewolfNames(WerewolfContext ctx)
	{
		Set<String> names = new HashSet<String>(ctx.getWerewolfManager().getWerewolfPlayerNames());
		for (Player online : ctx.getPlugin().getServer().getOnlinePlayers())
		{
			if (ctx.getWerewolfManager().isWerewolf(online))
			{
				names.add(online.getName());
			}
		}
		return names;
	}

	/**
	 * Finds a player by name, online or not: online players, then werewolves.yml, then players the server has seen.
	 * Unknown names give null rather than a made-up offline player.
	 */
	public static UUID findPlayerId(WerewolfContext ctx, String name)
	{
		Player online = ctx.getPlugin().getServer().getPlayerExact(name);
		if (online != null)
		{
			return online.getUniqueId();
		}

		UUID werewolfId = ctx.getWerewolfManager().findWerewolfByName(name);
		if (werewolfId != null)
		{
			return werewolfId;
		}

		for (OfflinePlayer offline : ctx.getPlugin().getServer().getOfflinePlayers())
		{
			if (name.equalsIgnoreCase(offline.getName()))
			{
				return offline.getUniqueId();
			}
		}

		online = ctx.getPlugin().getServer().getPlayer(name);
		return online != null ? online.getUniqueId() : null;
	}

	/** Looks up a werewolf by name for an admin command, telling the sender if there is none */
	public static UUID findWerewolf(WerewolfContext ctx, Player player, String name)
	{
		UUID playerId = findPlayerId(ctx, name);
		if (playerId == null)
		{
			ctx.getLanguageManager().setPlayerName(name);
			ctx.getPlugin().sendInfo(player, ctx.getLanguageManager().getLanguageString(LANGUAGESTRING.AdminUnknownPlayer, ChatColor.RED));
			return null;
		}

		if (!ctx.getWerewolfManager().isWerewolf(playerId))
		{
			ctx.getLanguageManager().setPlayerName(name);
			ctx.getPlugin().sendInfo(player, ctx.getLanguageManager().getLanguageString(LANGUAGESTRING.AdminNotAWerewolf, ChatColor.RED));
			return null;
		}

		return playerId;
	}
}
