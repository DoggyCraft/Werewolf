package com.dogonfire.werewolf.managers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.werewolf.Werewolf;

/**
 * Per-clan wolf form stats, potion effects, blocked potions and diet, parsed from the config fields on {@link Werewolf}.
 */
public class WolfFormManager
{
	public static class Stats
	{
		public double	speed;
		public double	defense;
		public double	fistDamage;
		public double	itemDamage;

		public Stats(double speed, double defense, double fistDamage, double itemDamage)
		{
			this.speed = speed;
			this.defense = defense;
			this.fistDamage = fistDamage;
			this.itemDamage = itemDamage;
		}
	}

	private static final Map<String, String>					LEGACY_EFFECT_NAMES	= new HashMap<String, String>();
	// Effects running lower than this are topped up by the update loop (it runs every 100 ticks)
	private static final int									REFRESH_BELOW_TICKS	= 400;

	static
	{
		LEGACY_EFFECT_NAMES.put("jump", "jump_boost");
		LEGACY_EFFECT_NAMES.put("increase_damage", "strength");
		LEGACY_EFFECT_NAMES.put("damage_resistance", "resistance");
		LEGACY_EFFECT_NAMES.put("slow", "slowness");
		LEGACY_EFFECT_NAMES.put("fast_digging", "haste");
		LEGACY_EFFECT_NAMES.put("slow_digging", "mining_fatigue");
		LEGACY_EFFECT_NAMES.put("confusion", "nausea");
		LEGACY_EFFECT_NAMES.put("heal", "instant_health");
		LEGACY_EFFECT_NAMES.put("harm", "instant_damage");
	}

	private final Werewolf										plugin;
	private final EnumMap<ClanManager.ClanType, List<PotionEffect>>	clanEffects			= new EnumMap<ClanManager.ClanType, List<PotionEffect>>(ClanManager.ClanType.class);
	private final Set<PotionEffectType>							blockedPotions		= new HashSet<PotionEffectType>();
	private final Set<Material>									blockedFoods		= new HashSet<Material>();
	private final Map<UUID, List<PotionEffect>>					appliedEffects		= new HashMap<UUID, List<PotionEffect>>();

	public WolfFormManager(Werewolf plugin)
	{
		this.plugin = plugin;
	}

	/** Parses the effect, potion and food lists. Called from loadSettings, so invalid entries are logged once per (re)load. */
	public void load()
	{
		this.clanEffects.clear();
		for (ClanManager.ClanType clan : ClanManager.ClanType.values())
		{
			List<PotionEffect> effects = new ArrayList<PotionEffect>();
			List<String> entries = this.plugin.wolfFormEffects.get(clan);
			if (entries != null)
			{
				for (String entry : entries)
				{
					PotionEffect effect = parseEffect(entry, "Werewolf.Effects." + clan.name());
					if (effect != null)
					{
						effects.add(effect);
					}
				}
			}
			this.clanEffects.put(clan, effects);
		}

		this.blockedPotions.clear();
		for (String name : this.plugin.blockedPotions)
		{
			PotionEffectType type = matchEffectType(name);
			if (type == null)
			{
				this.plugin.log("Invalid potion effect '" + name + "' in Werewolf.BlockedPotions, ignoring it");
				continue;
			}
			this.blockedPotions.add(type);
		}

		this.blockedFoods.clear();
		for (String name : this.plugin.dietBlockedFoods)
		{
			Material material = Material.matchMaterial(name);
			if (material == null)
			{
				this.plugin.log("Invalid material '" + name + "' in Diet.BlockedFoods, ignoring it");
				continue;
			}
			this.blockedFoods.add(material);
		}
	}

