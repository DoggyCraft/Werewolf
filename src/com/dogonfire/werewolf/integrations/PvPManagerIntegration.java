package com.dogonfire.werewolf.integrations;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.events.TrackerTargetEvent;
import com.dogonfire.werewolf.events.WerewolfSniffEvent;

import me.chancesd.pvpmanager.PvPManager;

/**
 * Trackers can't lock onto, and werewolves can't sniff, players that PvPManager wouldn't let the hunter attack (PvP
 * toggled off on either side, newbie/respawn protection, PvP-free world...). Only registered when PvPManager 4.x is
 * enabled.
 */
public class PvPManagerIntegration implements IntegrationListener
{
	private final Werewolf	plugin;

	public PvPManagerIntegration(Werewolf plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void onTrackerTarget(TrackerTargetEvent event)
	{
		if (isProtected(event.getHunter(), event.getTarget()))
		{
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onSniff(WerewolfSniffEvent event)
	{
		if (isProtected(event.getWerewolf(), event.getTarget()))
		{
			event.setCancelled(true);
		}
	}

	private boolean isProtected(Player hunter, Player target)
	{
		if (!this.plugin.getSettings().integrations().pvpManager())
		{
			return false;
		}

		try
		{
			return PvPManager.getInstance().getPlayerManager().checkProtection(hunter, target).isProtected();
		}
		catch (RuntimeException e)
		{
			this.plugin.logDebug("PvPManager protection check failed for " + hunter.getName() + " -> " + target.getName() + ": " + e);
			return false;
		}
	}
}
