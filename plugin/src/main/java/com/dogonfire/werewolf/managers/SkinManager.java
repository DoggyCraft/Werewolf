package com.dogonfire.werewolf.managers;

import com.dogonfire.werewolf.api.Clan;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

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

		Clan clantype = ctx.getWerewolfManager().getWerewolfClan(player.getUniqueId());
		// The alpha wears the alpha skin of its clan, everyone else the clan skin
		boolean alpha = plugin.getSettings().clans().enabled() && ctx.getClanManager().isAlpha(player.getUniqueId());

		if (werewolfName.isEmpty())
		{
			werewolfName = "Werewolf";
		}

		WerewolfDisguise skin = ctx.getDisguises().getDisguise(clantype, alpha);
		
		if (skin == null)
		{
			plugin.log("WerewolfDisguise Skin is null!");
			return false;
		}

		plugin.logDebug("Skin: " + skin);

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

	/** Takes the werewolf skin off. The entry is forgotten even if the backend fails, so the next transformation re-applies it. */
	public void unsetWerewolfSkin(Player player)
	{
		if (this.skins.remove(player.getUniqueId()) == null || !ctx.isDisguisesEnabled())
		{
			return;
		}

		try
		{
			if (ctx.getDisguises().undisguise(player))
			{
				plugin.logDebug("Undisguised player " + player.getName() + "!");
			}
			else
			{
				plugin.logDebug("Could not undisguise " + player.getName());
			}
		}
		catch (RuntimeException e)
		{
			plugin.getLogger().log(Level.WARNING, "Could not undisguise " + player.getName(), e);
		}
	}

	/**
	 * On join, for a player not in wolf form: takes off any werewolf skin left from before (a skin we still track, or one
	 * the backend kept across a restart, such as a SkinsRestorer skin).
	 */
	public void clearLeftoverSkin(Player player)
	{
		unsetWerewolfSkin(player);
		if (!ctx.isDisguisesEnabled())
		{
			return;
		}

		try
		{
			if (ctx.getDisguises().clearLeftover(player))
			{
				plugin.log("Took a werewolf skin left over from before off " + player.getName() + ".");
			}
		}
		catch (RuntimeException e)
		{
			plugin.getLogger().log(Level.WARNING, "Could not check " + player.getName() + " for a left over werewolf skin", e);
		}
	}
}
