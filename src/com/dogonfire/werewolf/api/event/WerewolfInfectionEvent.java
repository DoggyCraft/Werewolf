package com.dogonfire.werewolf.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import com.dogonfire.werewolf.api.Clan;

/**
 * Fired when a player is about to become a werewolf (infected, or turned immediately).
 */
public class WerewolfInfectionEvent extends PlayerEvent implements Cancellable
{
	public enum Source
	{
		WILD_WOLF_BITE, WEREWOLF_BITE, POTION, COMMAND, API
	}

	private static final HandlerList	handlers	= new HandlerList();

	private Clan					clan;
	private final Source				source;
	private final boolean				turnNow;
	private boolean						cancelled;

	public WerewolfInfectionEvent(Player player, Clan clan, Source source, boolean turnNow)
	{
		super(player);
		this.clan = clan;
		this.source = source;
		this.turnNow = turnNow;
	}

	public Clan getClan()
	{
		return clan;
	}

	public void setClan(Clan clan)
	{
		this.clan = clan;
	}

	public Source getSource()
	{
		return source;
	}

	/** True when the player becomes a full werewolf right away rather than being infected */
	public boolean isTurnNow()
	{
		return turnNow;
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
