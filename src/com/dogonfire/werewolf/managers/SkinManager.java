package com.dogonfire.werewolf.managers;

import java.util.HashMap;
import java.util.UUID;

import com.dogonfire.werewolf.Werewolf;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.disguises.IWerewolfDisguiseFactory.WerewolfDisguise;
import com.dogonfire.werewolf.disguises.WerewolfDisguiseAPI;

public class SkinManager
{
	private Werewolf plugin;
	private HashMap<UUID, WerewolfDisguise>	skins	= new HashMap<UUID, WerewolfDisguise>();
	protected int							nextID	= -2147483648;

	public SkinManager(Werewolf p)
	{
		this.plugin = p;
	}

	public WerewolfDisguise getSkin(Player player)
	{
		return this.skins.get(player.getUniqueId());
	}

	public WerewolfDisguise getDisguise(Player player)
	{
		return this.skins.get(player.getUniqueId());
	}

	public int getNextAvailableID()
	{
		return this.nextID++;
	}

	public boolean setWerewolfSkin(Player player, String werewolfName)
	{
		if (this.skins.containsKey(player.getUniqueId()))
		{
			return true;
		}

		ClanManager.ClanType clanType = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());

		if (werewolfName.isEmpty())
		{
			werewolfName = "Werewolf";
		}

		WerewolfDisguise skin = WerewolfDisguiseAPI.getDisguise(clanType, plugin.useClans && Werewolf.getClanManager().isAlpha(player.getUniqueId()));
		
		if (skin == null)
		{
			plugin.log("WerewolfDisguise Skin is null!");
		}
		
		plugin.logDebug("Skin: " + skin.toString() + ". SkinName: " + skin.getSkinAccountName() + " - SkinUUID: " + skin.getSkinAccountUUID());

		// Disguise the player in an async thread
		String finalWerewolfName = werewolfName;
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			if(WerewolfDisguiseAPI.disguise(player, skin, finalWerewolfName))
			{
				plugin.logDebug("Disguised player " + player.getName() + "!");
				Werewolf.getWerewolfManager().howl(player);
			}
			else
			{
				plugin.logDebug("Could not disguise " + player.getName());
			}
		});

		this.skins.put(player.getUniqueId(), skin);

		return true;
	}

	public void unsetWerewolfSkin(Player player)
	{
		if (!this.skins.containsKey(player.getUniqueId()))
		{
			return;
		}

		// Undisguise the player in an async thread
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			if(WerewolfDisguiseAPI.undisguise(player))
			{
				plugin.logDebug("Undisguised player " + player.getName() + "!");
			}
			else
			{
				plugin.logDebug("Could not undisguise " + player.getName());
			}
		});

		this.skins.remove(player.getUniqueId());
	}
}
