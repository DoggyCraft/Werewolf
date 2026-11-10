package com.dogonfire.werewolf;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.dogonfire.werewolf.api.WerewolfService;
import com.dogonfire.werewolf.commands.SingleCommandExecutor;
import com.dogonfire.werewolf.commands.WerewolfCommand;
import com.dogonfire.werewolf.commands.sub.GrowlCommand;
import com.dogonfire.werewolf.commands.sub.HowlCommand;
import com.dogonfire.werewolf.config.ConfigMigrator;
import com.dogonfire.werewolf.config.Settings;
import com.dogonfire.werewolf.config.SettingsLoader;
import com.dogonfire.werewolf.disguises.WerewolfDisguiseAPI;
import com.dogonfire.werewolf.integrations.ForcePvPIntegration;
import com.dogonfire.werewolf.integrations.IntegrationListener;
import com.dogonfire.werewolf.integrations.PvPManagerIntegration;
import com.dogonfire.werewolf.integrations.VampireIntegration;
import com.dogonfire.werewolf.integrations.WorldGuardIntegration;
import com.dogonfire.werewolf.listeners.AlphaListener;
import com.dogonfire.werewolf.listeners.ChatListener;
import com.dogonfire.werewolf.listeners.DamageListener;
import com.dogonfire.werewolf.listeners.HuntingGearListener;
import com.dogonfire.werewolf.listeners.InteractListener;
import com.dogonfire.werewolf.listeners.InventoryListener;
import com.dogonfire.werewolf.listeners.PlayerListener;
import com.dogonfire.werewolf.listeners.WerewolfRestrictionListener;
import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.managers.SignManager;
import com.dogonfire.werewolf.managers.TrophyManager;
import com.dogonfire.werewolf.managers.WerewolfScoreboardManager;
import com.dogonfire.werewolf.storage.Storage;
import com.dogonfire.werewolf.tasks.InfoTask;
import com.dogonfire.werewolf.utils.WerewolfPlaceholderExpansion;

/**
 * The plugin. onEnable builds the {@link WerewolfContext} (the object graph), registers the always-on listeners,
 * commands and repeating tasks, then starts the features that depend on settings ({@link #startFeatures()}).
 * /ww reload ({@link #reload()}) re-reads config, language and data files and restarts only those features; onDisable
 * undoes everything in reverse.
 */
public class Werewolf extends JavaPlugin
{
	// https://bstats.org/plugin/bukkit/Werewolf/35251
	private static final int				BSTATS_PLUGIN_ID	= 35251;

	private volatile Settings				settings;
	private Storage							storage;
	private WerewolfContext					ctx;
	private String							disguiseBackend;
	private WerewolfServiceImpl				service;
	private Metrics							metrics;

	// Started by startFeatures, undone by stopFeatures
	private final List<Listener>			featureListeners	= new ArrayList<Listener>();
	private final List<BukkitTask>			featureTasks		= new ArrayList<BukkitTask>();
	private final List<IntegrationListener>	integrations		= new ArrayList<IntegrationListener>();
	private WerewolfPlaceholderExpansion	placeholderExpansion;

	@Override
	public void onEnable()
	{
		// Settings first: every constructor below may read them
		loadSettings();

		// All data files are read into memory now; managers get their repository from the context
		this.storage = new Storage(getDataFolder(), getLogger());
		this.storage.load();

		this.ctx = new WerewolfContext(this, this.storage);

		if (this.ctx.isVaultEnabled())
		{
			log("Vault detected. Bounties and sign economy are enabled!");
		}
		else
		{
			log("Vault not found. Werewolf bounties and signs are disabled.");
		}
		if (this.ctx.isVampireEnabled())
		{
			log("Vampire plugin detected. Enabling support for vampirism :-)");
		}
		if (this.ctx.isHealthBarEnabled())
		{
			log("HealthBar plugin detected. Enabling support for healthbars.");
		}

		this.ctx.getClanManager().load();
		this.ctx.getWerewolfManager().refreshAllProfiles();
		this.ctx.getMoonManager().snapshotPhases();
		this.ctx.getLanguageManager().load();
		initDisguises();

		registerCommands();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new PlayerListener(this.ctx), this);
		pm.registerEvents(new InteractListener(this.ctx), this);
		pm.registerEvents(new DamageListener(this.ctx), this);
		pm.registerEvents(new WerewolfRestrictionListener(this.ctx), this);
		pm.registerEvents(new HuntingGearListener(this.ctx), this);
		pm.registerEvents(new AlphaListener(this.ctx), this);
		pm.registerEvents(new InventoryListener(this.ctx), this);

