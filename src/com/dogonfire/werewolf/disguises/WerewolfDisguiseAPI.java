package com.dogonfire.werewolf.disguises;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.disguises.IWerewolfDisguiseFactory.WerewolfDisguise;

/** Hides which skin backend is in use, so that we don't depend on a particular disguise plugin */
public class WerewolfDisguiseAPI
{
	public enum Backend
	{
		BUILTIN, LIBSDISGUISES, SKINSRESTORER
	}

	private final WerewolfContext				ctx;
	private final Werewolf						plugin;
	private IWerewolfDisguiseFactory			werewolfDisguiser;
	private Backend								activeBackend;
	// Disguises by skin account, and the disguise each disguised player wears
	private final Map<UUID, WerewolfDisguise>	disguises	= new HashMap<UUID, WerewolfDisguise>();
	private final Map<UUID, WerewolfDisguise>	worn		= new HashMap<UUID, WerewolfDisguise>();

	public WerewolfDisguiseAPI(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	/**
	 * Picks the skin backend. Called on enable and when /ww reload changes Disguises.Backend (after every disguised
	 * player was undisguised with the old backend).
	 *
	 * @param backend "auto" (built-in on Paper, otherwise LibsDisguises, then SkinsRestorer),
	 *                or one of "builtin", "libsdisguises", "skinsrestorer" to force it
	 * @return whether a backend is available
	 */
	public boolean init(String backend)
	{
		PluginManager pm = this.plugin.getServer().getPluginManager();
		boolean libsDisguises = pm.getPlugin("LibsDisguises") != null && pm.getPlugin("LibsDisguises").isEnabled();
		boolean skinsRestorer = pm.getPlugin("SkinsRestorer") != null && pm.getPlugin("SkinsRestorer").isEnabled();
		boolean paper = isPaper();
		backend = backend == null ? "auto" : backend.toLowerCase();

		this.werewolfDisguiser = null;
		this.activeBackend = null;
		this.disguises.clear();
		this.worn.clear();

		switch (backend)
		{
		case "builtin":
			if (paper)
			{
				use(new PaperSkinFactory(this.ctx), Backend.BUILTIN);
			}
			break;
		case "libsdisguises":
			if (libsDisguises)
			{
				use(new LibsDisguisesFactory(this.ctx), Backend.LIBSDISGUISES);
			}
			break;
		case "skinsrestorer":
			if (skinsRestorer)
			{
				use(new SkinsRestorerFactory(this.ctx), Backend.SKINSRESTORER);
			}
			break;
		default:
			if (!backend.equals("auto"))
			{
				this.plugin.log("Unknown Disguises.Backend '" + backend + "', using auto.");
			}
			if (paper)
			{
				use(new PaperSkinFactory(this.ctx), Backend.BUILTIN);
			}
			else if (libsDisguises)
			{
				use(new LibsDisguisesFactory(this.ctx), Backend.LIBSDISGUISES);
			}
			else if (skinsRestorer)
			{
				use(new SkinsRestorerFactory(this.ctx), Backend.SKINSRESTORER);
			}
			break;
		}

		if (this.werewolfDisguiser == null)
		{
			this.plugin.log(ChatColor.RED + "No usable skin backend for Disguises.Backend '" + backend + "' (built-in needs Paper; otherwise install LibsDisguises or SkinsRestorer)... Werewolf disguises are disabled!");
			return false;
		}

		this.plugin.log("Using " + this.werewolfDisguiser.getClass().getSimpleName().replace("Factory", "") + " for werewolf skins.");
		return true;
	}

	private void use(IWerewolfDisguiseFactory factory, Backend backend)
	{
		this.werewolfDisguiser = factory;
		this.activeBackend = backend;
	}

	public boolean isEnabled()
	{
		return this.werewolfDisguiser != null;
	}

	/** The skin backend chosen by {@link #init(String)}, or null if disguises are disabled */
	public Backend getBackend()
	{
		return this.activeBackend;
	}

	// Checked by name so PaperSkinFactory (which links against Paper classes) is never loaded on Spigot
	private static boolean isPaper()
	{
		try
		{
			Class<?> profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
			Player.class.getMethod("setPlayerProfile", profileClass);
			return true;
		}
		catch (ClassNotFoundException | NoSuchMethodException e)
		{
			return false;
		}
	}

	public WerewolfDisguise getDisguise(UUID disguiseAccountId, String disguiseAccountName)
	{
		WerewolfDisguise cached = this.disguises.get(disguiseAccountId);
		if (cached != null)
		{
			return cached;
		}

		if (this.werewolfDisguiser == null)
		{
			return null;
		}

		this.plugin.logDebug("Creating new werewolf disguise - AccountId: " + disguiseAccountId + " AccountName: " + disguiseAccountName);

		WerewolfDisguise werewolfDisguise = this.werewolfDisguiser.newDisguise(disguiseAccountId, disguiseAccountName);
		this.disguises.put(disguiseAccountId, werewolfDisguise);
		return werewolfDisguise;
	}

	public boolean disguise(Player player, WerewolfDisguise skin, String werewolfName)
	{
		if (this.werewolfDisguiser == null)
		{
			this.plugin.log("[ERROR] Tried disguising a player without a disguise plugin!");
			return false;
		}

		if (!skin.disguise(player, werewolfName))
		{
			this.plugin.log("[ERROR] Could not disguise player " + player.getName() + "!");
			return false;
		}

		this.worn.put(player.getUniqueId(), skin);
		return true;
	}

	public boolean undisguise(Player player)
	{
		if (this.werewolfDisguiser == null)
		{
			this.plugin.log("[ERROR] Tried undisguising a player without a disguise plugin!");
			return false;
		}

		WerewolfDisguise skin = this.worn.get(player.getUniqueId());
		if (skin == null || !skin.undisguise(player))
		{
			return false;
		}

		this.worn.remove(player.getUniqueId());
		return true;
	}
}
