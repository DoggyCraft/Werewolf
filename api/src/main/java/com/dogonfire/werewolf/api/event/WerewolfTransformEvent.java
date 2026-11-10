package com.dogonfire.werewolf.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import com.dogonfire.werewolf.api.Clan;

/**
 * Fired on the main thread when a werewolf is about to change into wolf form or back into human form. The skin and
 * wolf form effects are applied a tick later. Cancelling has no effect when {@link #isCancellable()} is false (the
 * player is leaving, dying or the plugin is shutting down).
 */
public class WerewolfTransformEvent extends PlayerEvent implements Cancellable
{
	/** Why the werewolf changes form */
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
		/** Quit, join, world change, death or shutdown; can't be cancelled */
		FORCED,
		/** Any other plugin, through {@link com.dogonfire.werewolf.api.WerewolfService} */
		API
	}

	private static final HandlerList	handlers	= new HandlerList();

	private final Clan					clan;
	private final boolean				toWolfForm;
	private final Reason				reason;
	private boolean						cancelled;

	/**
	 * Created by Werewolf; other plugins only listen to this event.
	 *
	 * @param player the werewolf
	 * @param clan the werewolf's clan
	 * @param toWolfForm true when changing into wolf form, false when changing back into human form
	 * @param reason why
	 */
	public WerewolfTransformEvent(Player player, Clan clan, boolean toWolfForm, Reason reason)
	{
		super(player);
		this.clan = clan;
		this.toWolfForm = toWolfForm;
		this.reason = reason;
	}

	/** The werewolf's clan */
	public Clan getClan()
	{
		return clan;
	}

	/** True when changing into wolf form (or refreshing it), false when changing back into human form */
	public boolean isToWolfForm()
	{
		return toWolfForm;
	}

	/** Why the werewolf changes form */
	public Reason getReason()
	{
		return reason;
	}

	/** False for {@link Reason#FORCED}: the change happens even if the event is cancelled */
	public boolean isCancellable()
	{
		return reason != Reason.FORCED;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isCancelled()
	{
		return cancelled && isCancellable();
	}

	/** Cancelling keeps the werewolf in its current form, unless the event is not {@link #isCancellable() cancellable} */
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
