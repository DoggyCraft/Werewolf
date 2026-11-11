package com.dogonfire.werewolf.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleFunction;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.werewolf.config.Settings.LimitEquation;
import com.dogonfire.werewolf.config.Settings.Skin;
import com.dogonfire.werewolf.config.Settings.Stats;
import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.managers.ItemManager.ArmorPiece;
import com.dogonfire.werewolf.api.MoonPhase;

/**
 * Reads {@link Settings} from config.yml. Missing keys come from the bundled config.yml (Bukkit sets it as the
 * defaults), so the defaults live in one place. Invalid values are logged and replaced by the nearest valid value.
 */
public class SettingsLoader
{
	private static final Map<String, String>	LEGACY_EFFECT_NAMES	= Map.of(
			"jump", "jump_boost",
			"increase_damage", "strength",
			"damage_resistance", "resistance",
			"slow", "slowness",
			"fast_digging", "haste",
			"slow_digging", "mining_fatigue",
			"confusion", "nausea",
			"heal", "instant_health",
			"harm", "instant_damage");

	private static final Set<String>			DISGUISE_BACKENDS	= Set.of("auto", "builtin", "libsdisguises", "skinsrestorer");

	private final FileConfiguration				config;
	private final Logger						logger;
	private final String						mainWorldName;

	/**
	 * @param mainWorldName used when AllowedWorlds is empty
	 */
	public SettingsLoader(FileConfiguration config, Logger logger, String mainWorldName)
	{
		this.config = config;
		this.logger = logger;
		this.mainWorldName = mainWorldName;
	}

	public Settings load()
	{
		return new Settings(
				loadGeneral(),
				loadAllowedWorlds(),
				loadMoon(),
				loadWolfForm(),
				new Settings.BlockedCommands(
						strings("BlockedCommands.All"),
						strings("BlockedCommands.WolfForm"),
						strings("BlockedCommands.Alpha")),
				loadDiet(),
				new Settings.Infection(
						chance("Infection.CureChance"),
						chance("Infection.WerewolfBiteRisk"),
						chance("Infection.WerewolfBiteIntentRisk"),
						chance("Infection.WildWolfBiteRisk"),
						atLeast("Infection.AutoCureDays", 0)),
				new Settings.Maturity(
						atLeast("Maturity.Intent", 0),
						atLeast("Maturity.NoDropItems", 0),
						atLeast("Maturity.ScentTrack", 0),
						atLeast("Maturity.ControlledTransformation", 0),
						atLeast("Maturity.GoldImmunity", 0)),
				new Settings.Transformation(
						this.config.getBoolean("Transformation.Limit"),
						loadEquation(),
						this.config.getDouble("Transformation.Base"),
						this.config.getDouble("Transformation.Modifier")),
				new Settings.Cooldowns(
						atLeast("Cooldowns.Transform", 0),
						atLeast("Cooldowns.Howl", 0),
						atLeast("Cooldowns.Growl", 0),
						atLeast("Cooldowns.Sniff", 0)),
				new Settings.Scent(
						percent("Scent.Chance"),
						atLeast("Scent.Range", 0),
						this.config.getDouble("Scent.YOffset"),
						atLeast("Scent.ParticleAmount", 1),
						atLeast("Scent.Distances.Far", 0),
						atLeast("Scent.Distances.Close", 0),
						atLeast("Scent.Distances.VeryClose", 0),
						atLeast("Scent.SlownessAmplifier", -1)),
				new Settings.Hunting(
						this.config.getBoolean("Hunting.Enabled"),
						this.config.getBoolean("Hunting.Trackers.UseEitherHand"),
						this.config.getBoolean("Hunting.Target.Werewolf.WolfForm"),
						this.config.getBoolean("Hunting.Target.Werewolf.HumanForm"),
						this.config.getBoolean("Hunting.Target.Werewolf.Daytime"),
						this.config.getBoolean("Hunting.Target.Werewolf.Nighttime"),
						this.config.getBoolean("Hunting.Target.Vampire.Daytime"),
						this.config.getBoolean("Hunting.Target.Vampire.Nighttime"),
						this.config.getBoolean("Hunting.ForcePvP.Enabled"),
						this.config.getBoolean("Hunting.ForcePvP.Claims"),
						this.config.getBoolean("Hunting.ForcePvP.AdminClaims")),
				new Settings.Integrations(
						this.config.getBoolean("Integrations.WorldGuard"),
						this.config.getBoolean("Integrations.PvPManager"),
						this.config.getBoolean("Integrations.GriefPrevention")),
				loadItems(),
				new Settings.Signs(
						this.config.getBoolean("Signs.Enabled"),
						this.config.getDouble("Signs.CurePrice"),
						this.config.getDouble("Signs.InfectionPrice"),
						this.config.getDouble("Signs.WolfbanePrice"),
						this.config.getDouble("Signs.SilverSwordPrice"),
						this.config.getDouble("Signs.BookPrice")),
				new Settings.Names(
						this.config.getBoolean("WerewolfNames.Enabled"),
						strings("WerewolfNames.Given_names"),
						strings("WerewolfNames.Surnames")),
				loadClans());
	}

