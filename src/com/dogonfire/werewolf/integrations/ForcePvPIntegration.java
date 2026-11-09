package com.dogonfire.werewolf.integrations;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.managers.LanguageManager;

import me.chancesd.pvpmanager.event.PlayerTogglePvPEvent;
import me.chancesd.pvpmanager.player.CombatPlayer;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

/**
 * Forces PvPManager's PvP on for players inside a GriefPrevention claim they have no trust in, and turns it back off
 * when they leave the claim or log out. Only players whose PvP this turned on are tracked and restored, and they can't
 * toggle PvP off while trespassing. Only registered when both GriefPrevention and PvPManager 4.x are enabled.
 */
public class ForcePvPIntegration implements IntegrationListener
{
	public static final String	BYPASS_FORCE_PVP	= "werewolf.bypass.forcepvp";

	private final Werewolf		plugin;
	private final Set<UUID>		forced				= new HashSet<UUID>();
	private boolean				restoring			= false;

	public ForcePvPIntegration(Werewolf plugin)
	{
		this.plugin = plugin;
	}

	private boolean isActive()
	{
		return this.plugin.forcePvPEnabled && this.plugin.huntingEnabled && this.plugin.pvpManagerIntegration && this.plugin.griefPreventionIntegration;
	}

	// Claims are columns, so only a change of block column (or world) can change the claim
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onMove(PlayerMoveEvent event)
	{
		Location from = event.getFrom();
		Location to = event.getTo();
		if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ() && from.getWorld() == to.getWorld()))
		{
			return;
		}

		update(event.getPlayer(), to);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event)
	{
		if (event.getTo() != null)
		{
			update(event.getPlayer(), event.getTo());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onRespawn(PlayerRespawnEvent event)
	{
		update(event.getPlayer(), event.getRespawnLocation());
	}

	// PvPManager may still be loading the player's saved PvP state when they join
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event)
	{
		final UUID playerId = event.getPlayer().getUniqueId();
		this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
			Player player = this.plugin.getServer().getPlayer(playerId);
			if (player != null)
			{
				update(player, player.getLocation());
			}
		}, 20L);
	}

	// Before PvPManager saves the player's PvP state on quit
	@EventHandler(priority = EventPriority.LOWEST)
	public void onQuit(PlayerQuitEvent event)
	{
		restore(event.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void onTogglePvP(PlayerTogglePvPEvent event)
	{
		if (this.restoring || event.getPvPState() || !this.forced.contains(event.getPlayer().getUniqueId()))
		{
			return;
		}

		event.setCancelled(true);
		event.getPlayer().sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ForcePvPCannotDisable, ChatColor.RED));
	}

	private void update(Player player, Location location)
	{
		Claim claim = isActive() && location != null ? getTrespassedClaim(player, location) : null;
		if (claim == null)
		{
			restore(player);
			return;
		}

		CombatPlayer combatPlayer = CombatPlayer.get(player);
		if (combatPlayer == null || combatPlayer.hasPvPEnabled())
		{
			return;
		}

		combatPlayer.setPvP(true);
		if (combatPlayer.hasPvPEnabled())
		{
			this.forced.add(player.getUniqueId());
			Werewolf.getLanguageManager().setPlayerName(claim.getOwnerName());
			player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ForcePvPTrespassing, ChatColor.RED));
		}
	}

	private Claim getTrespassedClaim(Player player, Location location)
	{
		if (player.hasPermission(BYPASS_FORCE_PVP))
		{
			return null;
		}

		Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, true, null);
		if (claim == null)
		{
			return null;
		}

		if (claim.isAdminClaim() ? !this.plugin.forcePvPAdminClaims : !this.plugin.forcePvPClaims)
		{
			return null;
		}

		// Access is the lowest trust level, so being denied it means no trust at all
		return claim.checkPermission(player, ClaimPermission.Access, null) != null ? claim : null;
	}

	private void restore(Player player)
	{
		if (!this.forced.remove(player.getUniqueId()))
		{
			return;
		}

		CombatPlayer combatPlayer = CombatPlayer.get(player);
		if (combatPlayer == null)
		{
			return;
		}

		this.restoring = true;
		try
		{
			combatPlayer.setPvP(false);
		}
		finally
		{
			this.restoring = false;
		}
	}

	@Override
	public void shutdown()
	{
		for (Player player : this.plugin.getServer().getOnlinePlayers())
		{
			restore(player);
		}
		this.forced.clear();
	}
}
