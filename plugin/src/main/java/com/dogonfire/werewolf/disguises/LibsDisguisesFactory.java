package com.dogonfire.werewolf.disguises;

import java.util.UUID;

import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;

public class LibsDisguisesFactory implements IWerewolfDisguiseFactory
{
	private final WerewolfContext	ctx;
	private final Werewolf			plugin;

	public LibsDisguisesFactory(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	public class LibsDisguisesWerewolfDisguise extends WerewolfDisguise
	{
		public LibsDisguisesWerewolfDisguise(UUID werewolfDisguiseAccountId, String werewolfDisguiseAccountName)
		{
			super(LibsDisguisesFactory.this.ctx, werewolfDisguiseAccountId, werewolfDisguiseAccountName);
		}		
				
		public boolean disguise(Player player, String werewolfName)
		{
			plugin.logDebug("Disguising player " + player.getName() + " using LibsDisguises...");

			if(!plugin.getServer().getPluginManager().getPlugin("LibsDisguises").isEnabled())
			{
				plugin.logDebug("Didn't disguise player... Libs Disguises not enabled!");
				return false;	
			}
			
			try
			{
				if (DisguiseAPI.isViewSelfToggled(player))
				{
					DisguiseAPI.setViewDisguiseToggled(player, false);
				}

				plugin.logDebug("WerewolfName: " + werewolfName + " - WerewolfAccountName: " + getSkinAccountName());

				PlayerDisguise skin = new PlayerDisguise(werewolfName, getSkinAccountName());
				skin.getWatcher().setCapeEnabled(false);

				if (ctx.getSettings().names().enabled())
				{
					String customWerewolfName = ctx.getWerewolfManager().getWerewolfName(player.getUniqueId());
					if (customWerewolfName != null && !customWerewolfName.isEmpty())
					{
						skin.getWatcher().setCustomName(customWerewolfName);
						skin.getWatcher().setCustomNameVisible(true);
					}
				}

				DisguiseAPI.disguiseToAll(player, skin);

				return true;
			}
			catch (NoClassDefFoundError e)
			{
				plugin.logDebug("Couldn't disguise player... Libs Disguises not found!");
			}
			return false;		
		}
		
		public boolean undisguise(Player player)
		{
			try
			{
				DisguiseAPI.undisguiseToAll(player);
				player.setCustomNameVisible(false);
				return true;
			}
			catch (NoClassDefFoundError e)
			{
				plugin.logDebug("Couldn't un disguise player... Libs Disguises not found!");
			}
			return false;
						
		}		
	}

	@Override
	public LibsDisguisesWerewolfDisguise newDisguise(UUID disguiseAccountId, String disguiseAccountName)
	{
		return new LibsDisguisesWerewolfDisguise(disguiseAccountId, disguiseAccountName);
	}
}
