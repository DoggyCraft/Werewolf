package com.dogonfire.werewolf.listeners;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.clanjhoo.vampire.entity.VPlayer;
import com.clanjhoo.vampire.event.InfectionChangeEvent;
import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfAPI;
import com.dogonfire.werewolf.managers.LanguageManager;

/**
 * Silver armor against VampireRevamp: the armor's purity is the chance a human shakes off a new vampire infection.
 * Only registered when VampireRevamp is enabled, since it loads that plugin's classes.
 */
public class VampireHuntingListener implements Listener
{
	private final Random	random	= new Random();

	// InfectionChangeEvent has setCancelled but doesn't implement Cancellable, so ignoreCancelled can't be used
	@EventHandler
	public void onVampireInfection(InfectionChangeEvent event)
	{
		if (event.isCancelled() || !Werewolf.pluginEnabled)
		{
			return;
		}

		VPlayer vPlayer = event.getVPlayer();
		Player player = vPlayer != null ? vPlayer.getPlayer() : null;
		if (player == null || vPlayer.isVampire() || !WerewolfAPI.isHuman(player))
		{
			return;
		}

		// Only a new infection is rolled against, not the infection growing or being cured afterwards
		if (vPlayer.getInfection() > 0 || event.getInfection() <= 0)
		{
			return;
		}

		int purity = Werewolf.getItemManager().getSilverArmorPurity(player);
		if (purity > 0 && this.random.nextInt(100) < purity)
		{
			event.setCancelled(true);
			player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverArmorCleansed, ChatColor.AQUA));
		}
	}
}
