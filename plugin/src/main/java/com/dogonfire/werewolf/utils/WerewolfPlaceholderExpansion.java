package com.dogonfire.werewolf.utils;

import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.api.MoonPhase;
import com.dogonfire.werewolf.api.WerewolfProfile;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/*
 * %werewolf_<identifier>%. PlaceholderAPI may call this from any thread (e.g. async chat), so it only reads thread-safe
 * snapshots: WerewolfManager's werewolf profiles, ClanManager's clan names, MoonManager's phase per world and the
 * immutable Settings.
 */
public class WerewolfPlaceholderExpansion extends PlaceholderExpansion
{
	private final Werewolf			plugin;
	private final WerewolfContext	ctx;

	public WerewolfPlaceholderExpansion(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	@Override
	public boolean persist()
	{
		return true;
	}

	@Override
	public boolean canRegister()
	{
		return true;
	}

	@Override
	public String getAuthor()
	{
		return plugin.getDescription().getAuthors().toString();
	}

	@Override
	public String getIdentifier()
	{
		return "werewolf";
	}

	@Override
	public String getVersion()
	{
		return plugin.getDescription().getVersion();
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier)
	{
		if (player == null)
		{
			return "";
		}

		WerewolfProfile profile = ctx.getWerewolfManager().getProfile(player.getUniqueId());

		return switch (identifier)
		{
			// Clan display name, "" for non-werewolves
			case "clan", "clan_name" -> profile != null ? nullToEmpty(ctx.getClanManager().getClanName(profile.clan())) : "";
			// Clan constant (WildBite, WerewolfBite, Potion), "" for non-werewolves
			case "clan_id" -> profile != null ? profile.clan().name() : "";
			// "Werewolf" for werewolves (infected or full), otherwise ""
			case "werewolf" -> profile != null ? "Werewolf" : "";
			// "Wolf" or "Human" for werewolves, "" for everyone else
			case "form" -> profile == null ? "" : profile.isInWolfForm() ? "Wolf" : "Human";
			case "level" -> String.valueOf(profile != null ? profile.level() : 0);
			case "alpha" -> String.valueOf(profile != null && profile.alpha());
			case "infected" -> String.valueOf(profile != null && profile.infected());
			// Name in wolf form
			case "name" -> profile != null ? profile.werewolfName() : "";
			// The moon phase in the player's world, as named in config.yml
			case "moonphase" -> moonPhase(player);
			// Unknown identifier (e.g. %werewolf_fafafaf%)
			default -> null;
		};
	}

	private String moonPhase(Player player)
	{
		MoonPhase phase = ctx.getMoonManager().getPhaseSnapshot(player.getWorld().getUID());
		return phase != null ? nullToEmpty(ctx.getMoonManager().getPhaseName(phase)) : "";
	}

	private static String nullToEmpty(String text)
	{
		return text != null ? text : "";
	}
}
