package com.dogonfire.werewolf.disguises;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.config.Settings;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;

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
		public LibsDisguisesWerewolfDisguise(Clan clan, boolean alpha)
		{
			super(LibsDisguisesFactory.this.ctx, clan, alpha);
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

				plugin.logDebug("WerewolfName: " + werewolfName + " - Skin: " + this);

				// The first profile only gives the disguise its name; the skin comes from the second one's texture, so
				// LibsDisguises never looks a skin up at Mojang (as it would for a skin given by account name)
				PlayerDisguise skin = new PlayerDisguise(new UserProfile(profileId(werewolfName), werewolfName), skinProfile(werewolfName));
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

		// A profile carrying only the configured texture. LibsDisguises copies the texture onto the disguise's own
		// profile, so its id and name don't matter.
		private UserProfile skinProfile(String werewolfName)
		{
			Settings.Skin texture = getSkin();
			String signature = texture.signature() == null || texture.signature().isBlank() ? null : texture.signature();
			return new UserProfile(profileId(werewolfName), werewolfName, new ArrayList<>(List.of(new TextureProperty("textures", texture.value(), signature))));
		}
	}

	// The offline-mode style id for a name, so it never belongs to a real account
	private static UUID profileId(String name)
	{
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public LibsDisguisesWerewolfDisguise newDisguise(Clan clan, boolean alpha)
	{
		return new LibsDisguisesWerewolfDisguise(clan, alpha);
	}
}
