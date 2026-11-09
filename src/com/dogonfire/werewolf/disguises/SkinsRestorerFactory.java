package com.dogonfire.werewolf.disguises;

import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.managers.ClanManager;
import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.config.Settings;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinsRestorerFactory implements IWerewolfDisguiseFactory
{
    private final WerewolfContext ctx;
    private final Werewolf plugin;
    private final SkinsRestorer skinsRestorer;

    // Skin each player had set in SkinsRestorer before transforming; restored on undisguise
    private final Map<UUID, SkinIdentifier> previousSkins = new ConcurrentHashMap<>();

    public class SkinsRestorerWerewolfDisguise extends WerewolfDisguise
    {
        public SkinsRestorerWerewolfDisguise(UUID accountId, String accountName) {
            super(SkinsRestorerFactory.this.ctx, accountId, accountName);
        }

        @Override
        public boolean disguise(Player player, String werewolfName) {
            plugin.logDebug("Disguising player " + player.getName() + " using SkinsRestorer...");

            if(!plugin.getServer().getPluginManager().getPlugin("SkinsRestorer").isEnabled())
            {
                plugin.logDebug("Didn't disguise player... SkinsRestorer is not enabled!");
                return false;
            }

            plugin.logDebug("[SkinsRestorer] Started Disguising of player: " + player.getName());

            try
            {
                UUID playerId = player.getUniqueId();

                // Remember the current skin so it can be restored when the player turns back
                skinsRestorer.getPlayerStorage().getSkinIdOfPlayer(playerId).ifPresent(id -> previousSkins.putIfAbsent(playerId, id));

                String skinName;
                if (ctx.getClanManager().isAlpha(playerId)) {
                    skinName = "Alpha";
                }
                else {
                    skinName = ctx.getWerewolfManager().getWerewolfClan(playerId).name();
                }
                plugin.logDebug("[SkinsRestorer] Disguising " + player.getName() + " using the " + skinName + " skin.");
                skinsRestorer.getPlayerStorage().setSkinIdOfPlayer(playerId, SkinIdentifier.ofCustom(skinName));

                // Force skin refresh for player
                skinsRestorer.getSkinApplier(Player.class).applySkin(player);
                return true;
            }
            catch (DataRequestException e)
            {
                plugin.log("[SkinsRestorer] Failed to apply skin for player");
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public boolean undisguise(Player player) {
            plugin.logDebug("Undisguising player " + player.getName() + " using SkinsRestorer...");

            if(!plugin.getServer().getPluginManager().getPlugin("SkinsRestorer").isEnabled())
            {
                plugin.logDebug("Didn't undisguise player... SkinsRestorer is not enabled!");
                return false;
            }

            plugin.logDebug("[SkinsRestorer] Undisguise - Start undisguise!");

            try
            {
                SkinIdentifier previousSkin = previousSkins.remove(player.getUniqueId());
                if (previousSkin != null) {
                    skinsRestorer.getPlayerStorage().setSkinIdOfPlayer(player.getUniqueId(), previousSkin);
                }
                else {
                    skinsRestorer.getPlayerStorage().removeSkinIdOfPlayer(player.getUniqueId());
                }

                // Force skin refresh for player
                skinsRestorer.getSkinApplier(Player.class).applySkin(player);
                return true;
            }
            catch (DataRequestException e)
            {
                plugin.log("[SkinsRestorer] Failed to apply skin for player");
                e.printStackTrace();
            }
            return false;
        }
    }

    public SkinsRestorerFactory(WerewolfContext ctx)
    {
        this.ctx = ctx;
        this.plugin = ctx.getPlugin();
        this.skinsRestorer = SkinsRestorerProvider.get();

        Settings.Clans clans = ctx.getSettings().clans();
        skinsRestorer.getSkinStorage().setCustomSkinData("Alpha", SkinProperty.of(clans.alphaSkin().value(), clans.alphaSkin().signature()));
        for (Clan clan : Clan.values())
        {
            Settings.Skin skin = clans.skins().get(clan);
            skinsRestorer.getSkinStorage().setCustomSkinData(clan.name(), SkinProperty.of(skin.value(), skin.signature()));
        }
    }

    @Override
    public WerewolfDisguise newDisguise(UUID disguiseAccountId, String disguiseAccountName)
    {
        return new SkinsRestorerWerewolfDisguise(disguiseAccountId, disguiseAccountName);
    }
}
