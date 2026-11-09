package com.dogonfire.werewolf.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.events.WerewolfSniffEvent;

/**
 * Scent tracking: from Maturity.ScentTrack a werewolf in wolf form can sniff a player to pick up their scent, then
 * follow a particle trail towards them with /ww track (blinded and slowed while doing so). All state is in memory.
 */
public class ScentManager
{
	// Effects are refreshed every second, so they wear off quickly if tracking stops without cleanup
	private static final int		EFFECT_TICKS	= 60;

	private final Werewolf			plugin;
	private final WerewolfContext ctx;
	private final Random			random			= new Random();
	private final Map<UUID, UUID>	targets			= new HashMap<UUID, UUID>();
	private final Map<UUID, Long>	lastSniffs		= new HashMap<UUID, Long>();
	private final Set<UUID>			tracking		= new HashSet<UUID>();

	public ScentManager(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	public boolean canScentTrack(UUID playerId)
	{
		return ctx.getWerewolfManager().getNumberOfTransformations(playerId) >= this.plugin.getSettings().maturity().scentTrack();
	}

	public UUID getTarget(UUID werewolfId)
	{
		return this.targets.get(werewolfId);
	}

	public boolean isTracking(UUID werewolfId)
	{
		return this.tracking.contains(werewolfId);
	}

	/**
	 * A werewolf in wolf form sniffs another player. Returns false if nothing happened (not able to, on cooldown or
	 * cancelled by a listener).
	 */
	public boolean sniff(Player werewolf, Player target)
	{
		UUID werewolfId = werewolf.getUniqueId();
		if (!ctx.getWerewolfManager().hasWerewolfSkin(werewolfId) || !canScentTrack(werewolfId) || werewolf.equals(target))
		{
			return false;
		}

		Long lastSniff = this.lastSniffs.get(werewolfId);
		if (lastSniff != null && System.currentTimeMillis() - lastSniff < this.plugin.getSettings().cooldowns().sniff() * 1000L)
		{
			return false;
		}

		WerewolfSniffEvent event = new WerewolfSniffEvent(werewolf, target);
		this.plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled())
		{
			return false;
		}

		this.lastSniffs.put(werewolfId, System.currentTimeMillis());
		werewolf.getWorld().playSound(werewolf.getLocation(), Sound.ENTITY_WOLF_PANT, 1.0F, 0.6F);

		if (this.random.nextInt(100) < this.plugin.getSettings().scent().chance())
		{
			this.targets.put(werewolfId, target.getUniqueId());
			ctx.getLanguageManager().setPlayerName(target.getName());
			werewolf.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ScentFound, ChatColor.LIGHT_PURPLE));
		}
		else
		{
			werewolf.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ScentTry, ChatColor.GRAY));
		}

		return true;
	}

	public void startTracking(Player werewolf)
	{
		this.tracking.add(werewolf.getUniqueId());
		applyEffects(werewolf);
	}

	public void stopTracking(Player werewolf)
	{
		if (!this.tracking.remove(werewolf.getUniqueId()))
		{
			return;
		}

		removeEffect(werewolf, PotionEffectType.BLINDNESS, 0);
		if (this.plugin.getSettings().scent().slownessAmplifier() >= 0)
		{
			removeEffect(werewolf, PotionEffectType.SLOWNESS, this.plugin.getSettings().scent().slownessAmplifier());
		}
	}

	/** Forgets everything about a player, e.g. when they leave */
	public void clear(Player player)
	{
		stopTracking(player);
		this.targets.remove(player.getUniqueId());
		this.lastSniffs.remove(player.getUniqueId());
	}

	public void stopAll()
	{
		for (UUID werewolfId : new HashSet<UUID>(this.tracking))
		{
			Player werewolf = this.plugin.getServer().getPlayer(werewolfId);
			if (werewolf != null)
			{
				stopTracking(werewolf);
			}
		}
		this.tracking.clear();
	}

	/** Runs once a second: shows each tracking werewolf the trail to their target */
	public void update()
	{
		for (UUID werewolfId : new HashSet<UUID>(this.tracking))
		{
			Player werewolf = this.plugin.getServer().getPlayer(werewolfId);
			if (werewolf == null)
			{
				this.tracking.remove(werewolfId);
				continue;
			}

			if (!ctx.getWerewolfManager().hasWerewolfSkin(werewolfId))
			{
				stopTracking(werewolf);
				continue;
			}

			UUID targetId = this.targets.get(werewolfId);
			Player target = targetId != null ? this.plugin.getServer().getPlayer(targetId) : null;
			if (target == null || !target.getWorld().equals(werewolf.getWorld()))
			{
				stopTracking(werewolf);
				werewolf.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ScentLost, ChatColor.RED));
				continue;
			}

			applyEffects(werewolf);
			showTrail(werewolf, target);
		}
	}

	private void showTrail(Player werewolf, Player target)
	{
		Vector start = werewolf.getLocation().toVector();
		Vector direction = target.getLocation().toVector().subtract(start);
		if (direction.lengthSquared() < 0.01D || this.plugin.getSettings().scent().range() <= 0)
		{
			return;
		}

		double distance = werewolf.getLocation().distance(target.getLocation());
		Color color;
		if (distance >= this.plugin.getSettings().scent().distanceFar())
		{
			color = Color.fromRGB(128, 128, 128);
		}
		else if (distance >= this.plugin.getSettings().scent().distanceClose())
		{
			color = Color.fromRGB(0, 128, 0);
		}
		else if (distance >= this.plugin.getSettings().scent().distanceVeryClose())
		{
			color = Color.fromRGB(128, 128, 0);
		}
		else
		{
			color = Color.fromRGB(128, 0, 0);
		}

		Particle.DustOptions dust = new Particle.DustOptions(color, 1.0F);
		int range = (int) Math.min(this.plugin.getSettings().scent().range(), Math.ceil(distance));
		BlockIterator iterator = new BlockIterator(werewolf.getWorld(), start, direction, this.plugin.getSettings().scent().yOffset(), Math.max(1, range));
		while (iterator.hasNext())
		{
			Location location = iterator.next().getLocation().add(0.5D, 0.5D, 0.5D);
			werewolf.spawnParticle(Particle.DUST, location, Math.max(1, this.plugin.getSettings().scent().particleAmount()), 0.2D, 0.2D, 0.2D, 0.0D, dust);
		}
	}

	private void applyEffects(Player werewolf)
	{
		werewolf.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, EFFECT_TICKS, 0, false, false));
		if (this.plugin.getSettings().scent().slownessAmplifier() >= 0)
		{
			werewolf.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, EFFECT_TICKS, this.plugin.getSettings().scent().slownessAmplifier(), false, false));
		}
	}

	private void removeEffect(Player player, PotionEffectType type, int amplifier)
	{
		PotionEffect current = player.getPotionEffect(type);
		if (current != null && current.getAmplifier() == amplifier && current.getDuration() <= EFFECT_TICKS)
		{
			player.removePotionEffect(type);
		}
	}
}
