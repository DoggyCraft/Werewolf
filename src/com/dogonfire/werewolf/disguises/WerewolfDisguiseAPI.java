package com.dogonfire.werewolf.disguises;

import java.util.HashMap;
import java.util.UUID;

import com.dogonfire.werewolf.managers.ClanManager;
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
	
	public static boolean init()
	{
		PluginManager pm = Werewolf.instance.getServer().getPluginManager();
		
		if (pm.getPlugin("LibsDisguises") != null && pm.getPlugin("LibsDisguises").isEnabled())
		{
			Werewolf.instance().log("Lib's Disguises found, using it for werewolf disguise!");
			werewolfDisguiser = new LibsDisguisesFactory();
			return werewolfDisguiser.isEnabled();
		}
		else if (pm.getPlugin("SkinsRestorer") != null && pm.getPlugin("SkinsRestorer").isEnabled())
		{
			Werewolf.instance().log("SkinsRestorer found, using it for werewolf disguises!");
			werewolfDisguiser = new SkinsRestorerFactory();
			return werewolfDisguiser.isEnabled();
		}
		else if (pm.getPlugin("MySkin") != null && pm.getPlugin("MySkin").isEnabled())
		{
			Werewolf.instance().log("MySkin found - Werewolf doesn't support MySkin anymore, please use SkinsRestorer or Lib's Disguises instead!");
//			Werewolf.instance().log("MySkin found, using it for werewolf disguises!");
//			werewolfDisguiser = new MySkinFactory();
//			return werewolfDisguiser.isEnabled();
		}

		Werewolf.instance().log(ChatColor.RED + "No supported disguise plugin found... Werewolf disguises are disabled!");
		return false;
	}

	public static WerewolfDisguise getDisguise(ClanManager.ClanType clanType, boolean isAlpha)
	{
		if (disguises.containsKey(Werewolf.getClanManager().getWerewolfAccountId(clanType, isAlpha)))
		{
			return disguises.get(Werewolf.getClanManager().getWerewolfAccountId(clanType, isAlpha));
		}

		Werewolf.instance().logDebug("Creating new werewolf disguise - ClanType: " + clanType.name() + " IsAlpha: " + isAlpha);
		
		WerewolfDisguise werewolfDisguise = werewolfDisguiser.newDisguise(clanType, isAlpha);
		disguises.put(werewolfDisguise.accountId, werewolfDisguise);
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