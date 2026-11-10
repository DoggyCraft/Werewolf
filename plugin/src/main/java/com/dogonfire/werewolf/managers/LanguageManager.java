package com.dogonfire.werewolf.managers;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LanguageManager
{
	private final Werewolf plugin;
	private final WerewolfContext ctx;
	private FileConfiguration	languageConfig	= null;
	private Random				random			= new Random();
	private String				amount;
	private String				playerName;
	private String				type;
	private String				authorName;
	private String				languageName;

	private YamlConfiguration loadBundledLanguage(String fileName)
	{
		InputStream stream = this.plugin.getResource("lang/" + fileName);
		if (stream == null)
		{
			return null;
		}
		return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
	}

	/**
	 * Loads plugins/Werewolf/lang/<language>.yml, copying the bundled file there first if it's missing.
	 * Keys missing from that file fall back to the bundled file for the language, then to bundled english.
	 */
	public void load()
	{
		String languageFileName = this.plugin.getSettings().general().language() + ".yml";

		YamlConfiguration english = loadBundledLanguage("english.yml");
		YamlConfiguration bundled = loadBundledLanguage(languageFileName);
		if (bundled != null && english != null && bundled != english)
		{
			bundled.setDefaults(english);
		}

		File languageConfigFile = new File(this.plugin.getDataFolder(), "lang/" + languageFileName);
		if (!languageConfigFile.exists() && bundled != null)
		{
			this.plugin.saveResource("lang/" + languageFileName, false);
		}

		if (languageConfigFile.exists())
		{
			this.languageConfig = new YamlConfiguration();
			try
			{
				this.languageConfig.load(languageConfigFile);
			}
			catch (Exception e)
			{
				this.plugin.log("Could not load data from " + languageConfigFile + ": " + e.getMessage());
			}
			this.languageConfig.setDefaults(bundled != null ? bundled : english);
		}
		else
		{
			this.plugin.log("Language '" + this.plugin.getSettings().general().language() + "' was not found. Using english.");
			this.languageConfig = english;
		}

		this.languageName = this.languageConfig.getString("Version.Name");
		this.authorName = this.languageConfig.getString("Version.Author");

		this.plugin.logDebug("Loaded " + this.languageName + " by " + this.authorName + " version " + this.languageConfig.getString("Version.Version"));
	}

	public String getLanguageString(LANGUAGESTRING type, ChatColor color)
	{
		List<String> strings = this.languageConfig.getStringList(type.name());
		if (strings.size() == 0)
		{
			this.plugin.log("No language strings found for " + type.name() + "!");
			return type.name() + " MISSING";
		}
		String text = (String) strings.toArray()[this.random.nextInt(strings.size())];

		return parseString(text, color);
	}

	public LanguageManager(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	public String getAuthor()
	{
		return this.authorName;
	}


	public String parseString(String id, ChatColor color)
	{
		String string = color + id.replaceAll("&([0-9a-f])", "§$1");

		if (string.contains("$ServerName"))
		{
			string = string.replace("$ServerName", ChatColor.GOLD + this.plugin.getSettings().general().serverName() + color);
		}
		if (string.contains("$PlayerName"))
		{
			string = string.replace("$PlayerName", ChatColor.GOLD + this.playerName + color);
		}
		if (string.contains("$Amount"))
		{
			string = string.replace("$Amount", ChatColor.GOLD + this.amount + color);
		}
		if (string.contains("$Type"))
		{
			string = string.replace("$Type", ChatColor.GOLD + this.type + color);
		}
		return string;
	}


	public String getPlayerName()
	{
		return this.playerName;
	}

	public void setPlayerName(String name)
	{
		if (name == null)
		{
			this.plugin.logDebug("WARNING: Setting null playername");
		}
		this.playerName = name;
	}

	public String getAmount()
	{
		return this.amount;
	}

	public void setAmount(String amount)
	{
		this.amount = amount;
	}

	public String getType()
	{
		return this.type;
	}

	public void setType(String t)
	{
		if (t == null)
		{
			this.plugin.logDebug("WARNING: Setting null type");
		}
		this.type = t;
	}

	public static enum LANGUAGESTRING
	{
		PlayerIsAFullWerewolf,
		PlayerIsAnInfectedWerewolf,
		PlayerIsNotAWerewolf,
		YouTeleportedToClanHome,
		YouCutTheHeadOfWerewolf,
		FullMoonIsRising,
		FullMoonNumberOfWerewolves,
		YouBoughtAInfectionPotion,
		YouBoughtACurePotion,
		YouBoughtAWolfbanePotion,
		YouBoughtASilverSword,
		YouBoughtALoreBook,
		YouDoNotHaveEnoughMoney,
		YouPlacedAInfectionPotionSign,
		YouPlacedACurePotionSign,
		YouPlacedAWolfbanePotionSign,
		YouPlacedASilverSwordSign,
		YouPlacedABookSign,
		YouAreNotTheAlphaOfTheClan,
		YourWerewolfName,
		DenyVampire,
		NoRecentAlphaCall,
		ClanAlphaCannotAnswerCall,
		YouCalledClanMember,
		YouAreBeingCalled,
		NewClanAlpha,
		CannotTransformSoSoon,
		WerewolfCommandControlledTransformation,
		WerewolfCommandGoldImmunity,
		WerewolfCommandNoDropItems,
		WerewolfCommandFullMoonImmunity,
		KilledMobPoints,
		MustExperienceMoreFullMoons,
		NoWerewolfHuntersInWorld,
		TopWerewolfHunters,
		CreatedWerewolfInfectionPotion,
		CreatedWerewolfCurePotion,
		CreatedWerewolfWolfbanePotion,
		CreatedWerewolfSilverSword,
		CreatedWerewolfLoreBook,
		NotAWerewolfAnymore,
		NowAWerewolf,
		InvalidCommand,
		HelpCall,
		HelpWerewolf,
		HelpCheck,
		HelpTop,
		HelpHunt,
		HelpBounty,
		HelpAddBounty,
		HelpInfectSelf,
		HelpInfectOther,
		HelpToggleSelf,
		HelpToggleOther,
		HelpInfectionPotion,
		HelpCurePotion,
		HelpWolfbanePotion,
		HelpSilverSword,
		HelpLoreBook,
		HelpCure,
		HelpTransform,
		HelpClan,
		HelpSetHome,
		HelpHome,
		ClanSetHome,
		MadeWerewolf,
		UnMadeWerewolf,
		Infected,
		PlayerIsHuntingWerewolves,
		PlayerIsNoLongerHuntingWerewolves,
		TakeCompassInHands,
		YouMustHaveYourHandsFree,
		NoPermissionForCommand,
		NoWerewolvesOnline,
		NoWerewolvesInThisWorld,
		WerewolvesCannotHuntWerewolves,
		WerewolfCommandNumberOfWerewolves,
		WerewolfCommandIsWerewolfAlpha,
		WerewolfCommandIsWerewolf,
		WerewolfCommandIsNotWerewolf,
		WerewolfCommandNextFullMoon,
		WerewolfCommandListCommands,
		Transform,
		Untransform,
		BiteVictim,
		BiteAttacker,
		BiteWildWolf,
		KilledMob,
		WerewolfTryEat,
		WerewolfTryDefense,
		DrinkCureSuccess,
		DrinkCureFailure,
		DrinkInfectionSuccess,
		DrinkInfectionFailure,
		PotionCreated,
		UrgesInfectedThisNight,
		UrgesInside,
		UrgesInfectedHumanForm,
		UrgesTransformed,
		InfectionPotionTitle,
		InfectionPotionDescription1,
		InfectionPotionDescription2,
		CurePotionTitle,
		CurePotionDescription1,
		CurePotionDescription2,
		WolfbanePotionTitle,
		WolfbanePotionDescription1,
		WolfbanePotionDescription2,
		SilverSwordTitle,
		SilverSwordDescription1,
		SilverSwordDescription2,
		SilverArmorTitle,
		SilverArmorDescription1,
		SilverArmorDescription2,
		LoreBookTitle,
		BountyPlayerAdded,
		BountyServerAdded,
		BountyTotal,
		KilledWerewolfBounty,
		KilledWerewolfNoBounty,
		TrophyDescription,
		InfoCommandHowl,
		InfoCommandGrowl,
		CurrentBounty,
		Today,
		Tomorrow,
		In2Days,
		In3Days,
		In4Days,
		In5Days,
		In6Days,
		In7Days,
		WerewolfCommandMoonPhase,
		WerewolfCommandIntentOn,
		WerewolfCommandIntentOff,
		CannotControlTransformationTonight,
		TransformationTimeLimit,
		TransformationTimeExpired,
		FullMoonLevelGained,
		AbilityOnCooldown,
		IntentEnabled,
		IntentDisabled,
		InvalidMoonPhase,
		MoonPhaseSet,
		HelpIntent,
		HelpSetPhase,
		BlockedCommand,
		BlockedCommandWolfForm,
		BlockedCommandAlpha,
		CannotTransformInGameMode,
		MoonPullBlockedByGameMode,
		PlayerCannotTransformTonight,
		SilverArmorPurity,
		SilverArmorDefense,
		SilverArmorHumansOnly,
		SilverArmorCannotWear,
		SilverArmorCleansed,
		SilverBurnsWerewolf,
		WolfbaneHumanForm,
		WerewolfTrackerTitle,
		VampireTrackerTitle,
		WerewolfTrackerDescription,
		VampireTrackerDescription,
		TrackerClearDescription,
		TrackerHumansOnly,
		TrackerAlreadyTracking,
		TrackerFound,
		TrackerNoTargets,
		TrackerNoTarget,
		TrackerCleared,
		TrackerLost,
		TrackerActionBar,
		TrackerOnlyHumans,
		TrackerOnlyHumansCraft,
		HuntingDisabled,
		ScentFound,
		ScentTry,
		ScentLost,
		ScentTrackStarted,
		ScentTrackStopped,
		ScentTrackNotInWolfForm,
		ScentNoTarget,
		ScentTargetNotFound,
		ScentTargetOtherWorld,
		CreatedWerewolfTracker,
		CreatedSilverArmor,
		HelpTrack,
		HelpTracker,
		HelpSilverArmor,
		ForcePvPTrespassing,
		ForcePvPCannotDisable,
		AlphaSlainByHunter,
		WerewolfResistsVampirism,
		HelpSetLevel,
		HelpAddLevel,
		HelpSetAlpha,
		HelpCureOther,
		AdminLevelSet,
		AdminAlphaSet,
		AdminCured,
		AdminNotAWerewolf,
		AdminUnknownPlayer,
		AdminInvalidNumber,
		AdminInvalidClan,
		AdminChangeCancelled,
		ClanHomeNotSet,
		EconomyUnavailable;
	}
}