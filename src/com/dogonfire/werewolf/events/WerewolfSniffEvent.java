package com.dogonfire.werewolf.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired when a werewolf in wolf form sniffs another player to pick up their scent. Cancelling stops the sniff (no
 * cooldown is used and the target is not set).
 */
public class WerewolfSniffEvent extends PlayerEvent implements Cancellable
{
	private static final HandlerList	handlers	= new HandlerList();

	private final Player				target;
	private boolean						cancelled;

	public WerewolfSniffEvent(Player werewolf, Player target)
	{
		super(werewolf);
		this.target = target;
	}

	/** The sniffing werewolf, same as {@link #getPlayer()} */
	public Player getWerewolf()
	{
		return getPlayer();
	}

	public Player getTarget()
	{
		return target;
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
