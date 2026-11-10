package com.dogonfire.werewolf.disguises;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;

/**
 * Built-in skin changing using Paper's Player#setPlayerProfile. Paper refreshes the player
 * for everyone, so no other plugin is needed. Only constructed when running on Paper.
 */
public class PaperSkinFactory implements IWerewolfDisguiseFactory
{
	private static final String				TEXTURES		= "textures";

	// The player's own skin, kept so it can be put back when they turn human. Empty means they had no skin data
	// (offline mode, or no textures at login), so turning back just removes the werewolf textures.
	private final Map<UUID, Optional<ProfileProperty>>	originalSkins	= new ConcurrentHashMap<>();
	private final WerewolfContext				ctx;
	private final Werewolf						plugin;

	public PaperSkinFactory(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	public class PaperWerewolfDisguise extends WerewolfDisguise
	{
		public PaperWerewolfDisguise(UUID accountId, String accountName)
		{
			super(PaperSkinFactory.this.ctx, accountId, accountName);
		}

		@Override
		public boolean disguise(Player player, String werewolfName)
		{
			plugin.logDebug("Disguising player " + player.getName() + " using the built-in Paper skins...");

			PlayerProfile profile = player.getPlayerProfile();

			if (!originalSkins.containsKey(player.getUniqueId()))
			{
				ProfileProperty own = null;
				boolean wearingWerewolfSkin = false;
				for (ProfileProperty property : profile.getProperties())
				{
					if (property.getName().equals(TEXTURES))
					{
						if (isWerewolfTexture(property.getValue()))
						{
							wearingWerewolfSkin = true;
						}
						else
						{
							own = property;
						}
					}
				}
				// A werewolf skin still on from a moment ago (a refresh while the original is being looked up) isn't theirs
				if (!wearingWerewolfSkin)
				{
					originalSkins.put(player.getUniqueId(), Optional.ofNullable(own));
				}
			}

			profile.setProperty(new ProfileProperty(TEXTURES, getSkinTextureValue(player), getSkinTextureSignature(player)));
			player.setPlayerProfile(profile);
			return true;
		}

		@Override
		public boolean undisguise(Player player)
		{
			plugin.logDebug("Undisguising player " + player.getName() + " using the built-in Paper skins...");

			PlayerProfile profile = player.getPlayerProfile();
			Optional<ProfileProperty> originalSkin = originalSkins.remove(player.getUniqueId());

			// Known: put their own skin back, or no skin data at all if they had none
			// Unknown on an offline-mode server: Mojang can't help, so the same as having none
			if (originalSkin != null || !plugin.getServer().getOnlineMode() || !plugin.isEnabled())
			{
				profile.removeProperty(TEXTURES);
				if (originalSkin != null)
				{
					originalSkin.ifPresent(profile::setProperty);
				}
				player.setPlayerProfile(profile);
				return true;
			}

			// We don't know the original skin, so ask Mojang for it
			PlayerProfile lookup = plugin.getServer().createProfile(player.getUniqueId(), player.getName());
			lookup.update().thenAccept(updated -> plugin.getServer().getScheduler().runTask(plugin, () ->
			{
				// Back in wolf form meanwhile (a skin refresh undisguises and disguises right away)
				if (!player.isOnline() || ctx.getSkinManager().getSkin(player) != null)
				{
					return;
				}

				PlayerProfile current = player.getPlayerProfile();
				current.removeProperty(TEXTURES);
				for (ProfileProperty property : updated.getProperties())
				{
					if (property.getName().equals(TEXTURES))
					{
						current.setProperty(property);
					}
				}
				player.setPlayerProfile(current);
			})).exceptionally(ex ->
			{
				plugin.getLogger().log(Level.WARNING, "Could not look up the skin of " + player.getName() + " to give it back; they keep the werewolf skin until they relog", ex);
				return null;
			});
			return true;
		}
	}

	@Override
	public WerewolfDisguise newDisguise(UUID disguiseAccountId, String disguiseAccountName)
	{
		return new PaperWerewolfDisguise(disguiseAccountId, disguiseAccountName);
	}
}
