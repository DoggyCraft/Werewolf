package com.dogonfire.werewolf.managers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.events.TrackerTargetEvent;
import com.dogonfire.werewolf.events.TrackerTargetEvent.TargetType;

/**
 * Werewolf and vampire tracker compasses. A human right-clicks a tracker to lock onto the nearest valid target; while
 * they hold it, their compass points at the target and the action bar shows the distance. Locks are kept in memory.
 */
public class TrackerManager
{
	private static class Lock
	{
		private final UUID			targetId;
		private final TargetType	type;

		private Lock(UUID targetId, TargetType type)
		{
			this.targetId = targetId;
			this.type = type;
		}
	}

	private final Werewolf			plugin;
	private final WerewolfContext ctx;
	private final Map<UUID, Lock>	locks	= new HashMap<UUID, Lock>();

	public TrackerManager(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	public TargetType getTrackerType(ItemStack item)
	{
		if (ctx.getItemManager().isWerewolfTracker(item))
		{
			return TargetType.WEREWOLF;
		}
		if (ctx.getItemManager().isVampireTracker(item))
		{
			return TargetType.VAMPIRE;
		}
		return null;
	}

	/** True if the hunter has locked a tracker onto someone */
	public boolean hasTarget(UUID hunterId)
	{
		return this.locks.containsKey(hunterId);
	}

	public Player getTarget(UUID hunterId)
	{
		Lock lock = this.locks.get(hunterId);
		return lock != null ? this.plugin.getServer().getPlayer(lock.targetId) : null;
	}

	/** Whether the target is one this type of tracker may point at right now (by form and time of day) */
	public boolean isValidTarget(Player hunter, Player target, TargetType type)
	{
		if (target == null || target.equals(hunter) || !target.isOnline() || !target.getWorld().equals(hunter.getWorld()) || target.getGameMode() == GameMode.SPECTATOR)
		{
			return false;
		}

		boolean night = ctx.getMoonManager().isNight(hunter.getWorld());

		if (type == TargetType.WEREWOLF)
		{
			if (!ctx.getWerewolfManager().isWerewolf(target))
			{
				return false;
			}
			boolean wolfForm = ctx.getWerewolfManager().hasWerewolfSkin(target.getUniqueId());
			boolean formAllowed = wolfForm ? this.plugin.getSettings().hunting().trackWerewolfWolfForm() : this.plugin.getSettings().hunting().trackWerewolfHumanForm();
			boolean timeAllowed = night ? this.plugin.getSettings().hunting().trackWerewolfNighttime() : this.plugin.getSettings().hunting().trackWerewolfDaytime();
			return formAllowed && timeAllowed;
		}

		if (!ctx.getWerewolfManager().isVampire(target))
		{
			return false;
		}
		return night ? this.plugin.getSettings().hunting().trackVampireNighttime() : this.plugin.getSettings().hunting().trackVampireDaytime();
	}

	private boolean isHoldingTracker(Player hunter, TargetType type)
	{
		if (type.equals(getTrackerType(hunter.getInventory().getItemInMainHand())))
		{
			return true;
		}
		return this.plugin.getSettings().hunting().trackerUseEitherHand() && type.equals(getTrackerType(hunter.getInventory().getItemInOffHand()));
	}

	/** Right-click: locks onto the nearest valid target that no listener vetoes */
	public void lock(Player hunter, TargetType type, String itemName)
	{
		Lock existing = this.locks.get(hunter.getUniqueId());
		if (existing != null)
		{
			Player target = this.plugin.getServer().getPlayer(existing.targetId);
			ctx.getLanguageManager().setPlayerName(target != null ? target.getName() : "?");
			hunter.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrackerAlreadyTracking, ChatColor.AQUA));
			return;
		}

		List<Player> candidates = new ArrayList<Player>();
		for (Player player : hunter.getWorld().getPlayers())
		{
			if (isValidTarget(hunter, player, type))
			{
				candidates.add(player);
			}
		}
		candidates.sort(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(hunter.getLocation())));

		for (Player target : candidates)
		{
			TrackerTargetEvent event = new TrackerTargetEvent(hunter, target, type, false);
			this.plugin.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled())
			{
				continue;
			}

			this.locks.put(hunter.getUniqueId(), new Lock(target.getUniqueId(), type));
			hunter.setCompassTarget(target.getLocation());

			ctx.getLanguageManager().setType(itemName);
			ctx.getLanguageManager().setPlayerName(target.getName());
			ctx.getLanguageManager().setAmount("" + Math.round(hunter.getLocation().distance(target.getLocation())));
			hunter.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrackerFound, ChatColor.AQUA));
			return;
		}

		hunter.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrackerNoTargets, ChatColor.RED));
	}

	/** Left-click: lets go of the target */
	public void clear(Player hunter, String itemName)
	{
		Lock lock = this.locks.remove(hunter.getUniqueId());
		ctx.getLanguageManager().setType(itemName);
		if (lock == null)
		{
			hunter.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrackerNoTarget, ChatColor.RED));
			return;
		}

		resetCompass(hunter);
		hunter.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrackerCleared, ChatColor.AQUA));
	}

	/** Drops a hunter's lock without a message, e.g. when they leave */
	public void remove(UUID hunterId)
	{
		this.locks.remove(hunterId);
	}

	public void clearAll()
	{
		for (UUID hunterId : this.locks.keySet())
		{
			Player hunter = this.plugin.getServer().getPlayer(hunterId);
			if (hunter != null)
			{
				resetCompass(hunter);
			}
		}
		this.locks.clear();
	}

	private void resetCompass(Player hunter)
	{
		hunter.setCompassTarget(hunter.getWorld().getSpawnLocation());
	}

	/** Runs once a second: points each hunter's compass at their target, or loses the target if it is no longer valid */
	public void update()
	{
		for (Map.Entry<UUID, Lock> entry : new ArrayList<Map.Entry<UUID, Lock>>(this.locks.entrySet()))
		{
			Player hunter = this.plugin.getServer().getPlayer(entry.getKey());
			if (hunter == null)
			{
				this.locks.remove(entry.getKey());
				continue;
			}

			Lock lock = entry.getValue();
			Player target = this.plugin.getServer().getPlayer(lock.targetId);

			if (this.plugin.getSettings().hunting().enabled() && target != null && ctx.getWerewolfManager().isHuman(hunter) && isHoldingTracker(hunter, lock.type) && isValidTarget(hunter, target, lock.type))
			{
				TrackerTargetEvent event = new TrackerTargetEvent(hunter, target, lock.type, true);
				this.plugin.getServer().getPluginManager().callEvent(event);
				if (!event.isCancelled())
				{
					hunter.setCompassTarget(target.getLocation());

					ctx.getLanguageManager().setPlayerName(target.getName());
					ctx.getLanguageManager().setAmount("" + Math.round(hunter.getLocation().distance(target.getLocation())));
					String message = ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrackerActionBar, ChatColor.AQUA);
					hunter.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
					continue;
				}
			}

			this.locks.remove(entry.getKey());
			resetCompass(hunter);

			String targetName = target != null ? target.getName() : this.plugin.getServer().getOfflinePlayer(lock.targetId).getName();
			ctx.getLanguageManager().setPlayerName(targetName != null ? targetName : "?");
			hunter.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrackerLost, ChatColor.RED));
		}
	}
}
