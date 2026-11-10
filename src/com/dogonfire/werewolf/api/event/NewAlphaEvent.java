package com.dogonfire.werewolf.api.event;

import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.dogonfire.werewolf.api.Clan;

/**
 * Fired on the main thread when a clan is about to get a new alpha. Either player may be offline. Cancelling keeps
 * the old alpha (or, when the old alpha is gone, leaves the slot to the next candidate or empty).
 * <p>
 * Not fired when a clan's alpha slot is cleared because the clan has no members left.
 */
public class NewAlphaEvent extends Event implements Cancellable
{
	/** Why the alpha changes */
	public enum Cause
	{
		/** The slot was empty or the alpha left the clan (cured, changed clan): the best ranked member takes over */
		SUCCESSION,
		/** The alpha was slain by a werewolf of the same clan, or by a hunter (the runner-up takes over) */
		KILL,
		/** An admin used /ww setalpha */
		COMMAND,
		/** Another plugin, through {@link com.dogonfire.werewolf.api.WerewolfService#setAlpha} */
		API
	}

	private static final HandlerList	handlers	= new HandlerList();

	private final Clan					clan;
	private final UUID					oldAlphaId;
	private final UUID					newAlphaId;
	private final Cause					cause;
	private boolean						cancelled;

	/**
	 * Created by Werewolf; other plugins only listen to this event.
	 *
	 * @param clan the clan whose alpha changes
	 * @param oldAlphaId the current alpha, or null if the clan has none
	 * @param newAlphaId the werewolf about to become alpha
	 * @param cause why the alpha changes
	 */
	public NewAlphaEvent(Clan clan, UUID oldAlphaId, UUID newAlphaId, Cause cause)
	{
		this.clan = clan;
		this.oldAlphaId = oldAlphaId;
		this.newAlphaId = newAlphaId;
		this.cause = cause;
	}

	/** The clan whose alpha changes */
	public Clan getClan()
	{
		return clan;
	}

	/** The previous alpha, or null if the clan had none */
	public UUID getOldAlphaId()
	{
		return oldAlphaId;
	}

	/** The werewolf about to become alpha */
	public UUID getNewAlphaId()
	{
		return newAlphaId;
	}

	/** Why the alpha changes */
	public Cause getCause()
	{
		return cause;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}

	/** Cancelling keeps the clan's current alpha */
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