	private PotionEffect parseEffect(String entry, String path)
	{
		String[] parts = entry.trim().split(":");
		// Allow namespaced names such as minecraft:speed:1:600
		if (parts.length == 4)
		{
			parts = new String[] { parts[0] + ":" + parts[1], parts[2], parts[3] };
		}

		if (parts.length != 3)
		{
			this.plugin.log("Invalid effect '" + entry + "' in " + path + ", expected NAME:amplifier:duration");
			return null;
		}

		PotionEffectType type = matchEffectType(parts[0]);
		if (type == null)
		{
			this.plugin.log("Unknown potion effect '" + parts[0] + "' in " + path + ", ignoring it");
			return null;
		}

		try
		{
			int amplifier = Integer.parseInt(parts[1].trim());
			int duration = Integer.parseInt(parts[2].trim());
			if (amplifier < 0 || (duration <= 0 && duration != PotionEffect.INFINITE_DURATION))
			{
				throw new NumberFormatException();
			}
			return new PotionEffect(type, duration, amplifier, false, false);
		}
		catch (NumberFormatException ex)
		{
			this.plugin.log("Invalid amplifier or duration in effect '" + entry + "' in " + path + ", ignoring it");
			return null;
		}
	}

	public static PotionEffectType matchEffectType(String name)
	{
		if (name == null)
		{
			return null;
		}

		String key = name.trim().toLowerCase(Locale.ROOT);
		String legacy = LEGACY_EFFECT_NAMES.get(key);
		if (legacy != null)
		{
			key = legacy;
		}

		NamespacedKey namespacedKey = NamespacedKey.fromString(key);
		if (namespacedKey == null)
		{
			return null;
		}

		return Registry.EFFECT.get(namespacedKey);
	}

	public Stats getStats(UUID playerId)
	{
		ClanManager.ClanType clan = Werewolf.getWerewolfManager().getWerewolfClan(playerId);
		Map<ClanManager.ClanType, Stats> stats = Werewolf.getClanManager().isAlpha(playerId) ? this.plugin.alphaStats : this.plugin.werewolfStats;
		return stats.get(clan);
	}

	public boolean isBlockedPotion(Player player, PotionEffectType type)
	{
		if (!this.blockedPotions.contains(type))
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
		return this.blockedFoods.contains(material);
	}

	private List<PotionEffect> getClanEffects(UUID playerId)
	{
		List<PotionEffect> effects = this.clanEffects.get(Werewolf.getWerewolfManager().getWerewolfClan(playerId));
		return effects != null ? effects : new ArrayList<PotionEffect>();
	}

	/** Gives a player who just turned into wolf form their clan's walk speed and effects. */
	public void applyWolfForm(Player player)
	{
		List<PotionEffect> effects = getClanEffects(player.getUniqueId());
		this.appliedEffects.put(player.getUniqueId(), effects);

		for (PotionEffect effect : effects)
		{
			Werewolf.pu.addPotionEffectNoGraphic(player, effect);
		}

		refreshWolfForm(player);
	}

	/** Keeps a werewolf's speed and effects up while in wolf form, and strips blocked potions. Runs from the update loop. */
	public void refreshWolfForm(Player player)
	{
		Stats stats = getStats(player.getUniqueId());
		if (stats != null)
		{
			float speed = (float) Math.max(-1.0D, Math.min(1.0D, stats.speed));
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
				Werewolf.pu.addPotionEffectNoGraphic(player, effect);
			}
		}

		for (PotionEffectType type : this.blockedPotions)
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
			boolean useClan = fallbackToClanEffects && Werewolf.getWerewolfManager().isWerewolf(player.getUniqueId());
			effects = useClan ? getClanEffects(player.getUniqueId()) : new ArrayList<PotionEffect>();
		}

		for (PotionEffect effect : effects)
		{
			PotionEffect current = player.getPotionEffect(effect.getType());
			if (current != null && current.getAmplifier() == effect.getAmplifier())
			{
				Werewolf.pu.removePotionEffectNoGraphic(player, effect.getType());
			}
		}

		player.setWalkSpeed(0.2F);
	}
}
