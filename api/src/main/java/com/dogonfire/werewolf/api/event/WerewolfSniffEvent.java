package com.dogonfire.werewolf.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired on the main thread when a werewolf in wolf form sniffs another player to pick up their scent. Cancelling stops
 * the sniff (no cooldown is used and the target is not set).
 */
public class WerewolfSniffEvent extends PlayerEvent implements Cancellable
{
	private static final HandlerList	handlers	= new HandlerList();

	private final Player				target;
	private boolean						cancelled;

	/**
	 * Created by Werewolf; other plugins only listen to this event.
	 *
	 * @param werewolf the sniffing werewolf
	 * @param target the player being sniffed
	 */
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

	/** The player being sniffed */
	public Player getTarget()
	{
		return target;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}

	/** Cancelling stops the sniff */
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
