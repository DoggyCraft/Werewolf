package com.dogonfire.werewolf.disguises;

import com.dogonfire.werewolf.managers.ClanManager;
import com.dogonfire.werewolf.Werewolf;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import org.bukkit.entity.Player;

public class SkinsRestorerFactory implements IWerewolfDisguiseFactory
{
    private SkinsRestorerAPI skinsRestorerAPI;
    private long lastSkinUpdateTime	= 0L;
    private boolean enabled = true;

    public class SkinsRestorerWerewolfDisguise extends WerewolfDisguise
    {
        String disguiseName;

        public SkinsRestorerWerewolfDisguise(ClanManager.ClanType clanType, boolean isAlpha) {
            super(clanType, isAlpha);

            if (isAlpha) {
                this.disguiseName = clanType.name() + "_Alpha";
            }
            else {
                this.disguiseName = clanType.name();
            }
        }

        @Override
        public boolean disguise(Player player, String werewolfName) {
            Werewolf.instance().logDebug("Disguising player " + player.getName() + " using SkinsRestorer...");

            if(!Werewolf.instance().getServer().getPluginManager().getPlugin("SkinsRestorer").isEnabled())
            {
                Werewolf.instance().logDebug("Didn't disguise player... SkinsRestorer is not enabled!");
                return false;
            }

            Werewolf.instance().logDebug("[SkinsRestorer] Started Disguising of player: " + player.getName());

            try
            {
                // Cache the old, current skin
                skinsRestorerAPI.setSkinData(player.getName() + "_cache", skinsRestorerAPI.getSkinData(player.getName()));

                if (System.currentTimeMillis() - lastSkinUpdateTime >= 180000L)
                {
                    Werewolf.instance().logDebug("[SkinsRestorer] Updating skin data...");
                    updateSkinData();
                }

                // #setSkin() for player skin
                Werewolf.instance().logDebug("[SkinsRestorer] Disguising " + player.getName() + " using the " + this.disguiseName + " disguise.");
                skinsRestorerAPI.setSkin(player.getName(), this.disguiseName);

                // Force skin refresh for player
                skinsRestorerAPI.applySkin(new PlayerWrapper(player));
                return true;
            }
            catch (SkinRequestException e)
            {
                Werewolf.instance().log("[SkinsRestorer] Failed to apply skin for player");
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public boolean undisguise(Player player) {
            Werewolf.instance().logDebug("Undisguising player " + player.getName() + " using SkinsRestorer...");

            if(!Werewolf.instance().getServer().getPluginManager().getPlugin("SkinsRestorer").isEnabled())
            {
                Werewolf.instance().logDebug("Didn't undisguise player... SkinsRestorer is not enabled!");
                return false;
            }

            Werewolf.instance().logDebug("[SkinsRestorer] Undisguise - Start undisguise!");

            try
            {
                skinsRestorerAPI.setSkin(player.getName(), player.getName() + "_cache");

                // Force skin refresh for player
                skinsRestorerAPI.applySkin(new PlayerWrapper(player));
                return true;
            }
            catch (SkinRequestException e)
            {
                Werewolf.instance().log("[SkinsRestorer] Failed to apply skin for player");
                e.printStackTrace();
            }
            return false;
        }
    }

    private boolean updateSkinData() {
        this.lastSkinUpdateTime = System.currentTimeMillis();
        try
        {
            ClanManager clanManager = Werewolf.getClanManager();
            for (ClanManager.ClanType clanType : ClanManager.ClanType.values()) {
                skinsRestorerAPI.setSkinData(clanType.name() + "_Alpha", skinsRestorerAPI.createPlatformProperty("textures", clanManager.getWerewolfTextureForAlpha(clanType), clanManager.getWerewolfTextureSignatureForAlpha(clanType)));
                skinsRestorerAPI.setSkinData(clanType.name(), skinsRestorerAPI.createPlatformProperty("textures", clanManager.getWerewolfTextureForClan(clanType), clanManager.getWerewolfTextureSignatureForClan(clanType)));
            }
        }
        catch (NullPointerException e)
        {
            Werewolf.instance().log("[ERROR] Couldn't save Werewolf skin data to SkinsRestorer's cache...");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public SkinsRestorerFactory()
    {
        if(!Werewolf.instance().getServer().getPluginManager().getPlugin("SkinsRestorer").isEnabled())
        {
            Werewolf.instance().log("SkinsRestorer is not enabled!");
            this.enabled = false;
            return;
        }

        this.skinsRestorerAPI = SkinsRestorerAPI.getApi();
        this.enabled = updateSkinData();
    }

    @Override
    public WerewolfDisguise newDisguise(ClanManager.ClanType clanType, boolean isAlpha)
    {
        return new SkinsRestorerWerewolfDisguise(clanType, isAlpha);
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}
