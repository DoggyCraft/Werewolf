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
import net.skinsrestorer.api.property.SkinType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinsRestorerFactory implements IWerewolfDisguiseFactory
{
    private static final String ALPHA_PREFIX = "Alpha_";

    private final WerewolfContext ctx;
    private final Werewolf plugin;
    private final SkinsRestorer skinsRestorer;

    // Skin each player had set in SkinsRestorer before transforming; restored on undisguise
    private final Map<UUID, SkinIdentifier> previousSkins = new ConcurrentHashMap<>();

    public class SkinsRestorerWerewolfDisguise extends WerewolfDisguise
    {
        public SkinsRestorerWerewolfDisguise(Clan clan, boolean alpha) {
            super(SkinsRestorerFactory.this.ctx, clan, alpha);
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
                // (never a werewolf skin left over from a crash, or they would stay a werewolf after turning back)
                skinsRestorer.getPlayerStorage().getSkinIdOfPlayer(playerId).filter(id -> !isWerewolfSkin(id)).ifPresent(id -> previousSkins.putIfAbsent(playerId, id));

                String skinName = customSkinName(getClan(), isAlpha());
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
        registerSkins();
    }

    // The clan and alpha skins as SkinsRestorer custom skins named after each Clan constant and Alpha_<Clan>
    private void registerSkins()
    {
        Settings.Clans clans = ctx.getSettings().clans();
        for (Clan clan : Clan.values())
        {
            for (boolean alpha : new boolean[] { false, true })
            {
                Settings.Skin skin = clans.skin(clan, alpha);
                skinsRestorer.getSkinStorage().setCustomSkinData(customSkinName(clan, alpha), SkinProperty.of(skin.value(), skin.signature()));
            }
        }
    }

    private static String customSkinName(Clan clan, boolean alpha)
    {
        return alpha ? ALPHA_PREFIX + clan.name() : clan.name();
    }

    @Override
    public void reloadSkins()
    {
        registerSkins();
    }

    // One of the custom skins registered above, or the shared "Alpha" skin of versions before per-clan alpha skins
    private static boolean isWerewolfSkin(SkinIdentifier id)
    {
        if (id.getSkinType() != SkinType.CUSTOM)
        {
            return false;
        }
        if (id.getIdentifier().equalsIgnoreCase("Alpha"))
        {
            return true;
        }
        for (Clan clan : Clan.values())
        {
            if (id.getIdentifier().equalsIgnoreCase(customSkinName(clan, false)) || id.getIdentifier().equalsIgnoreCase(customSkinName(clan, true)))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * SkinsRestorer keeps the skin it was given across restarts, so a player who was in wolf form when the server
     * crashed would keep the werewolf skin. The skin they had before is unknown by then, so they get their own back.
     */
    @Override
    public boolean clearLeftover(Player player)
    {
        if (!plugin.getServer().getPluginManager().getPlugin("SkinsRestorer").isEnabled())
        {
            return false;
        }

        try
        {
            UUID playerId = player.getUniqueId();
            if (!skinsRestorer.getPlayerStorage().getSkinIdOfPlayer(playerId).filter(SkinsRestorerFactory::isWerewolfSkin).isPresent())
            {
                return false;
            }

            SkinIdentifier previousSkin = previousSkins.remove(playerId);
            if (previousSkin != null)
            {
                skinsRestorer.getPlayerStorage().setSkinIdOfPlayer(playerId, previousSkin);
            }
            else
            {
                skinsRestorer.getPlayerStorage().removeSkinIdOfPlayer(playerId);
            }
            skinsRestorer.getSkinApplier(Player.class).applySkin(player);
            return true;
        }
        catch (DataRequestException e)
        {
            plugin.log("[SkinsRestorer] Failed to remove a left over werewolf skin from " + player.getName());
            return false;
        }
    }

    @Override
    public WerewolfDisguise newDisguise(Clan clan, boolean alpha)
    {
        return new SkinsRestorerWerewolfDisguise(clan, alpha);
    }
}
