package com.dogonfire.werewolf.api.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import com.dogonfire.werewolf.api.Clan;

/**
 * Fired on the main thread when a player is about to become a werewolf: infected (they turn on the next transformation
 * night) or, when {@link #isTurnNow()}, a full werewolf in wolf form right away.
 * <p>
 * For bites the event is fired before the infection roll, for every bite that could infect (the configured risk is
 * above 0): {@link #getChance()} starts at that risk ({@code Infection.WildWolfBiteRisk}, or the biting werewolf's
 * {@code Infection.WerewolfBiteRisk}/{@code WerewolfBiteIntentRisk}), and listeners may change it or cancel. Other
 * sources start with a chance of 1. After the event the infection happens with probability {@link #getChance()};
 * silver armor's protection against bites is rolled after that.
 */
public class WerewolfInfectionEvent extends PlayerEvent implements Cancellable
{
	/** Where the infection comes from */
	public enum Source
	{
		/** Bitten by an untamed wolf */
		WILD_WOLF_BITE,
		/** Bitten by a werewolf in wolf form */
		WEREWOLF_BITE,
		/** Splashed with or drank an infection potion */
		POTION,
		/** An admin used /ww infect or /ww toggle */
		COMMAND,
		/** Another plugin, through {@link com.dogonfire.werewolf.api.WerewolfService#infect} */
		API
	}

	private static final HandlerList	handlers	= new HandlerList();

	private Clan						clan;
	private final Source				source;
	private final boolean				turnNow;
	private final Entity				biter;
	private double						chance;
	private boolean						cancelled;

	/**
	 * Created by Werewolf; other plugins only listen to this event.
	 *
	 * @param player the player about to be infected
	 * @param clan the clan they would join, or null for a random one
	 * @param source where the infection comes from
	 * @param turnNow true if they become a full werewolf in wolf form right away
	 * @param biter the wolf or werewolf that bit them, or null if it was not a bite
	 * @param chance the probability (0 to 1) that the infection happens
	 */
	public WerewolfInfectionEvent(Player player, Clan clan, Source source, boolean turnNow, Entity biter, double chance)
	{
		super(player);
		this.clan = clan;
		this.source = source;
		this.turnNow = turnNow;
		this.biter = biter;
		this.chance = clamp(chance);
	}

	/** The clan the player will join; null means a random clan */
	public Clan getClan()
	{
		return clan;
	}

	/** Changes the clan the player will join; null picks a random clan */
	public void setClan(Clan clan)
	{
		this.clan = clan;
	}

	/** Where the infection comes from */
	public Source getSource()
	{
		return source;
	}

	/** True when the player becomes a full werewolf right away rather than being infected */
	public boolean isTurnNow()
	{
		return turnNow;
	}

	/** The wolf (or werewolf player) that bit the player, or null when the source is not a bite */
	public Entity getBiter()
	{
		return biter;
	}

	/** The probability, from 0 to 1, that the infection happens once the event is through */
	public double getChance()
	{
		return chance;
	}

	/** Changes the probability that the infection happens; clamped to 0..1. 0 never infects, 1 always does. */
	public void setChance(double chance)
	{
		this.chance = clamp(chance);
	}

	private static double clamp(double chance)
	{
		return Double.isNaN(chance) ? 0 : Math.max(0, Math.min(1, chance));
	}

	/** {@inheritDoc} */
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}

	/** Cancelling stops the infection (no roll is made) */
	@Override
	public void setCancelled(boolean cancelled)
	{
		this.cancelled = cancelled;
	}

	/** {@inheritDoc} */
	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}

	/** The handler list Bukkit needs for every event class */
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
