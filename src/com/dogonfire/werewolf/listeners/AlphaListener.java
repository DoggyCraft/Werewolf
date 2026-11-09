package com.dogonfire.werewolf.listeners;

import com.dogonfire.werewolf.api.Clan;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.api.event.NewAlphaEvent;
import com.dogonfire.werewolf.managers.ClanManager;
import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.managers.WerewolfManager;

/**
 * Alpha succession on death, and swapping the alpha skin when a clan's alpha changes. Empty alpha slots are filled by
 * {@link ClanManager#checkAlphas()} from the main loop.
 */
public class AlphaListener implements Listener
{
	private final Werewolf	plugin;
	private final WerewolfContext ctx;

	public AlphaListener(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	/*
	 * An alpha slain by a member of their own clan loses the alpha to the killer. An alpha slain by a human hunter (when
	 * hunting is enabled) loses it to the runner-up of the clan. LOW, so it runs before PlayerListener may cure the alpha.
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onAlphaDeath(PlayerDeathEvent event)
	{
		if (!ctx.isWerewolvesEnabled() || !this.plugin.getSettings().clans().enabled())
		{
			return;
		}

		Player victim = event.getEntity();
		Player killer = victim.getKiller();
		WerewolfManager werewolfManager = ctx.getWerewolfManager();
		if (killer == null || killer.equals(victim) || !werewolfManager.isWerewolf(victim))
		{
			return;
		}

		Clan clan = werewolfManager.getWerewolfClan(victim.getUniqueId());
		if (!ctx.getClanManager().isAlpha(clan, victim.getUniqueId()))
		{
			return;
		}

		LanguageManager languageManager = ctx.getLanguageManager();

		if (werewolfManager.isWerewolf(killer))
		{
			if (werewolfManager.getWerewolfClan(killer.getUniqueId()) == clan && ctx.getClanManager().assignAlphaInClan(clan, killer.getUniqueId()))
			{
				languageManager.setPlayerName(killer.getName());
				werewolfManager.sendMessageToClan(clan, languageManager.getLanguageString(LanguageManager.LANGUAGESTRING.NewClanAlpha, ChatColor.WHITE));
			}
		}
		else if (this.plugin.getSettings().hunting().enabled() && ctx.getWerewolfManager().isHuman(killer))
		{
			UUID runnerUp = ctx.getClanManager().getRunnerUp(clan);
			if (runnerUp != null && ctx.getClanManager().assignAlphaInClan(clan, runnerUp))
			{
				languageManager.setPlayerName(killer.getName());
				werewolfManager.sendMessageToClan(clan, languageManager.getLanguageString(LanguageManager.LANGUAGESTRING.AlphaSlainByHunter, ChatColor.WHITE));

				languageManager.setPlayerName(werewolfManager.getPlayerName(runnerUp));
				werewolfManager.sendMessageToClan(clan, languageManager.getLanguageString(LanguageManager.LANGUAGESTRING.NewClanAlpha, ChatColor.WHITE));
			}
		}
	}

	// The alpha has its own skin: swap skins for the old and new alpha if they are in wolf form, once the change is stored
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onNewAlpha(NewAlphaEvent event)
	{
		ctx.getWerewolfManager().refreshWerewolfSkinLater(event.getOldAlphaId());
		ctx.getWerewolfManager().refreshWerewolfSkinLater(event.getNewAlphaId());
	}
}
