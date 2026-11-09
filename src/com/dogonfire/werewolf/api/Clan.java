package com.dogonfire.werewolf.api;

/**
 * The werewolf clans. The constant names are stored in werewolves.yml and clans.yml and used as config keys, so they
 * never change. Each clan's display name is configured in config.yml ({@code Clans.<Clan>.Name}), see
 * {@link WerewolfService#getClanName(Clan)}.
 */
public enum Clan
{
	/** Infected by a wild wolf (Silvermane by default) */
	WildBite,
	/** Infected by a werewolf (Bloodmoon by default) */
	WerewolfBite,
	/** Infected by an infection potion (Witherfang by default) */
	Potion
}
