package com.dogonfire.werewolf.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired on the main thread when a hunter's tracker compass locks onto a target, and again every time it updates.
 * Cancelling when locking makes the tracker try the next nearest target; cancelling an update makes the tracker lose
 * the target.
 */
public class TrackerTargetEvent extends PlayerEvent implements Cancellable
{
	/** What the tracker tracks */
	public enum TargetType
	{
		/** A werewolf tracker, tracking a werewolf */
		WEREWOLF,
		/** A vampire tracker, tracking a vampire (VampireRevamp) */
		VAMPIRE
	}

	private static final HandlerList	handlers	= new HandlerList();

	private final Player				target;
	private final TargetType			type;
	private final boolean				update;
	private boolean						cancelled;

	/**
	 * Created by Werewolf; other plugins only listen to this event.
	 *
	 * @param hunter the player holding the tracker
	 * @param target the tracked player
	 * @param type what the tracker tracks
	 * @param update false when locking on, true for the periodic updates after that
	 */
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

	/** The tracked player */
	public Player getTarget()
	{
		return target;
	}

	/** What the tracker tracks */
	public TargetType getType()
	{
		return type;
	}

	/** False when the tracker is locking onto the target, true for the periodic updates after that */
	public boolean isUpdate()
	{
		return update;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}

	/** Cancelling skips this target when locking on, or loses it on an update */
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
