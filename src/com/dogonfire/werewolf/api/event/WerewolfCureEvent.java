package com.dogonfire.werewolf.api.event;

import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a werewolf is about to be cured. The player may be offline (auto cure).
 */
public class WerewolfCureEvent extends Event implements Cancellable
{
	public enum Cause
	{
		CURE_POTION, AUTO_CURE, SLAIN, VAMPIRISM, COMMAND, API
	}

	private static final HandlerList	handlers	= new HandlerList();

	private final UUID					playerId;
	private final Cause					cause;
	private boolean						cancelled;

	public WerewolfCureEvent(UUID playerId, Cause cause)
	{
		this.playerId = playerId;
		this.cause = cause;
	}

	public UUID getPlayerId()
	{
		return playerId;
	}

	public Cause getCause()
	{
		return cause;
	}

	@Override
	public boolean isCancelled()
	{
		return cancelled;
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
