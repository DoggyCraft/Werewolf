package com.dogonfire.werewolf;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.dogonfire.werewolf.config.Settings;
import com.dogonfire.werewolf.disguises.WerewolfDisguiseAPI;
import com.dogonfire.werewolf.managers.ClanManager;
import com.dogonfire.werewolf.managers.HuntManager;
import com.dogonfire.werewolf.managers.ItemManager;
import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.managers.MoonManager;
import com.dogonfire.werewolf.managers.PermissionsManager;
import com.dogonfire.werewolf.managers.ScentManager;
import com.dogonfire.werewolf.managers.SignManager;
import com.dogonfire.werewolf.managers.SkinManager;
import com.dogonfire.werewolf.managers.StatisticsManager;
import com.dogonfire.werewolf.managers.TrackerManager;
import com.dogonfire.werewolf.managers.TrophyManager;
import com.dogonfire.werewolf.managers.WerewolfManager;
import com.dogonfire.werewolf.managers.WerewolfScoreboardManager;
import com.dogonfire.werewolf.managers.WolfFormManager;
import com.dogonfire.werewolf.storage.Storage;

import net.milkbowl.vault.economy.Economy;

/**
 * The plugin's object graph, built once per enable by {@link Werewolf#onEnable()} and passed to the constructor of every
 * manager, listener, task and command. There is no static way to reach it.
 * <p>
 * The core managers are created here and live until the plugin is disabled, so their in-memory state (wolf forms,
 * cooldowns, skins) survives /ww reload. Constructors must not call other managers: the graph is still being built.
 * The feature managers (signs, trophies, scoreboards) are created and dropped by {@link Werewolf} according to the
 * current {@link Settings}, so they can be null and must be fetched at use time, never kept in a field.
 */
public final class WerewolfContext
{
	private final Werewolf						plugin;
	private final Storage						storage;

	private final boolean						vaultEnabled;
	private final boolean						vampireEnabled;
	private final boolean						healthBarEnabled;

	private final LanguageManager				languageManager;
	private final PermissionsManager			permissionsManager;
	private final MoonManager					moonManager;
	private final WolfFormManager				wolfFormManager;
	private final WerewolfManager				werewolfManager;
	private final ClanManager					clanManager;
	private final StatisticsManager				statisticsManager;
	private final ItemManager					itemManager;
	private final TrackerManager				trackerManager;
	private final ScentManager					scentManager;
	private final SkinManager					skinManager;
	private final WerewolfDisguiseAPI			disguises;
	private final HuntManager					huntManager;

	// Feature managers, (re)created by Werewolf.startFeatures according to Settings
	private volatile SignManager				signManager;
	private volatile TrophyManager				trophyManager;
	private volatile WerewolfScoreboardManager	scoreboardManager;

	// /ww on and /ww off; read from the async chat thread
	private volatile boolean					werewolvesEnabled	= true;

	WerewolfContext(Werewolf plugin, Storage storage)
	{
		this.plugin = plugin;
		this.storage = storage;

		PluginManager pm = plugin.getServer().getPluginManager();
		this.vaultEnabled = isEnabled(pm, "Vault");
		this.vampireEnabled = isEnabled(pm, "VampireRevamp");
		this.healthBarEnabled = isEnabled(pm, "HealthBar");

		this.languageManager = new LanguageManager(this);
		this.permissionsManager = new PermissionsManager(this);
		this.moonManager = new MoonManager(this);
		this.wolfFormManager = new WolfFormManager(this);
		this.werewolfManager = new WerewolfManager(this, storage.werewolves());
		this.clanManager = new ClanManager(this, storage.clans());
		this.statisticsManager = new StatisticsManager();
		this.itemManager = new ItemManager(this);
		this.trackerManager = new TrackerManager(this);
		this.scentManager = new ScentManager(this);
		this.skinManager = new SkinManager(this);
		this.disguises = new WerewolfDisguiseAPI(this);
		// Bounties and hunting need Vault (its economy is looked up on use, see getEconomy)
		this.huntManager = this.vaultEnabled ? new HuntManager(this, storage.hunters()) : null;
	}

	static boolean isEnabled(PluginManager pm, String name)
	{
		Plugin other = pm.getPlugin(name);
		return other != null && other.isEnabled();
	}

	public Werewolf getPlugin()
	{
		return this.plugin;
	}

	/** The current settings; replaced on /ww reload, so read them when needed instead of keeping them */
	public Settings getSettings()
	{
		return this.plugin.getSettings();
	}

	public Storage getStorage()
	{
		return this.storage;
	}

	public boolean isVaultEnabled()
	{
		return this.vaultEnabled;
	}

	public boolean isVampireEnabled()
	{
		return this.vampireEnabled;
	}

	public boolean isHealthBarEnabled()
	{
		return this.healthBarEnabled;
	}

	/** Whether a skin backend is in use; without one werewolves keep their own skin */
	public boolean isDisguisesEnabled()
	{
		return this.disguises.isEnabled();
	}

	/** False after /ww off: nobody transforms and most listeners do nothing */
	public boolean isWerewolvesEnabled()
	{
		return this.werewolvesEnabled;
	}

	public void setWerewolvesEnabled(boolean enabled)
	{
		this.werewolvesEnabled = enabled;
	}

	/**
	 * Vault's economy provider, looked up on every call because the economy plugin may enable after Werewolf (or be
	 * replaced). Null without Vault or without an economy plugin; callers must handle that.
	 */
	public Economy getEconomy()
	{
		if (!this.vaultEnabled)
		{
			return null;
		}

		RegisteredServiceProvider<Economy> provider = this.plugin.getServer().getServicesManager().getRegistration(Economy.class);
		return provider != null ? provider.getProvider() : null;
	}

	public LanguageManager getLanguageManager()
	{
		return this.languageManager;
	}

	public PermissionsManager getPermissionsManager()
	{
		return this.permissionsManager;
	}

	public MoonManager getMoonManager()
	{
		return this.moonManager;
	}

	public WolfFormManager getWolfFormManager()
	{
		return this.wolfFormManager;
	}

	public WerewolfManager getWerewolfManager()
	{
		return this.werewolfManager;
	}

	public ClanManager getClanManager()
	{
		return this.clanManager;
	}

	public StatisticsManager getStatisticsManager()
	{
		return this.statisticsManager;
	}

	public ItemManager getItemManager()
	{
		return this.itemManager;
	}

	public TrackerManager getTrackerManager()
	{
		return this.trackerManager;
	}

	public ScentManager getScentManager()
	{
		return this.scentManager;
	}

	public SkinManager getSkinManager()
	{
		return this.skinManager;
	}

	public WerewolfDisguiseAPI getDisguises()
	{
		return this.disguises;
	}

	/** Null without Vault */
	public HuntManager getHuntManager()
	{
		return this.huntManager;
	}

	/** Null unless Signs.Enabled */
	public SignManager getSignManager()
	{
		return this.signManager;
	}

	/** Null unless Trophies.Enabled */
	public TrophyManager getTrophyManager()
	{
		return this.trophyManager;
	}

	/** Null unless Scoreboards.Enabled */
	public WerewolfScoreboardManager getWerewolfScoreboardManager()
	{
		return this.scoreboardManager;
	}

	void setSignManager(SignManager signManager)
	{
		this.signManager = signManager;
	}

	void setTrophyManager(TrophyManager trophyManager)
	{
		this.trophyManager = trophyManager;
	}

	void setWerewolfScoreboardManager(WerewolfScoreboardManager scoreboardManager)
	{
		this.scoreboardManager = scoreboardManager;
	}
}