	private Settings.General loadGeneral()
	{
		String backend = this.config.getString("Disguises.Backend", "auto").toLowerCase(Locale.ROOT);
		if (!DISGUISE_BACKENDS.contains(backend))
		{
			warn("Disguises.Backend", backend, "auto");
			backend = "auto";
		}

		return new Settings.General(
				this.config.getString("Settings.ServerName"),
				this.config.getString("Settings.Language"),
				this.config.getBoolean("Settings.Debug"),
				this.config.getBoolean("Settings.OnlyTransformDuringFullMoon"),
				this.config.getBoolean("Settings.DropArmorOnTransform"),
				this.config.getBoolean("Settings.RenameWerewolves"),
				this.config.getBoolean("Settings.WerewolfUrges"),
				this.config.getBoolean("Settings.WolfChat"),
				this.config.getBoolean("Settings.CureWerewolfWhenSlain"),
				this.config.getBoolean("Settings.AutoBounty"),
				atLeast("Settings.AutoBountyMaximum", 0),
				this.config.getBoolean("WerewolfGroup.Enabled"),
				this.config.getString("WerewolfGroup.Name"),
				this.config.getBoolean("Trophies.Enabled"),
				this.config.getBoolean("Scoreboards.Enabled"),
				this.config.getString("Chat.Prefix"),
				backend);
	}

	// So the notice is only logged once, not on every /ww reload
	private static volatile boolean	allowedWorldsNoticeLogged	= false;

	private List<String> loadAllowedWorlds()
	{
		List<String> worlds = strings("AllowedWorlds");
		if (worlds.isEmpty())
		{
			if (!allowedWorldsNoticeLogged)
			{
				allowedWorldsNoticeLogged = true;
				this.logger.info("AllowedWorlds is empty, so werewolves are only allowed in the main world (" + this.mainWorldName + ").");
			}
			return List.of(this.mainWorldName);
		}

		for (String worldName : worlds)
		{
			this.logger.info("Werewolves are allowed in worlds '" + worldName + "'");
		}
		return worlds;
	}

	private Settings.Moon loadMoon()
	{
		int start = range("Night.Start", 0, 24000);
		int end = range("Night.End", 0, 24000);
		if (start >= end)
		{
			this.logger.warning("Night.Start (" + start + ") is not before Night.End (" + end + "), so it is never night for werewolves.");
		}

		Map<MoonPhase, String> names = new EnumMap<MoonPhase, String>(MoonPhase.class);
		for (MoonPhase phase : MoonPhase.values())
		{
			names.put(phase, this.config.getString("Moon.PhaseNames." + phase.name(), phase.getDefaultName()));
		}

		return new Settings.Moon(start, end, Collections.unmodifiableMap(names));
	}

