package com.dogonfire.werewolf.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.werewolf.managers.ClanManager.ClanType;
import com.dogonfire.werewolf.managers.ItemManager.ArmorPiece;
import com.dogonfire.werewolf.managers.MoonManager.MoonPhase;

/**
 * Everything read from config.yml, validated. Built by {@link SettingsLoader} on enable and on /ww reload; never
 * changed afterwards. Each record matches a section of config.yml (see src/resources/config.yml for the keys).
 */
public record Settings(
		General general,
		List<String> allowedWorlds,
		Moon moon,
		WolfForm wolfForm,
		BlockedCommands blockedCommands,
		Diet diet,
		Infection infection,
		Maturity maturity,
		Transformation transformation,
		Cooldowns cooldowns,
		Scent scent,
		Hunting hunting,
		Integrations integrations,
		Items items,
		Signs signs,
		Names names,
		Clans clans)
{
	/** Settings.*, plus the small single-key sections (WerewolfGroup, Trophies, Scoreboards, Chat, Disguises) */
	public record General(
			String serverName,
			String language,
			boolean debug,
			boolean onlyTransformDuringFullMoon,
			boolean dropArmorOnTransform,
			boolean renameWerewolves,
			boolean werewolfUrges,
			boolean wolfChat,
			boolean cureWerewolfWhenSlain,
			boolean autoBounty,
			int autoBountyMaximum,
			boolean werewolfGroupEnabled,
			String werewolfGroupName,
			boolean trophiesEnabled,
			boolean scoreboardsEnabled,
			String chatPrefix,
			String disguiseBackend)
	{
	}

	/** Night.* and Moon.* */
	public record Moon(int nightStart, int nightEnd, Map<MoonPhase, String> phaseNames)
	{
	}

	public record Stats(double speed, double defense, double fistDamage, double itemDamage)
	{
	}

	/** Werewolf.*: global damage values, per-clan stats and effects, blocked potions */
	public record WolfForm(
			double handDamage,
			double itemDamage,
			double armorMultiplier,
			int wolfDistance,
			Map<ClanType, Stats> stats,
			Map<ClanType, Stats> alphaStats,
			Map<ClanType, List<PotionEffect>> effects,
			Set<PotionEffectType> blockedPotions)
	{
	}

	public record BlockedCommands(List<String> all, List<String> wolfForm, List<String> alpha)
	{
	}

	public record Diet(boolean enabled, boolean preventConsumption, boolean humanForm, Set<Material> blockedFoods)
	{
	}

	public record Infection(double cureChance, double werewolfBiteRisk, double werewolfBiteIntentRisk, double wildWolfBiteRisk, int autoCureDays)
	{
	}

	/** Number of transformations needed for each perk */
	public record Maturity(int intent, int noDropItems, int scentTrack, int controlledTransformation, int goldImmunity)
	{
	}

	public enum LimitEquation
	{
		LINEAR, EXPONENTIAL, CONSTANT
	}

	public record Transformation(boolean limit, LimitEquation equation, double base, double modifier)
	{
	}

	/** In seconds */
	public record Cooldowns(int transform, int howl, int growl, int sniff)
	{
	}

	public record Scent(int chance, int range, double yOffset, int particleAmount, int distanceFar, int distanceClose, int distanceVeryClose, int slownessAmplifier)
	{
	}

	public record Hunting(
			boolean enabled,
			boolean trackerUseEitherHand,
			boolean trackWerewolfWolfForm,
			boolean trackWerewolfHumanForm,
			boolean trackWerewolfDaytime,
			boolean trackWerewolfNighttime,
			boolean trackVampireDaytime,
			boolean trackVampireNighttime,
			boolean forcePvPEnabled,
			boolean forcePvPClaims,
			boolean forcePvPAdminClaims)
	{
	}

	public record Integrations(boolean worldGuard, boolean pvpManager, boolean griefPrevention)
	{
	}

	public record Items(
			int silverSwordMultiplier,
			double silverSwordPenalty,
			boolean silverSwordBypassesDefense,
			int wolfbaneUntransformChance,
			boolean craftableSilverSword,
			boolean craftableSilverArmor,
			boolean craftableLoreBook,
			boolean craftableInfectionPotion,
			boolean craftableCurePotion,
			boolean craftableWolfbanePotion,
			boolean craftableWerewolfTracker,
			boolean craftableVampireTracker,
			Map<ArmorPiece, Integer> silverArmorPurity,
			Map<ArmorPiece, Integer> silverArmorDefense)
	{
	}

	public record Signs(boolean enabled, double curePrice, double infectionPrice, double wolfbanePrice, double silverSwordPrice, double bookPrice)
	{
	}

	/** WerewolfNames.* */
	public record Names(boolean enabled, List<String> givenNames, List<String> surnames)
	{
	}

	/** The Minecraft account a werewolf skin comes from, and its texture */
	public record Skin(String accountName, UUID accountId, String value, String signature)
	{
	}

	public record Clans(boolean enabled, Map<ClanType, String> names, Skin alphaSkin, Map<ClanType, Skin> skins)
	{
	}
}
