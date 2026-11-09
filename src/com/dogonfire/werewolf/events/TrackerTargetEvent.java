package com.dogonfire.werewolf.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired when a hunter's tracker compass locks onto a target, and again every time it updates. Cancelling when locking
 * makes the tracker try the next nearest target; cancelling an update makes the tracker lose the target.
 */
public class TrackerTargetEvent extends PlayerEvent implements Cancellable
{
	public enum TargetType
	{
		WEREWOLF, VAMPIRE
	}

	private static final HandlerList	handlers	= new HandlerList();

	private final Player				target;
	private final TargetType			type;
	private final boolean				update;
	private boolean						cancelled;

	public TrackerTargetEvent(Player hunter, Player target, TargetType type, boolean update)
	{
		super(hunter);
		this.target = target;
		this.type = type;
		this.update = update;
	}

	/** The player holding the tracker, same as {@link #getPlayer()} */
	public Player getHunter()
	{
		return getPlayer();
	}

	public Player getTarget()
	{
		return target;
	}

	public TargetType getType()
	{
		return type;
	}

	/** False when the tracker is locking onto the target, true for the periodic updates after that */
	public boolean isUpdate()
	{
		return update;
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