	/**
	 * Per-clan stats not set in the file fall back to the global Werewolf.HandDamage/ItemDamage/ArmorMultiplier (alphas:
	 * double damage, half the damage taken), so configs from before per-clan stats keep their tuning.
	 */
	private Settings.WolfForm loadWolfForm()
	{
		double handDamage = this.config.getDouble("Werewolf.HandDamage");
		double itemDamage = this.config.getDouble("Werewolf.ItemDamage");
		double armorMultiplier = atLeast("Werewolf.ArmorMultiplier", 0.0D);

		Map<Clan, Stats> stats = new EnumMap<Clan, Stats>(Clan.class);
		Map<Clan, Stats> alphaStats = new EnumMap<Clan, Stats>(Clan.class);
		Map<Clan, List<PotionEffect>> effects = new EnumMap<Clan, List<PotionEffect>>(Clan.class);

		for (Clan clan : Clan.values())
		{
			String path = "Werewolf.Stats." + clan.name() + ".";
			Stats clanStats = new Stats(
					speed(path + "Speed"),
					setOr(path + "Defense", armorMultiplier),
					setOr(path + "FistDamage", handDamage),
					setOr(path + "ItemDamage", itemDamage));
			stats.put(clan, clanStats);

			alphaStats.put(clan, new Stats(
					this.config.isSet(path + "Alpha.Speed") ? speed(path + "Alpha.Speed") : clanStats.speed(),
					setOr(path + "Alpha.Defense", armorMultiplier / 2.0D),
					setOr(path + "Alpha.FistDamage", handDamage * 2.0D),
					setOr(path + "Alpha.ItemDamage", itemDamage * 2.0D)));

			String effectsPath = "Werewolf.Effects." + clan.name();
			List<PotionEffect> clanEffects = new ArrayList<PotionEffect>();
			for (String entry : this.config.getStringList(effectsPath))
			{
				PotionEffect effect = parseEffect(entry, effectsPath);
				if (effect != null)
				{
					clanEffects.add(effect);
				}
			}
			effects.put(clan, List.copyOf(clanEffects));
		}

		Set<PotionEffectType> blockedPotions = new HashSet<PotionEffectType>();
		for (String name : this.config.getStringList("Werewolf.BlockedPotions"))
		{
			PotionEffectType type = matchEffectType(name);
			if (type == null)
			{
				this.logger.warning("Invalid potion effect '" + name + "' in Werewolf.BlockedPotions, ignoring it");
				continue;
			}
			blockedPotions.add(type);
		}

		return new Settings.WolfForm(
				handDamage,
				itemDamage,
				armorMultiplier,
				atLeast("Werewolf.WolfDistance", 0),
				Collections.unmodifiableMap(stats),
				Collections.unmodifiableMap(alphaStats),
				Collections.unmodifiableMap(effects),
				Collections.unmodifiableSet(blockedPotions),
				mobTable("Werewolf.KillHealing", value -> (int) value));
	}

	private Settings.Diet loadDiet()
	{
		Set<Material> blockedFoods = new HashSet<Material>();
		for (String name : this.config.getStringList("Diet.BlockedFoods"))
		{
			Material material = Material.matchMaterial(name);
			if (material == null)
			{
				this.logger.warning("Invalid material '" + name + "' in Diet.BlockedFoods, ignoring it");
				continue;
			}
			blockedFoods.add(material);
		}

		return new Settings.Diet(
				this.config.getBoolean("Diet.Enabled"),
				this.config.getBoolean("Diet.PreventConsumption"),
				this.config.getBoolean("Diet.HumanForm"),
				Collections.unmodifiableSet(blockedFoods));
	}

