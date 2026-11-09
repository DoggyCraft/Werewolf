package com.dogonfire.werewolf.disguises;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.disguises.IWerewolfDisguiseFactory.WerewolfDisguise;

import net.md_5.bungee.api.ChatColor;

// Let's abstract disguise commands away, so that we don't depend on a particular Disguise plugin
public abstract class WerewolfDisguiseAPI
{
	static IWerewolfDisguiseFactory werewolfDisguiser;
	static private HashMap<UUID, WerewolfDisguise> disguises = new HashMap<UUID, WerewolfDisguise>(); 
	static private HashMap<UUID, UUID> disguisedPlayers = new HashMap<UUID, UUID>(); 
	
	/**
	 * @param backend "auto" (built-in on Paper, otherwise LibsDisguises, then SkinsRestorer),
	 *                or one of "builtin", "libsdisguises", "skinsrestorer" to force it
	 */
	public static boolean init(String backend)
	{
		PluginManager pm = Werewolf.instance.getServer().getPluginManager();
		boolean libsDisguises = pm.getPlugin("LibsDisguises") != null && pm.getPlugin("LibsDisguises").isEnabled();
		boolean skinsRestorer = pm.getPlugin("SkinsRestorer") != null && pm.getPlugin("SkinsRestorer").isEnabled();
		boolean paper = isPaper();
		backend = backend == null ? "auto" : backend.toLowerCase();

		werewolfDisguiser = null;
		disguises.clear();

		switch (backend)
		{
		case "builtin":
			if (paper)
			{
				werewolfDisguiser = new PaperSkinFactory();
			}
			break;
		case "libsdisguises":
			if (libsDisguises)
			{
				werewolfDisguiser = new LibsDisguisesFactory();
			}
			break;
		case "skinsrestorer":
			if (skinsRestorer)
			{
				werewolfDisguiser = new SkinsRestorerFactory();
			}
			break;
		default:
			if (!backend.equals("auto"))
			{
				Werewolf.instance().log("Unknown Disguises.Backend '" + backend + "', using auto.");
			}
			if (paper)
			{
				werewolfDisguiser = new PaperSkinFactory();
			}
			else if (libsDisguises)
			{
				werewolfDisguiser = new LibsDisguisesFactory();
			}
			else if (skinsRestorer)
			{
				werewolfDisguiser = new SkinsRestorerFactory();
			}
			break;
		}

		if (werewolfDisguiser == null)
		{
			Werewolf.instance().log(ChatColor.RED + "No usable skin backend for Disguises.Backend '" + backend + "' (built-in needs Paper; otherwise install LibsDisguises or SkinsRestorer)... Werewolf disguises are disabled!");
			return false;
		}

		Werewolf.instance().log("Using " + werewolfDisguiser.getClass().getSimpleName().replace("Factory", "") + " for werewolf skins.");
		return true;
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

	public static WerewolfDisguise getDisguise(UUID disguiseAccountId, String disguiseAccountName)
	{
		if (disguises.containsKey(disguiseAccountId))
		{
			return disguises.get(disguiseAccountId);
		}
		
		Werewolf.instance().logDebug("Creating new werewolf disguise - AccountId: " + disguiseAccountId + " AccountName: " + disguiseAccountName);
		
		WerewolfDisguise werewolfDisguise = werewolfDisguiser.newDisguise(disguiseAccountId, disguiseAccountName);
		disguises.put(disguiseAccountId, werewolfDisguise);
		return werewolfDisguise;
	}

	public static boolean disguise(Player player, WerewolfDisguise skin, String werewolfName)
	{				
		if(werewolfDisguiser==null)
		{
			Werewolf.instance().log("[ERROR] Tried disguising a player without a disguise plugin!");
			return false;
		}
		
		if(!skin.disguise(player, werewolfName))
		{
			Werewolf.instance().log("[ERROR] Could not disguise player " + player.getName() + "!");
			return false;
		}

		disguises.put(player.getUniqueId(), skin);

		disguisedPlayers.put(player.getUniqueId(), skin.getSkinAccountUUID());

		return true;		
	}

	public static boolean undisguise(Player player)
	{
		if(werewolfDisguiser==null)
		{
			Werewolf.instance().log("[ERROR] Tried undisguising a player without a disguise plugin!");
			return false;
		}
		
		if(!disguises.get(player.getUniqueId()).undisguise(player))
		{
			return false;
		}

		disguisedPlayers.remove(player.getUniqueId());
		
		return true;
	}
}