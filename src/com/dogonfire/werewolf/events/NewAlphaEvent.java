package com.dogonfire.werewolf.events;

import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.dogonfire.werewolf.managers.ClanManager.ClanType;

/**
 * Fired when a clan is about to get a new alpha. Either player may be offline.
 */
public class NewAlphaEvent extends Event implements Cancellable
{
	private static final HandlerList	handlers	= new HandlerList();

	private final ClanType				clan;
	private final UUID					oldAlphaId;
	private final UUID					newAlphaId;
	private boolean						cancelled;

	public NewAlphaEvent(ClanType clan, UUID oldAlphaId, UUID newAlphaId)
	{
		this.clan = clan;
		this.oldAlphaId = oldAlphaId;
		this.newAlphaId = newAlphaId;
	}

	public ClanType getClan()
	{
		return clan;
	}

	/** The previous alpha, or null if the clan had none */
	public UUID getOldAlphaId()
	{
		return oldAlphaId;
	}

	public UUID getNewAlphaId()
	{
		return newAlphaId;
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
