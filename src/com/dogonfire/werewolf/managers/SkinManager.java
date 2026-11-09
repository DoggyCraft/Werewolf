package com.dogonfire.werewolf.managers;

import com.dogonfire.werewolf.api.Clan;
import java.util.HashMap;
import java.util.UUID;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.disguises.IWerewolfDisguiseFactory.WerewolfDisguise;

public class SkinManager
{
	private final Werewolf plugin;
	private final WerewolfContext ctx;
	private HashMap<UUID, WerewolfDisguise>	skins	= new HashMap<UUID, WerewolfDisguise>();

	public SkinManager(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	public WerewolfDisguise getSkin(Player player)
	{
		return this.skins.get(player.getUniqueId());
	}



	public boolean setWerewolfSkin(Player player, String werewolfName)
	{
		return applyWerewolfSkin(player, werewolfName, true);
	}

	/** Swaps the skin of a disguised werewolf for the one it should have now (alpha or clan skin), without howling */
	public boolean refreshWerewolfSkin(Player player, String werewolfName)
	{
		unsetWerewolfSkin(player);
		return applyWerewolfSkin(player, werewolfName, false);
	}

	private boolean applyWerewolfSkin(Player player, String werewolfName, boolean howl)
	{
		if (this.skins.containsKey(player.getUniqueId()))
		{
			return true;
		}

		UUID werewolfAccountId;
		String werewolfAccountName;

		Clan clantype = ctx.getWerewolfManager().getWerewolfClan(player.getUniqueId());

		// Get the correct account, if alpha, alphaskin, else the clanskin
		if (plugin.getSettings().clans().enabled() && ctx.getClanManager().isAlpha(player.getUniqueId()))
		{
			werewolfAccountId = ctx.getClanManager().getWerewolfAccountIdForAlpha(clantype);
			werewolfAccountName = ctx.getClanManager().getWerewolfAccountForAlpha(clantype);
		}
		else
		{
			werewolfAccountId = ctx.getClanManager().getWerewolfAccountIdForClan(clantype);
			werewolfAccountName = ctx.getClanManager().getWerewolfAccountForClan(clantype);
		}

		if (werewolfName.isEmpty())
		{
			werewolfName = "Werewolf";
		}

		WerewolfDisguise skin = ctx.getDisguises().getDisguise(werewolfAccountId, werewolfAccountName);
		
		if (skin == null)
		{
			plugin.log("WerewolfDisguise Skin is null!");
			return false;
		}

		plugin.logDebug("Skin: " + skin.toString() + ". SkinName: " + skin.getSkinAccountName() + " - SkinUUID: " + skin.getSkinAccountUUID());

		if(ctx.getDisguises().disguise(player, skin, werewolfName))
		{
			plugin.logDebug("Disguised player " + player.getName() + "!");
			if (howl)
			{
				ctx.getWerewolfManager().howl(player);
			}
		}
		else
		{
			plugin.logDebug("Could not disguise " + player.getName());
		}

		this.skins.put(player.getUniqueId(), skin);

		return true;
	}

	public void unsetWerewolfSkin(Player player)
	{
		if (!this.skins.containsKey(player.getUniqueId()))
		{
			return;
		}

		if(ctx.getDisguises().undisguise(player))
		{
			plugin.logDebug("Undisguised player " + player.getName() + "!");
		}
		else
		{
			plugin.logDebug("Could not undisguise " + player.getName());
		}

		this.skins.remove(player.getUniqueId());
	}
}
