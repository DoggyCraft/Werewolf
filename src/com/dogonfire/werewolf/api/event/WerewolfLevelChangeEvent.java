package com.dogonfire.werewolf.api.event;

import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread when a werewolf's level (the number of full moons it has transformed under, which unlocks
 * the {@code Maturity.*} perks) is about to change. The player may be offline (admin commands, the API). Not fired
 * when a werewolf is cured (its data, level included, is removed) or newly infected (it starts at level 0).
 */
public class WerewolfLevelChangeEvent extends Event implements Cancellable
{
	/** Why the level changes */
	public enum Cause
	{
		/** The werewolf transformed under a transformation night; at most once per night. A cancelled level is not retried that night. */
		FULL_MOON,
		/** An admin used /ww setlevel or /ww addlevel */
		COMMAND,
		/** Another plugin, through {@link com.dogonfire.werewolf.api.WerewolfService#setLevel} */
		API
	}

	private static final HandlerList	handlers	= new HandlerList();

	private final UUID					playerId;
	private final int					oldLevel;
	private int							newLevel;
	private final Cause					cause;
	private boolean						cancelled;

	/**
	 * Created by Werewolf; other plugins only listen to this event.
	 *
	 * @param playerId the werewolf
	 * @param oldLevel its current level
	 * @param newLevel the level it is about to get (0 or more)
	 * @param cause why the level changes
	 */
	public WerewolfLevelChangeEvent(UUID playerId, int oldLevel, int newLevel, Cause cause)
	{
		this.playerId = playerId;
		this.oldLevel = oldLevel;
		this.newLevel = newLevel;
		this.cause = cause;
	}

	/** The werewolf whose level changes */
	public UUID getPlayerId()
	{
		return playerId;
	}

	/** The werewolf's current level */
	public int getOldLevel()
	{
		return oldLevel;
	}

	/** The level the werewolf is about to get */
	public int getNewLevel()
	{
		return newLevel;
	}

	/** Changes the level the werewolf gets; negative values become 0 */
	public void setNewLevel(int newLevel)
	{
		this.newLevel = Math.max(0, newLevel);
	}

	/** Why the level changes */
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

	/** Cancelling keeps the old level */
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
