package com.dogonfire.werewolf.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Upgrades an old config.yml in place. config-version is missing (= 1) in every config written before 0.12, when the
 * plugin rewrote the whole file on every start. The original file is backed up to config.v&lt;version&gt;.bak first.
 * Apart from saveDefaultConfig() on first start, this is the only code that writes config.yml.
 */
public class ConfigMigrator
{
	public static final int		CURRENT_VERSION	= 4;
	public static final String	VERSION_KEY		= "config-version";

	/** Renamed keys: old path, new path */
	private static final String[][]	RENAMED_KEYS	= {
			{ "WerewolfWolf.ItemDamage", "Werewolf.ItemDamage" },
			{ "WerewolfWolf.HandDamage", "Werewolf.HandDamage" },
			{ "WerewolfWolf.ArmorMultiplier", "Werewolf.ArmorMultiplier" },
			{ "WerewolfWolf.WolfDistance", "Werewolf.WolfDistance" },
			{ "Infection.CurePotionChance", "Infection.CureChance" } };

	/** Keys no version reads any more */
	private static final List<String>	DEAD_KEYS		= List.of(
			"WerewolfWolf",
			"Pounce",
			"Skins",
			"Items.Enabled",
			"Settings.MetricsOptOut",
			"Settings.DisplayUpdateNotifications");

	/** The skin keys of Bloodmoon (WerewolfBite) and Silvermane (WildBite) */
	private static final List<String>	SKIN_KEYS		= List.of("AccountName", "AccountUUID", "SkinValue", "SkinSignature");

	/** The prefixes of the version 2 skin keys (Clans.&lt;prefix&gt;SkinValue, ...): the shared alpha skin and each clan */
	private static final List<String>	V2_SKIN_PREFIXES	= List.of("Alpha", "Potion", "WerewolfBite", "WildBite");

	private final File					file;
	private final FileConfiguration		config;
	private final Logger				logger;

	/**
	 * @param file   config.yml
	 * @param config the loaded config.yml (with the bundled defaults set)
	 */
	public ConfigMigrator(File file, FileConfiguration config, Logger logger)
	{
		this.file = file;
		this.config = config;
		this.logger = logger;
	}

	public void migrate()
	{
		if (!this.file.exists())
		{
			return;
		}

		// Bukkit loads a config.yml with a syntax error as an empty config; migrating that would wipe the admin's file
		try
		{
			new YamlConfiguration().load(this.file);
		}
		catch (IOException | InvalidConfigurationException ex)
		{
			this.logger.severe("config.yml could not be read, so the defaults are used. Fix the file and use /ww reload. It will not be changed. Cause: " + ex.getMessage());
			return;
		}

		int version = this.config.isSet(VERSION_KEY) ? this.config.getInt(VERSION_KEY) : 1;
		if (version > CURRENT_VERSION)
		{
			this.logger.warning("config.yml is version " + version + ", but this Werewolf version only knows up to version " + CURRENT_VERSION + ". Unknown settings are ignored.");
			return;
		}
		if (version == CURRENT_VERSION)
		{
			return;
		}

		boolean backedUp = backup(version);

		if (version < 2)
		{
			migrateToVersion2();
		}
		if (version < 3)
		{
			migrateToVersion3();
		}
		// Version 4 adds Werewolf.KillHealing and Clans.MobPoints. Nothing to move: a config without them uses the
		// bundled sections whole (see SettingsLoader.section)
		this.config.set(VERSION_KEY, CURRENT_VERSION);

		if (!backedUp)
		{
			return;
		}
		try
		{
			saveAtomically();
			this.logger.info("Upgraded config.yml from version " + version + " to " + CURRENT_VERSION + ".");
		}
		catch (IOException ex)
		{
			this.logger.log(Level.SEVERE, "Could not save the upgraded config.yml. The upgrade is used for now and will be retried on the next start.", ex);
		}
	}

