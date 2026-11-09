package com.dogonfire.werewolf.integrations;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.events.TrackerTargetEvent;
import com.dogonfire.werewolf.events.WerewolfSniffEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;

/**
 * Trackers can't lock onto, and werewolves can't sniff, players while either side is in a region with PvP denied, or
 * while the target is in an invincible region. Only registered when WorldGuard is enabled and Integrations.WorldGuard is on.
 */
public class WorldGuardIntegration implements IntegrationListener
{
	private final Werewolf	plugin;
	private final WerewolfContext ctx;

	public WorldGuardIntegration(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
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
		if (!this.plugin.getSettings().integrations().worldGuard())
		{
			return false;
		}

		RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();

		return isPvPDenied(query, hunter) || isPvPDenied(query, target) || query.queryState(BukkitAdapter.adapt(target.getLocation()), WorldGuardPlugin.inst().wrapPlayer(target), Flags.INVINCIBILITY) == StateFlag.State.ALLOW;
	}

	// An unset pvp flag means allowed, like WorldGuard's own PvP check
	private boolean isPvPDenied(RegionQuery query, Player player)
	{
		return query.queryState(BukkitAdapter.adapt(player.getLocation()), WorldGuardPlugin.inst().wrapPlayer(player), Flags.PVP) == StateFlag.State.DENY;
	}
}
