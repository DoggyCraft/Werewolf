package com.dogonfire.werewolf.api.event;

import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread when a werewolf (infected or full) is about to be cured. The player may be offline (auto
 * cure, admin commands, the API). After a cure the player's werewolf data is removed and, if they are in wolf form,
 * they turn back into a human a moment later.
 */
public class WerewolfCureEvent extends Event implements Cancellable
{
	/** Why the werewolf is cured */
	public enum Cause
	{
		/** Drank or was splashed with a cure potion */
		CURE_POTION,
		/** Didn't transform for {@code Infection.AutoCureDays} */
		AUTO_CURE,
		/** Slain in wolf form with {@code Settings.CureWerewolfWhenSlain} on */
		SLAIN,
		/** Became a vampire (VampireRevamp) */
		VAMPIRISM,
		/** An admin used /ww cure or /ww toggle */
		COMMAND,
		/** Another plugin, through {@link com.dogonfire.werewolf.api.WerewolfService#cure} */
		API
	}

	private static final HandlerList	handlers	= new HandlerList();

	private final UUID					playerId;
	private final Cause					cause;
	private boolean						cancelled;

	/**
	 * Created by Werewolf; other plugins only listen to this event.
	 *
	 * @param playerId the werewolf about to be cured
	 * @param cause why
	 */
	public WerewolfCureEvent(UUID playerId, Cause cause)
	{
		this.playerId = playerId;
		this.cause = cause;
	}

	/** The werewolf about to be cured; may be offline */
	public UUID getPlayerId()
	{
		return playerId;
	}

	/** Why the werewolf is cured */
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

	/** Cancelling keeps the player a werewolf */
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