	private LimitEquation loadEquation()
	{
		String name = this.config.getString("Transformation.Equation", "LINEAR");
		try
		{
			return LimitEquation.valueOf(name.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex)
		{
			warn("Transformation.Equation", name, "CONSTANT");
			return LimitEquation.CONSTANT;
		}
	}

	private Settings.Items loadItems()
	{
		Map<ArmorPiece, Integer> purity = new EnumMap<ArmorPiece, Integer>(ArmorPiece.class);
		Map<ArmorPiece, Integer> defense = new EnumMap<ArmorPiece, Integer>(ArmorPiece.class);
		for (ArmorPiece piece : ArmorPiece.values())
		{
			String path = "Items.SilverArmor." + piece.getConfigName() + ".";
			purity.put(piece, percent(path + "Purity"));
			defense.put(piece, percent(path + "Defense"));
		}

		return new Settings.Items(
				atLeast("Items.SilverSwordMultiplier", 0),
				atLeast("Items.SilverSwordPenalty", 0.0D),
				this.config.getBoolean("Items.SilverSwordBypassesDefense"),
				percent("Items.WolfbaneUntransformChance"),
				this.config.getBoolean("Items.CraftableSilverSword"),
				this.config.getBoolean("Items.CraftableSilverArmor"),
				this.config.getBoolean("Items.CraftableLoreBookEnabled"),
				this.config.getBoolean("Items.CraftableInfectionPotionEnabled"),
				this.config.getBoolean("Items.CraftableCurePotionEnabled"),
				this.config.getBoolean("Items.CraftableWolfbanePotionEnabled"),
				this.config.getBoolean("Items.CraftableWerewolfTracker"),
				this.config.getBoolean("Items.CraftableVampireTracker"),
				Collections.unmodifiableMap(purity),
				Collections.unmodifiableMap(defense));
	}

	private Settings.Clans loadClans()
	{
		Skin alphaSkin = loadSkin("Clans.AlphaSkin");
		Map<Clan, String> names = new EnumMap<Clan, String>(Clan.class);
		Map<Clan, Skin> skins = new EnumMap<Clan, Skin>(Clan.class);
		Map<Clan, Skin> alphaSkins = new EnumMap<Clan, Skin>(Clan.class);
		for (Clan clan : Clan.values())
		{
			names.put(clan, this.config.getString("Clans." + clan.name() + "Name"));
			skins.put(clan, loadSkin("Clans.Skins." + clan.name()));
			alphaSkins.put(clan, loadAlphaSkin("Clans.Skins." + clan.name() + ".Alpha", alphaSkin));
		}

		return new Settings.Clans(
				this.config.getBoolean("Clans.Enabled"),
				Collections.unmodifiableMap(names),
				alphaSkin,
				Collections.unmodifiableMap(skins),
				Collections.unmodifiableMap(alphaSkins),
				mobTable("Clans.MobPoints", value -> value));
	}

	/** @param path a section with the Value and Signature of a skin */
	private Skin loadSkin(String path)
	{
		return new Skin(this.config.getString(path + ".Value"), this.config.getString(path + ".Signature"));
	}

	// A clan's own alpha skin, or the shared one when it isn't set (backends need both the texture and its signature)
	private Skin loadAlphaSkin(String path, Skin shared)
	{
		Skin skin = loadSkin(path);
		boolean hasValue = skin.value() != null && !skin.value().isBlank();
		boolean hasSignature = skin.signature() != null && !skin.signature().isBlank();
		if (hasValue && hasSignature)
		{
			return skin;
		}
		if (hasValue || hasSignature)
		{
			this.logger.warning(path + " in config.yml needs both Value and Signature, using Clans.AlphaSkin");
		}
		return shared;
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
			this.logger.warning("Invalid effect '" + entry + "' in " + path + ", expected NAME:amplifier:duration");
			return null;
		}

		PotionEffectType type = matchEffectType(parts[0]);
		if (type == null)
		{
			this.logger.warning("Unknown potion effect '" + parts[0] + "' in " + path + ", ignoring it");
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
			this.logger.warning("Invalid amplifier or duration in effect '" + entry + "' in " + path + ", ignoring it");
			return null;
		}
	}

	/** Accepts Minecraft ids (speed, minecraft:speed) and the old Bukkit names (JUMP, INCREASE_DAMAGE, ...) */
	public static PotionEffectType matchEffectType(String name)
	{
		if (name == null)
		{
			return null;
		}

		String key = name.trim().toLowerCase(Locale.ROOT);
		key = LEGACY_EFFECT_NAMES.getOrDefault(key, key);

		NamespacedKey namespacedKey = NamespacedKey.fromString(key);
		if (namespacedKey == null)
		{
			return null;
		}

		return Registry.EFFECT.get(namespacedKey);
	}

