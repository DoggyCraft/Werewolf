package com.dogonfire.werewolf;

import com.clanjhoo.vampire.VampireAPI;
import com.dogonfire.werewolf.commands.SingleCommandExecutor;
import com.dogonfire.werewolf.commands.WerewolfCommand;
import com.dogonfire.werewolf.commands.sub.GrowlCommand;
import com.dogonfire.werewolf.commands.sub.HowlCommand;
import com.dogonfire.werewolf.config.ConfigMigrator;
import com.dogonfire.werewolf.config.Settings;
import com.dogonfire.werewolf.config.SettingsLoader;
import com.dogonfire.werewolf.tasks.CompassTrackerTask;
import com.dogonfire.werewolf.disguises.WerewolfDisguiseAPI;
import com.dogonfire.werewolf.events.WerewolfTransformEvent;
import com.dogonfire.werewolf.integrations.ForcePvPIntegration;
import com.dogonfire.werewolf.integrations.IntegrationListener;
import com.dogonfire.werewolf.integrations.PvPManagerIntegration;
import com.dogonfire.werewolf.integrations.VampireIntegration;
import com.dogonfire.werewolf.integrations.WorldGuardIntegration;
import com.dogonfire.werewolf.listeners.ChatListener;
import com.dogonfire.werewolf.listeners.DamageListener;
import com.dogonfire.werewolf.listeners.HuntingGearListener;
import com.dogonfire.werewolf.listeners.AlphaListener;
import com.dogonfire.werewolf.listeners.InteractListener;
import com.dogonfire.werewolf.listeners.InventoryListener;
import com.dogonfire.werewolf.listeners.PlayerListener;
import com.dogonfire.werewolf.listeners.WerewolfRestrictionListener;
import com.dogonfire.werewolf.managers.*;
import com.dogonfire.werewolf.tasks.CentralMessageTask;
import com.dogonfire.werewolf.storage.Storage;
import com.dogonfire.werewolf.tasks.DisguiseTask;
import com.dogonfire.werewolf.tasks.UndisguiseTask;
import com.dogonfire.werewolf.utils.PacketUtils;
import com.dogonfire.werewolf.utils.WerewolfPlaceholderExpansion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Werewolf extends JavaPlugin
{
	public static Werewolf 						instance;
	public static boolean						pluginEnabled							= false;

	public boolean 								vampireEnabled 							= false;
	public boolean								vaultEnabled							= false;
	public boolean								disguisesEnabled						= false;
	public boolean								healthBarEnabled						= false;
	public static Server						server									= null;
	private volatile Settings				settings								= null;
	private static Werewolf						plugin;
	public static PacketUtils pu										= null;
	private static LanguageManager languageManager							= null;
	private static ClanManager clanManager								= null;
	private static SignManager					signManager								= null;
	private static WerewolfManager				werewolfManager							= null;
	private static HuntManager huntManager								= null;
	private static TrophyManager				trophyManager							= null;
	private static Storage						storage									= null;
	private static SkinManager					skinManager								= null;
	private static PermissionsManager			permissionsManager						= null;
	private static WerewolfScoreboardManager	werewolfScoreboardManager				= null;
	private static StatisticsManager			statisticsManager						= null;
	private static ItemManager itemManager								= null;
	private static MoonManager					moonManager								= null;
	private static WolfFormManager				wolfFormManager							= null;
	private static TrackerManager				trackerManager							= null;
	private static ScentManager					scentManager							= null;

	private static Economy						economy									= null;
	private final List<IntegrationListener>		integrations							= new ArrayList<IntegrationListener>();

	// The integration is written against PvPManager 4.x (me.chancesd); 3.x (me.NoChance) has a different API
	private boolean isPvPManager4(PluginManager pm)
	{
		Plugin pvpManager = pm.getPlugin("PvPManager");
		if (pvpManager == null || !pvpManager.isEnabled())
		{
			return false;
		}

		try
		{
			Class.forName("me.chancesd.pvpmanager.player.CombatPlayer", false, pvpManager.getClass().getClassLoader());
			return true;
		}
		catch (ClassNotFoundException | LinkageError e)
		{
			log("PvPManager " + pvpManager.getDescription().getVersion() + " is not supported (needs 4.x). PvPManager integration is disabled.");
			return false;
		}
	}

	public static Werewolf instance()
	{
		return instance;
	}
	
	public static StatisticsManager getStatisticsManager()
	{
		return statisticsManager;
	}

	public static WerewolfScoreboardManager getWerewolfScoreboardManager()
	{
		return werewolfScoreboardManager;
	}

	public static ItemManager getItemManager()
	{
		return itemManager;
	}

	public static MoonManager getMoonManager()
	{
		return moonManager;
	}

	public static WolfFormManager getWolfFormManager()
	{
		return wolfFormManager;
	}

	public static TrackerManager getTrackerManager()
	{
		return trackerManager;
	}

	public static ScentManager getScentManager()
	{
		return scentManager;
	}

	public static SignManager getSignManager()
	{
		return signManager;
	}

	public static LanguageManager getLanguageManager()
	{
		return languageManager;
	}

	/*
	 * public static PotionManager getPotionManager() { return potionManager; }
	 */

	public static PermissionsManager getPermissionsManager()
	{
		return permissionsManager;
	}

	public static ClanManager getClanManager()
	{
		return clanManager;
	}

	public static WerewolfManager getWerewolfManager()
	{
		return werewolfManager;
	}

	public static HuntManager getHuntManager()
	{
		return huntManager;
	}

	public static Storage getStorage()
	{
		return storage;
	}

	public static TrophyManager getTrophyManager()
	{
		return trophyManager;
	}

	public static SkinManager getSkinManager()
	{
		return skinManager;
	}

	public static Economy getEconomy()
	{
		return economy;
	}

	public void announcementMessage(World world, String messageText, Sound sound, long delay)
	{
		server.getScheduler().runTaskLater(plugin, new CentralMessageTask(world, messageText, sound), delay);
	}

	public void disguiseWerewolf(Player p)
	{
		server.getScheduler().runTaskLater(plugin, new DisguiseTask(plugin, p), 1L);
	}

	/** Back to human form (the werewolf's data is kept) */
	public void undisguiseWerewolf(UUID playerId, boolean makeVisible)
	{
		server.getScheduler().runTaskLater(plugin, new UndisguiseTask(plugin, playerId, makeVisible, false, null), 8L);
	}

	/**
	 * The visual part of a cure: the caller has already removed the player's data. {@code originalGroup} is the
	 * permission group to put them back in, read before the data was removed.
	 */
	public void undisguiseCuredWerewolf(UUID playerId, String originalGroup)
	{
		server.getScheduler().runTaskLater(plugin, new UndisguiseTask(plugin, playerId, true, true, originalGroup), 8L);
	}

	public void sendInfo(Player player, String message)
	{
		if (player == null)
		{
			log(message);
		}
		else
		{
			player.sendMessage(message);
		}
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, String name, int amount, int delay)
	{
		if (playerId == null)
		{
			return;
		}

		Player player = getServer().getPlayer(playerId);
		if (player == null)
		{
			logDebug("sendInfo can not find online player with id " + playerId);
			return;
		}

		getServer().getScheduler().runTaskLater(this, new com.dogonfire.werewolf.tasks.InfoTask(this, color, playerId, message, amount, name), delay);
	}

	public void onDisable()
	{
		if (vaultEnabled)
		{
			CompassTrackerTask.stop();
		}

		for (IntegrationListener integration : this.integrations)
		{
			integration.shutdown();
		}
		this.integrations.clear();

		if (scentManager != null)
		{
			scentManager.stopAll();
		}
		if (trackerManager != null)
		{
			trackerManager.clearAll();
		}

		for (Player player : getServer().getOnlinePlayers())
		{
			if (getWerewolfManager().isWerewolf(player) && getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
			{
				untransform(player, WerewolfTransformEvent.Reason.FORCED);
			}
		}

		// Writes every changed data file and waits for the writes
		if (storage != null)
		{
			storage.close();
			storage = null;
		}

		pluginEnabled = false;
	}

	@Override
	public void onEnable()
	{
		DamageListener damageListener = null;
		PlayerListener playerListener = null;
		InteractListener interactListener = null;
		InventoryListener inventoryListener = null;
		ChatListener chatListener = null;
		WerewolfRestrictionListener restrictionListener = null;
		HuntingGearListener huntingGearListener = null;

		plugin = this;
		instance = this;
		server = getServer();

		// All data files are read into memory now; managers get their repository from here
		storage = new Storage(getDataFolder(), getLogger());
		storage.load();

		loadSettings();

		registerCommands();

		pluginEnabled = true;

		moonManager = new MoonManager(this);
		wolfFormManager = new WolfFormManager(this);
		werewolfManager = new WerewolfManager(this, storage.werewolves());
		clanManager = new ClanManager(this, storage.clans());
		// potionManager = new PotionManager(this);
		languageManager = new LanguageManager(this);
		statisticsManager = new StatisticsManager();
		itemManager = new ItemManager(this);
		trackerManager = new TrackerManager(this);
		scentManager = new ScentManager(this);
		damageListener = new DamageListener(this);
		playerListener = new PlayerListener(this);
		interactListener = new InteractListener(this);
		chatListener = new ChatListener(this);
		restrictionListener = new WerewolfRestrictionListener(this);
		huntingGearListener = new HuntingGearListener(this);
		skinManager = new SkinManager(this);

		inventoryListener = new InventoryListener(this);

		pu = new PacketUtils();

		PluginManager pm = getServer().getPluginManager();

		// Check for Vault
		if (pm.getPlugin("Vault") != null && pm.getPlugin("Vault").isEnabled())
		{
			this.vaultEnabled = true;
			huntManager = new HuntManager(this, storage.hunters());

			log("Vault detected. Bounties and sign economy are enabled!");

			CompassTrackerTask.setPlugin(this);

			RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
			if (economyProvider != null)
			{
				economy = economyProvider.getProvider();
			}
			else
			{
				plugin.log("Vault not found.");
			}
		}
		else
		{
			log("Vault not found. Werewolf bounties and signs are disabled.");
		}

		permissionsManager = new PermissionsManager(this);

		if (pm.getPlugin("VampireRevamp") != null && pm.getPlugin("VampireRevamp").isEnabled())
		{
			log("Vampire plugin detected. Enabling support for vampirism :-)");

			this.vampireEnabled = true;
			this.integrations.add(new VampireIntegration());
		}

		// Check for HealthBar
		if (pm.getPlugin("HealthBar") != null && pm.getPlugin("HealthBar").isEnabled())
		{
			log("HealthBar plugin detected. Enabling support for healthbars.");

			this.healthBarEnabled = true;
		}

		// Check for PlaceholderAPI
		if (pm.getPlugin("PlaceholderAPI") != null && pm.getPlugin("PlaceholderAPI").isEnabled())
		{
			new WerewolfPlaceholderExpansion(this).register();
		}

		// Region/PvP plugins. Each integration class references its plugin's classes, so only construct it when present
		boolean worldGuard = pm.getPlugin("WorldGuard") != null && pm.getPlugin("WorldGuard").isEnabled();
		boolean pvpManager = isPvPManager4(pm);
		boolean griefPrevention = pm.getPlugin("GriefPrevention") != null && pm.getPlugin("GriefPrevention").isEnabled();
		if (worldGuard)
		{
			log("WorldGuard detected. Trackers and sniffing respect PvP and invincible regions.");
			this.integrations.add(new WorldGuardIntegration(this));
		}
		if (pvpManager)
		{
			log("PvPManager detected. Trackers and sniffing respect PvP protection.");
			this.integrations.add(new PvPManagerIntegration(this));
		}
		if (pvpManager && griefPrevention)
		{
			log("GriefPrevention and PvPManager detected. Force PvP for trespassers is available (Hunting.ForcePvP).");
			this.integrations.add(new ForcePvPIntegration(this));
		}

		getServer().getPluginManager().registerEvents(playerListener, this);
		getServer().getPluginManager().registerEvents(interactListener, this);
		getServer().getPluginManager().registerEvents(damageListener, this);
		getServer().getPluginManager().registerEvents(chatListener, this);
		getServer().getPluginManager().registerEvents(restrictionListener, this);
		getServer().getPluginManager().registerEvents(huntingGearListener, this);
		getServer().getPluginManager().registerEvents(new AlphaListener(this), this);
		getServer().getPluginManager().registerEvents(inventoryListener, this);
		for (IntegrationListener integration : this.integrations)
		{
			getServer().getPluginManager().registerEvents(integration, this);
		}

		permissionsManager.load();
		clanManager.load();
		languageManager.load();

		// Needs the skin settings from config.yml
		this.disguisesEnabled = WerewolfDisguiseAPI.init(this.settings.general().disguiseBackend());
		if (this.disguisesEnabled)
		{
			// Pre-cache some known disguises
			for (Settings.Skin skin : this.settings.clans().skins().values())
			{
				WerewolfDisguiseAPI.getDisguise(skin.accountId(), skin.accountName());
			}
			WerewolfDisguiseAPI.getDisguise(this.settings.clans().alphaSkin().accountId(), this.settings.clans().alphaSkin().accountName());
		}

		itemManager.setupRecipes();

		if (this.settings.general().scoreboardsEnabled())
		{
			Werewolf.werewolfScoreboardManager = new WerewolfScoreboardManager();
		}

		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			public void run()
			{
				Werewolf.getWerewolfManager().update();
			}
		}, 20L, 100L);

		// Writes changed data files every 3 minutes (and on /ww save and disable)
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> storage.flush(), 3600L, 3600L);

		// Tracker compasses, scent trails and the silver sword penalty, once a second
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			public void run()
			{
				if (!Werewolf.pluginEnabled)
				{
					return;
				}
				Werewolf.getTrackerManager().update();
				Werewolf.getScentManager().update();
				Werewolf.getItemManager().update();
			}
		}, 20L, 20L);

		if (this.settings.clans().enabled())
		{
			getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
			{
				public void run()
				{
					Werewolf.getClanManager().updateClans();
				}
			}, 20L, 72000L);
		}
	}

	public boolean isWerewolvesAllowedInWorld(Player player)
	{
		return this.settings.allowedWorlds().contains(player.getWorld().getName());
	}

	public boolean isUnderOpenSky(Player player)
	{
		return player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getLocation().getBlockY();
	}

	public void transform(Player player, WerewolfTransformEvent.Reason reason)
	{
		if (!isWerewolvesAllowedInWorld(player))
		{
			return;
		}

		if (!getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			player.getLocation().getWorld().playEffect(player.getLocation(), Effect.SMOKE, 100);
			player.getLocation().getWorld().playEffect(player.getLocation().add(new Vector(0, 1, 0)), Effect.SMOKE, 100);
			player.getLocation().getWorld().playEffect(player.getLocation().add(new Vector(0, 2, 0)), Effect.SMOKE, 100);
			werewolfManager.setWerewolfSkin(player, reason);
		}
		else
		{
			plugin.log("Could not transform " + player.getName() + ": Not a werewolf!");
		}
	}

	public void untransform(Player player, WerewolfTransformEvent.Reason reason)
	{
		player.getLocation().getWorld().playEffect(player.getLocation(), Effect.SMOKE, 100);
		player.getLocation().getWorld().playEffect(player.getLocation().add(new Vector(0, 1, 0)), Effect.SMOKE, 100);
		player.getLocation().getWorld().playEffect(player.getLocation().add(new Vector(0, 2, 0)), Effect.SMOKE, 100);

		werewolfManager.unsetWerewolfSkin(player.getUniqueId(), true, reason);
	}

	public void log(String message)
	{
		plugin.getLogger().info(message);
	}

	public void logDebug(String message)
	{
		if (this.settings != null && this.settings.general().debug())
		{
			plugin.getLogger().info("[Debug] " + message);
		}
	}

	public Settings getSettings()
	{
		return this.settings;
	}

	public void reloadSettings()
	{
		reloadConfig();
		loadSettings();

		storage.reload();
		getClanManager().load();
		getLanguageManager().load();
	}

	/**
	 * Writes the bundled config.yml on first start, upgrades an old config.yml and reads the settings from it. Bukkit
	 * uses the bundled config.yml as defaults, so missing keys get their default values.
	 */
	public void loadSettings()
	{
		saveDefaultConfig();
		new ConfigMigrator(new File(getDataFolder(), "config.yml"), getConfig(), getLogger()).migrate();
		this.settings = new SettingsLoader(getConfig(), getLogger(), getServer().getWorlds().get(0).getName()).load();

		if (this.settings.general().trophiesEnabled() && trophyManager == null)
		{
			trophyManager = new TrophyManager(this, storage.trophies());
			getServer().getPluginManager().registerEvents(trophyManager, this);
		}

		if (this.settings.signs().enabled() && signManager == null)
		{
			signManager = new SignManager(this);
			getServer().getPluginManager().registerEvents(signManager, this);
		}
	}

	private void registerCommands()
	{
		WerewolfCommand werewolfCommand = new WerewolfCommand(this);
		getCommand("werewolf").setExecutor(werewolfCommand);
		getCommand("werewolf").setTabCompleter(werewolfCommand);

		SingleCommandExecutor growl = new SingleCommandExecutor(this, new GrowlCommand(this));
		getCommand("growl").setExecutor(growl);
		getCommand("growl").setTabCompleter(growl);

		SingleCommandExecutor howl = new SingleCommandExecutor(this, new HowlCommand(this));
		getCommand("howl").setExecutor(howl);
		getCommand("howl").setTabCompleter(howl);
	}
}
