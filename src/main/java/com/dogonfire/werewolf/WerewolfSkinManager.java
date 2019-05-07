package com.dogonfire.werewolf;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.entity.Player;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;

public class WerewolfSkinManager
{
	private Werewolf						plugin;
	private HashMap<UUID, PlayerDisguise>	skins			= new HashMap<UUID, PlayerDisguise>();
	protected int							nextID			= -2147483648;

	WerewolfSkinManager(Werewolf p)
	{
		this.plugin = p;
	}

	public PlayerDisguise getSkin(Player player)
	{
		return (PlayerDisguise) this.skins.get(player.getUniqueId());
	}
	
	public Disguise getDisguise(Player player)
	{
		return (Disguise) this.skins.get(player.getUniqueId());
	}

	public int getNextAvailableID()
	{
		return this.nextID++;
	}
	
	// TODO: Only disguise for everyone BUT the player, so they don't have to see their own face
	public boolean setWerewolfSkin(Player player)
	{
		if (this.skins.containsKey(player.getUniqueId()))
		{
			return true;
		}
		
		String account;

		ClanManager.ClanType clantype = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());
		
		if(plugin.useClans && Werewolf.getClanManager().isAlpha(player.getUniqueId()))
		{
			account = Werewolf.getClanManager().getWerewolfAccountForAlpha(clantype);
						
		}
		else
		{
			account = Werewolf.getClanManager().getWerewolfAccountForClan(clantype);
		}
		
		if (DisguiseAPI.isViewSelfToggled(player)) {
			DisguiseAPI.setViewDisguiseToggled(player, false);
		}
		
		PlayerDisguise skin = new PlayerDisguise(player.getName());
		skin.setSkin(account);
		this.skins.put(player.getUniqueId(), skin);
		DisguiseAPI.disguiseToAll(player, skin);

		return true;
	}

	public void unsetWerewolfSkin(Player player)
	{
		if (!this.skins.containsKey(player.getUniqueId()))
		{
			return;
		}

		DisguiseAPI.undisguiseToAll(player);

		this.skins.remove(player.getUniqueId());
	}
}