	private void migrateToVersion2()
	{
		for (String[] renamed : RENAMED_KEYS)
		{
			if (this.config.isSet(renamed[0]) && !this.config.isSet(renamed[1]))
			{
				this.config.set(renamed[1], this.config.get(renamed[0]));
			}
		}
		this.config.set("Infection.CurePotionChance", null);

		for (String key : DEAD_KEYS)
		{
			this.config.set(key, null);
		}

		migrateSwappedClanSkins();
	}

	/**
	 * Before 0.12 Bloodmoon (WerewolfBite) used the Silvermane skin account and vice versa. Configs still holding that
	 * default pairing get the two clans' skin settings swapped.
	 */
	private void migrateSwappedClanSkins()
	{
		if (!"SM_Werewolf".equals(this.config.getString("Clans.WerewolfBiteAccountName")) || !"BM_Werewolf".equals(this.config.getString("Clans.WildBiteAccountName")))
		{
			return;
		}

		this.logger.info("Swapping the Bloodmoon and Silvermane skins so each clan uses its own skin account.");

		for (String key : SKIN_KEYS)
		{
			Object bloodmoon = this.config.get("Clans.WerewolfBite" + key);
			this.config.set("Clans.WerewolfBite" + key, this.config.get("Clans.WildBite" + key));
			this.config.set("Clans.WildBite" + key, bloodmoon);
		}
	}

	/**
	 * Version 3 keeps only the texture of each skin, in sections: Clans.&lt;Clan&gt;SkinValue/SkinSignature move to
	 * Clans.Skins.&lt;Clan&gt;.Value/Signature and Clans.AlphaSkinValue/Signature to Clans.AlphaSkin.Value/Signature.
	 * The skin account names and UUIDs are no longer used and are removed.
	 */
	private void migrateToVersion3()
	{
		for (String prefix : V2_SKIN_PREFIXES)
		{
			String section = prefix.equals("Alpha") ? "Clans.AlphaSkin" : "Clans.Skins." + prefix;
			moveSkinKey("Clans." + prefix + "SkinValue", section + ".Value");
			moveSkinKey("Clans." + prefix + "SkinSignature", section + ".Signature");
			this.config.set("Clans." + prefix + "AccountName", null);
			this.config.set("Clans." + prefix + "AccountUUID", null);
		}

		if (this.config.isSet("Clans.Skins"))
		{
			this.config.setComments("Clans.Skins", List.of(
					"Werewolf skins: signed textures (Value and Signature, e.g. from https://mineskin.org). No real Minecraft account is needed.",
					"Set Clans.Skins.<Clan>.Alpha.Value/Signature to give a clan's alpha its own skin; otherwise it uses AlphaSkin."));
		}
	}

	// Only a value the file sets is moved (a missing or empty one gets the bundled default)
	private void moveSkinKey(String from, String to)
	{
		String value = this.config.isSet(from) ? this.config.getString(from) : null;
		if (value != null && !value.isBlank() && !this.config.isSet(to))
		{
			this.config.set(to, value);
		}
		this.config.set(from, null);
	}

	/** Copies config.yml to config.v&lt;version&gt;.bak, unless that backup already exists */
	private boolean backup(int version)
	{
		Path backup = this.file.toPath().resolveSibling("config.v" + version + ".bak");
		if (Files.exists(backup))
		{
			return true;
		}

		try
		{
			Files.copy(this.file.toPath(), backup);
			this.logger.info("Backed up config.yml to " + backup.getFileName() + " before upgrading it.");
			return true;
		}
		catch (IOException ex)
		{
			this.logger.log(Level.SEVERE, "Could not back up config.yml, so the upgrade is only applied in memory and config.yml is left unchanged.", ex);
			return false;
		}
	}

	private void saveAtomically() throws IOException
	{
		Path target = this.file.toPath();
		Path temp = Files.createTempFile(target.getParent(), "config", ".yml.tmp");
		try
		{
			Files.writeString(temp, this.config.saveToString(), StandardCharsets.UTF_8);
			try
			{
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException ex)
			{
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			Files.deleteIfExists(temp);
		}
	}
}
