package com.dogonfire.werewolf.listeners;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfAPI;
import com.dogonfire.werewolf.events.NewAlphaEvent;
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

	public AlphaListener(Werewolf plugin)
	{
		this.plugin = plugin;
	}

	/*
	 * An alpha slain by a member of their own clan loses the alpha to the killer. An alpha slain by a human hunter (when
	 * hunting is enabled) loses it to the runner-up of the clan. LOW, so it runs before PlayerListener may cure the alpha.
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onAlphaDeath(PlayerDeathEvent event)
	{
		if (!Werewolf.pluginEnabled || !this.plugin.getSettings().clans().enabled())
		{
			return;
		}

		Player victim = event.getEntity();
		Player killer = victim.getKiller();
		WerewolfManager werewolfManager = Werewolf.getWerewolfManager();
		if (killer == null || killer.equals(victim) || !werewolfManager.isWerewolf(victim))
		{
			return;
		}

		ClanManager.ClanType clan = werewolfManager.getWerewolfClan(victim.getUniqueId());
		if (!Werewolf.getClanManager().isAlpha(clan, victim.getUniqueId()))
		{
			return;
		}

		LanguageManager languageManager = Werewolf.getLanguageManager();

		if (werewolfManager.isWerewolf(killer))
		{
			if (werewolfManager.getWerewolfClan(killer.getUniqueId()) == clan && Werewolf.getClanManager().assignAlphaInClan(clan, killer.getUniqueId()))
			{
				languageManager.setPlayerName(killer.getName());
				werewolfManager.sendMessageToClan(clan, languageManager.getLanguageString(LanguageManager.LANGUAGESTRING.NewClanAlpha, ChatColor.WHITE));
			}
		}
		else if (this.plugin.getSettings().hunting().enabled() && WerewolfAPI.isHuman(killer))
		{
			UUID runnerUp = Werewolf.getClanManager().getRunnerUp(clan);
			if (runnerUp != null && Werewolf.getClanManager().assignAlphaInClan(clan, runnerUp))
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
		Werewolf.getWerewolfManager().refreshWerewolfSkinLater(event.getOldAlphaId());
		Werewolf.getWerewolfManager().refreshWerewolfSkinLater(event.getNewAlphaId());
	}
}
