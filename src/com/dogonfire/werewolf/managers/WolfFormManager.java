package com.dogonfire.werewolf.managers;

import com.dogonfire.werewolf.api.Clan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.config.Settings.Stats;

/**
 * Applies the per-clan wolf form stats and potion effects, and checks blocked potions and the diet (all from
 * {@link com.dogonfire.werewolf.config.Settings}).
 */
public class WolfFormManager
{
	// Effects running lower than this are topped up by the update loop (it runs every 100 ticks)
	private static final int						REFRESH_BELOW_TICKS	= 400;

	private final Werewolf							plugin;
	private final WerewolfContext ctx;
	private final Map<UUID, List<PotionEffect>>		appliedEffects		= new HashMap<UUID, List<PotionEffect>>();

	public WolfFormManager(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	public Stats getStats(UUID playerId)
	{
		Clan clan = ctx.getWerewolfManager().getWerewolfClan(playerId);
		Map<Clan, Stats> stats = ctx.getClanManager().isAlpha(playerId) ? this.plugin.getSettings().wolfForm().alphaStats() : this.plugin.getSettings().wolfForm().stats();
		return stats.get(clan);
	}

	public boolean isBlockedPotion(Player player, PotionEffectType type)
	{
		if (!this.plugin.getSettings().wolfForm().blockedPotions().contains(type))
		{
			return false;
		}

		for (PotionEffect effect : getClanEffects(player.getUniqueId()))
		{
			if (effect.getType().equals(type))
			{
				return false;
			}
		}

		return true;
	}

	public boolean isBlockedFood(Material material)
	{
		return this.plugin.getSettings().diet().blockedFoods().contains(material);
	}

	private List<PotionEffect> getClanEffects(UUID playerId)
	{
		List<PotionEffect> effects = this.plugin.getSettings().wolfForm().effects().get(ctx.getWerewolfManager().getWerewolfClan(playerId));
		return effects != null ? effects : new ArrayList<PotionEffect>();
	}

	// Configured effects are shown without particles, and no longer than the client can display
	private static void addWithoutParticles(Player player, PotionEffect effect)
	{
		int duration = Math.min(effect.getDuration(), Short.MAX_VALUE);
		player.addPotionEffect(new PotionEffect(effect.getType(), duration, effect.getAmplifier(), effect.isAmbient(), false));
	}

	/** Gives a player who just turned into wolf form their clan's walk speed and effects. */
	public void applyWolfForm(Player player)
	{
		List<PotionEffect> effects = getClanEffects(player.getUniqueId());
		this.appliedEffects.put(player.getUniqueId(), effects);

		for (PotionEffect effect : effects)
		{
			addWithoutParticles(player, effect);
		}

		refreshWolfForm(player);
	}

	/** Keeps a werewolf's speed and effects up while in wolf form, and strips blocked potions. Runs from the update loop. */
	public void refreshWolfForm(Player player)
	{
		Stats stats = getStats(player.getUniqueId());
		if (stats != null)
		{
			float speed = (float) Math.max(-1.0D, Math.min(1.0D, stats.speed()));
			if (player.getWalkSpeed() != speed)
			{
				player.setWalkSpeed(speed);
			}
		}

		List<PotionEffect> effects = this.appliedEffects.get(player.getUniqueId());
		if (effects == null)
		{
			effects = getClanEffects(player.getUniqueId());
			this.appliedEffects.put(player.getUniqueId(), effects);
		}

		for (PotionEffect effect : effects)
		{
			PotionEffect current = player.getPotionEffect(effect.getType());
			if (current == null || current.getAmplifier() < effect.getAmplifier()
					|| (current.getAmplifier() == effect.getAmplifier() && !current.isInfinite() && current.getDuration() < REFRESH_BELOW_TICKS))
			{
				addWithoutParticles(player, effect);
			}
		}

		for (PotionEffectType type : this.plugin.getSettings().wolfForm().blockedPotions())
		{
			if (player.hasPotionEffect(type) && isBlockedPotion(player, type))
			{
				player.removePotionEffect(type);
			}
		}
	}

	/**
	 * Removes the wolf form effects and walk speed. Only effects of the configured types at the configured strength are
	 * removed, so potions the player drank themselves at another strength are kept.
	 */
	public void removeWolfForm(Player player, boolean fallbackToClanEffects)
	{
		List<PotionEffect> effects = this.appliedEffects.remove(player.getUniqueId());
		if (effects == null)
		{
			// Nothing recorded (e.g. the server restarted while they were in wolf form): fall back to their clan's list
			boolean useClan = fallbackToClanEffects && ctx.getWerewolfManager().isWerewolf(player.getUniqueId());
			effects = useClan ? getClanEffects(player.getUniqueId()) : new ArrayList<PotionEffect>();
		}

		for (PotionEffect effect : effects)
		{
			PotionEffect current = player.getPotionEffect(effect.getType());
			if (current != null && current.getAmplifier() == effect.getAmplifier())
			{
				player.removePotionEffect(effect.getType());
			}
		}

		player.setWalkSpeed(0.2F);
	}
}