	/**
	 * A section of EntityType names to values, plus an optional 'default' entry for mobs not listed (0 without one).
	 * Unknown mobs and non-numbers are logged and skipped; negative values become 0.
	 */
	private <N extends Number> Settings.MobTable<N> mobTable(String path, DoubleFunction<N> convert)
	{
		Map<EntityType, N> values = new EnumMap<EntityType, N>(EntityType.class);
		N defaultValue = convert.apply(0.0D);
		ConfigurationSection section = section(path);
		if (section == null)
		{
			this.logger.warning(path + " in config.yml is not a section of mob: value entries, so no mob gives anything");
			return new Settings.MobTable<N>(Collections.unmodifiableMap(values), defaultValue);
		}

		List<String> unknown = new ArrayList<String>();
		for (String key : section.getKeys(false))
		{
			if (!(section.get(key) instanceof Number number))
			{
				this.logger.warning("Invalid value '" + section.get(key) + "' for " + path + "." + key + " in config.yml, ignoring it");
				continue;
			}

			double value = number.doubleValue();
			if (value < 0.0D)
			{
				warn(path + "." + key, value, 0);
				value = 0.0D;
			}

			if (key.equalsIgnoreCase("default"))
			{
				defaultValue = convert.apply(value);
				continue;
			}

			EntityType type = matchEntityType(key);
			if (type == null)
			{
				unknown.add(key);
				continue;
			}
			values.put(type, convert.apply(value));
		}

		// The bundled lists have mobs from the newest Minecraft, so older servers don't know some: one line, not one each
		if (!unknown.isEmpty())
		{
			this.logger.warning("Ignoring mobs in " + path + " in config.yml that this server doesn't know: " + String.join(", ", unknown));
		}

		return new Settings.MobTable<N>(Collections.unmodifiableMap(values), defaultValue);
	}

	/** Accepts EntityType names (ZOMBIE_HORSE) and Minecraft ids (zombie_horse, minecraft:zombie_horse) */
	public static EntityType matchEntityType(String name)
	{
		String key = name.trim().toUpperCase(Locale.ROOT);
		if (key.startsWith("MINECRAFT:"))
		{
			key = key.substring("MINECRAFT:".length());
		}

		if (key.equals("UNKNOWN"))
		{
			return null;
		}

		try
		{
			return EntityType.valueOf(key);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	/**
	 * The section at path in config.yml, or the bundled one when the file doesn't have it at all (e.g. an upgraded
	 * config). Bukkit's getConfigurationSection would return an empty section in that case. A section the file does
	 * have is used as it is: entries the admin removed are not filled in from the bundled one.
	 */
	private ConfigurationSection section(String path)
	{
		if (this.config.isSet(path))
		{
			return this.config.getConfigurationSection(path);
		}

		Configuration defaults = this.config.getDefaults();
		return defaults != null ? defaults.getConfigurationSection(path) : null;
	}

	private List<String> strings(String path)
	{
		return List.copyOf(this.config.getStringList(path));
	}

	/** The value at path if the file sets it, otherwise fallback (instead of the bundled default) */
	private double setOr(String path, double fallback)
	{
		double value = this.config.isSet(path) ? this.config.getDouble(path) : fallback;
		if (value < 0.0D)
		{
			warn(path, value, 0.0D);
			return 0.0D;
		}
		return value;
	}

	private double speed(String path)
	{
		double value = this.config.getDouble(path);
		if (value < -1.0D || value > 1.0D)
		{
			double clamped = Math.max(-1.0D, Math.min(1.0D, value));
			warn(path, value, clamped);
			return clamped;
		}
		return value;
	}

	private double chance(String path)
	{
		double value = this.config.getDouble(path);
		if (value < 0.0D || value > 1.0D)
		{
			double clamped = Math.max(0.0D, Math.min(1.0D, value));
			warn(path, value, clamped);
			return clamped;
		}
		return value;
	}

	private int percent(String path)
	{
		return range(path, 0, 100);
	}

	private int range(String path, int min, int max)
	{
		int value = this.config.getInt(path);
		if (value < min || value > max)
		{
			int clamped = Math.max(min, Math.min(max, value));
			warn(path, value, clamped);
			return clamped;
		}
		return value;
	}

	private int atLeast(String path, int min)
	{
		return range(path, min, Integer.MAX_VALUE);
	}

	private double atLeast(String path, double min)
	{
		double value = this.config.getDouble(path);
		if (value < min)
		{
			warn(path, value, min);
			return min;
		}
		return value;
	}

	private void warn(String path, Object value, Object used)
	{
		this.logger.warning("Invalid value '" + value + "' for " + path + " in config.yml, using " + used);
	}
}
