package com.dogonfire.werewolf.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import com.dogonfire.werewolf.managers.ClanManager.ClanType;

/**
 * Fired when a werewolf is about to change into wolf form or back into human form.
 * Cancelling has no effect when {@link #isCancellable()} is false (the player is leaving or dying).
 */
public class WerewolfTransformEvent extends PlayerEvent implements Cancellable
{
	public enum Reason
	{
		/** Night or full moon, from the main loop */
		MOON,
		/** Wolf form ran out of time */
		TIME_LIMIT,
		/** The werewolf chose to with /ww transform */
		CONTROLLED,
		/** Infection that turns the player immediately (infection potion) */
		INFECTION,
		/** Splashed with wolfbane */
		WOLFBANE,
		/** An admin command or the plugin being switched on/off */
		COMMAND,
		/** Re-applying the skin, e.g. after becoming alpha */
		REFRESH,
		/** Quit, join, world change or death; can't be cancelled */
		FORCED,
		/** Any other plugin, through WerewolfAPI */
		API
	}

	private static final HandlerList	handlers	= new HandlerList();

	private final ClanType				clan;
	private final boolean				toWolfForm;
	private final Reason				reason;
	private boolean						cancelled;

	public WerewolfTransformEvent(Player player, ClanType clan, boolean toWolfForm, Reason reason)
	{
		super(player);
		this.clan = clan;
		this.toWolfForm = toWolfForm;
		this.reason = reason;
	}

	public ClanType getClan()
	{
		return clan;
	}

	public boolean isToWolfForm()
	{
		return toWolfForm;
	}

	public Reason getReason()
	{
		return reason;
	}

	public boolean isCancellable()
	{
		return reason != Reason.FORCED;
	}

	@Override
	public boolean isCancelled()
	{
		return cancelled && isCancellable();
	}

	@Override
	public void setCancelled(boolean cancelled)
	{
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}

	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