		if (WerewolfContext.isEnabled(pm, "PlaceholderAPI"))
		{
			this.placeholderExpansion = new WerewolfPlaceholderExpansion(this.ctx);
			this.placeholderExpansion.register();
		}

		// Moon, transformations, infection and auto-cure
		getServer().getScheduler().runTaskTimer(this, () -> this.ctx.getWerewolfManager().update(), 20L, 100L);

		// Writes changed data files every 3 minutes (and on /ww save and disable)
		getServer().getScheduler().runTaskTimer(this, () -> this.storage.flush(), 3600L, 3600L);

		// Tracker compasses, scent trails and the silver sword penalty, once a second
		getServer().getScheduler().runTaskTimer(this, () -> {
			if (!this.ctx.isWerewolvesEnabled())
			{
				return;
			}
			this.ctx.getTrackerManager().update();
			this.ctx.getScentManager().update();
			this.ctx.getItemManager().update();
		}, 20L, 20L);

		startFeatures();

		// Last, so other plugins only see the API once everything is loaded
		this.service = new WerewolfServiceImpl(this.ctx);
		getServer().getServicesManager().register(WerewolfService.class, this.service, this, ServicePriority.Normal);
		WerewolfAPI.setService(this.service);

		startMetrics();
	}

	@Override
	public void onDisable()
	{
		// First, so other plugins stop using the API while everything shuts down
		WerewolfAPI.setService(null);
		if (this.service != null)
		{
			getServer().getServicesManager().unregister(WerewolfService.class, this.service);
			this.service = null;
		}

		if (this.metrics != null)
		{
			this.metrics.shutdown();
			this.metrics = null;
		}

		if (this.ctx == null)
		{
			// onEnable failed before the graph was built
			return;
		}

		stopFeatures();

		if (this.ctx.getHuntManager() != null)
		{
			this.ctx.getHuntManager().stop();
		}
		this.ctx.getScentManager().stopAll();
		this.ctx.getTrackerManager().clearAll();

		// Back to human form now (no delayed task), so werewolves.yml is saved with them in human form
		this.ctx.getWerewolfManager().untransformAllForShutdown();

		WerewolfScoreboardManager scoreboards = this.ctx.getWerewolfScoreboardManager();
		if (scoreboards != null)
		{
			scoreboards.stop();
			this.ctx.setWerewolfScoreboardManager(null);
		}

		if (this.placeholderExpansion != null)
		{
			this.placeholderExpansion.unregister();
			this.placeholderExpansion = null;
		}

		getServer().getScheduler().cancelTasks(this);
		HandlerList.unregisterAll(this);

		// Writes every changed data file and waits for the writes
		this.storage.close();

		this.ctx = null;
		this.storage = null;
	}

	/**
	 * /ww reload: config.yml (migrated), the language file and the data files edited on disk are read again, and the
	 * features are restarted so toggles take effect. Werewolves stay in wolf form; only a changed skin backend
	 * re-skins them.
	 */
	public void reload()
	{
		reloadConfig();
		loadSettings();
		this.ctx.getLanguageManager().load();
		this.storage.reload();
		this.ctx.getClanManager().load();
		this.ctx.getWerewolfManager().refreshAllProfiles();

		if (!this.settings.general().disguiseBackend().equalsIgnoreCase(this.disguiseBackend))
		{
			switchDisguiseBackend();
		}
		else
		{
			// Changed Clans.* skins, for backends that register them up front (SkinsRestorer)
			this.ctx.getDisguises().reloadSkins();
		}

		stopFeatures();
		startFeatures();
	}

	/**
	 * Anonymous usage statistics on bstats.org; admins opt out for all plugins in plugins/bStats/config.yml. The charts
	 * read the current settings and context each time bStats collects them (on the main thread), so they follow
	 * /ww reload.
	 */
	private void startMetrics()
	{
		this.metrics = new Metrics(this, BSTATS_PLUGIN_ID);

		this.metrics.addCustomChart(new SimplePie("disguise_backend", () -> {
			WerewolfDisguiseAPI.Backend backend = this.ctx.isDisguisesEnabled() ? this.ctx.getDisguises().getBackend() : null;
			return backend == null ? "none" : backend.name().toLowerCase();
		}));
		this.metrics.addCustomChart(new SimplePie("language", () -> this.settings.general().language()));
		this.metrics.addCustomChart(new SimplePie("clans", () -> this.settings.clans().enabled() ? "Enabled" : "Disabled"));
		this.metrics.addCustomChart(new SimplePie("hunting", () -> this.settings.hunting().enabled() ? "Enabled" : "Disabled"));
		this.metrics.addCustomChart(new SimplePie("werewolves", () -> {
			int count = this.ctx.getWerewolfManager().getProfiles().size();
			if (count == 0)
			{
				return "0";
			}
			if (count <= 10)
			{
				return "1-10";
			}
			if (count <= 50)
			{
				return "11-50";
			}
			if (count <= 200)
			{
				return "51-200";
			}
			return "200+";
		}));
		this.metrics.addCustomChart(new AdvancedPie("integrations", () -> {
			Map<String, Integer> used = new HashMap<String, Integer>();
			if (this.ctx.isVaultEnabled())
			{
				used.put("Vault", 1);
			}
			if (this.placeholderExpansion != null)
			{
				used.put("PlaceholderAPI", 1);
			}
			for (IntegrationListener integration : this.integrations)
			{
				used.put(integration.getClass().getSimpleName().replace("Integration", ""), 1);
			}
			return used;
		}));
	}

	/** Features that can be switched on and off in config.yml, plus the optional-plugin integrations */
	private void startFeatures()
	{
		Settings settings = this.settings;
		PluginManager pm = getServer().getPluginManager();

		if (settings.signs().enabled())
		{
			SignManager signManager = new SignManager(this.ctx);
			this.ctx.setSignManager(signManager);
			registerFeature(signManager);
		}

		if (settings.general().trophiesEnabled())
		{
			TrophyManager trophyManager = new TrophyManager(this.ctx, this.storage.trophies());
			this.ctx.setTrophyManager(trophyManager);
			registerFeature(trophyManager);
		}

		updateScoreboards(settings);

		if (settings.general().wolfChat())
		{
			registerFeature(new ChatListener(this.ctx));
		}

		if (settings.clans().enabled())
		{
			this.featureTasks.add(getServer().getScheduler().runTaskTimer(this, () -> this.ctx.getClanManager().updateClans(), 20L, 72000L));
		}

		this.ctx.getItemManager().addRecipes();

		startIntegrations(pm, settings);
	}

	// Each integration class references its plugin's classes, so only construct it when that plugin is present
	private void startIntegrations(PluginManager pm, Settings settings)
	{
		if (this.ctx.isVampireEnabled())
		{
			this.integrations.add(new VampireIntegration(this.ctx));
		}

		boolean worldGuard = WerewolfContext.isEnabled(pm, "WorldGuard");
		boolean pvpManager = isPvPManager4(pm);
		boolean griefPrevention = WerewolfContext.isEnabled(pm, "GriefPrevention");
		if (worldGuard && settings.integrations().worldGuard())
		{
			log("WorldGuard detected. Trackers and sniffing respect PvP and invincible regions.");
			this.integrations.add(new WorldGuardIntegration(this.ctx));
		}
		if (pvpManager && settings.integrations().pvpManager())
		{
			log("PvPManager detected. Trackers and sniffing respect PvP protection.");
			this.integrations.add(new PvPManagerIntegration(this.ctx));
		}
		if (pvpManager && griefPrevention && settings.integrations().pvpManager() && settings.integrations().griefPrevention())
		{
			log("GriefPrevention and PvPManager detected. Force PvP for trespassers is available (Hunting.ForcePvP).");
			this.integrations.add(new ForcePvPIntegration(this.ctx));
		}

		for (IntegrationListener integration : this.integrations)
		{
			getServer().getPluginManager().registerEvents(integration, this);
		}
	}

	private void stopFeatures()
	{
		for (IntegrationListener integration : this.integrations)
		{
			HandlerList.unregisterAll(integration);
			try
			{
				integration.shutdown();
			}
			catch (RuntimeException | LinkageError e)
			{
				getLogger().log(Level.WARNING, "Could not shut down " + integration.getClass().getSimpleName(), e);
			}
		}
		this.integrations.clear();

		for (Listener listener : this.featureListeners)
		{
			HandlerList.unregisterAll(listener);
		}
		this.featureListeners.clear();

		for (BukkitTask task : this.featureTasks)
		{
			task.cancel();
		}
		this.featureTasks.clear();

		this.ctx.getItemManager().removeRecipes();

		this.ctx.setSignManager(null);
		this.ctx.setTrophyManager(null);
	}

	// Kept across a reload that leaves Scoreboards.Enabled on, so werewolves keep their hunting scoreboard and its counts
	private void updateScoreboards(Settings settings)
	{
		WerewolfScoreboardManager scoreboards = this.ctx.getWerewolfScoreboardManager();
		if (settings.general().scoreboardsEnabled() && scoreboards == null)
		{
			scoreboards = new WerewolfScoreboardManager(this.ctx);
			this.ctx.setWerewolfScoreboardManager(scoreboards);
			scoreboards.start();
		}
		else if (!settings.general().scoreboardsEnabled() && scoreboards != null)
		{
			scoreboards.stop();
			this.ctx.setWerewolfScoreboardManager(null);
		}
	}

	private void registerFeature(Listener listener)
	{
		getServer().getPluginManager().registerEvents(listener, this);
		this.featureListeners.add(listener);
	}

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

	private void initDisguises()
	{
		this.disguiseBackend = this.settings.general().disguiseBackend();
		this.ctx.getDisguises().init(this.disguiseBackend);
	}

	// Disguises.Backend changed: take the old skins off, switch, and put the new ones on (no transformation messages)
	private void switchDisguiseBackend()
	{
		List<Player> inWolfForm = new ArrayList<Player>();
		for (Player player : getServer().getOnlinePlayers())
		{
			if (this.ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
			{
				inWolfForm.add(player);
				this.ctx.getSkinManager().unsetWerewolfSkin(player);
			}
		}

		initDisguises();

		if (this.ctx.isDisguisesEnabled())
		{
			for (Player player : inWolfForm)
			{
				this.ctx.getSkinManager().refreshWerewolfSkin(player, player.getPlayerListName());
			}
		}
	}

	private void registerCommands()
	{
		WerewolfCommand werewolfCommand = new WerewolfCommand(this.ctx);
		getCommand("werewolf").setExecutor(werewolfCommand);
		getCommand("werewolf").setTabCompleter(werewolfCommand);

		SingleCommandExecutor growl = new SingleCommandExecutor(this.ctx, new GrowlCommand(this.ctx));
		getCommand("growl").setExecutor(growl);
		getCommand("growl").setTabCompleter(growl);

		SingleCommandExecutor howl = new SingleCommandExecutor(this.ctx, new HowlCommand(this.ctx));
		getCommand("howl").setExecutor(howl);
		getCommand("howl").setTabCompleter(howl);
	}

	/**
	 * Writes the bundled config.yml on first start, upgrades an old config.yml and reads the settings from it. Bukkit
	 * uses the bundled config.yml as defaults, so missing keys get their default values.
	 */
	private void loadSettings()
	{
		saveDefaultConfig();
		new ConfigMigrator(new File(getDataFolder(), "config.yml"), getConfig(), getLogger()).migrate();
		this.settings = new SettingsLoader(getConfig(), getLogger(), getServer().getWorlds().get(0).getName()).load();
	}

	/** The current settings; replaced by /ww reload, so read them when needed instead of keeping them */
	public Settings getSettings()
	{
		return this.settings;
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

		getServer().getScheduler().runTaskLater(this, new InfoTask(this.ctx, color, playerId, message, amount, name), delay);
	}

	public void log(String message)
	{
		getLogger().info(message);
	}

	public void logDebug(String message)
	{
		if (this.settings != null && this.settings.general().debug())
		{
			getLogger().info("[Debug] " + message);
		}
	}
}
