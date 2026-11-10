package com.dogonfire.werewolf.integrations;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import com.clanjhoo.vampire.VampireAPI;
import com.clanjhoo.vampire.entity.VPlayer;
import com.clanjhoo.vampire.event.InfectionChangeEvent;
import com.clanjhoo.vampire.event.VampireTypeChangeEvent;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.api.event.WerewolfInfectionEvent;
import com.dogonfire.werewolf.managers.LanguageManager;

/**
 * VampireRevamp: nobody can be both a werewolf and a vampire, and silver armor's purity is the chance a human shakes off
 * a new vampire infection. Only registered when VampireRevamp is enabled.
 */
public class VampireIntegration implements IntegrationListener
{
	private final WerewolfContext	ctx;
	private final Random	random	= new Random();

	public VampireIntegration(WerewolfContext ctx)
	{
		this.ctx = ctx;
	}

	/*
	 * Werewolf blood rejects vampirism: any vampire infection of a werewolf (infected or full) is set back to zero.
	 * The change goes through rather than being cancelled, so a werewolf that somehow picked up some infection is cleared.
	 * InfectionChangeEvent has setCancelled but doesn't implement Cancellable, so ignoreCancelled can't be used.
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onVampireInfection(InfectionChangeEvent event)
	{
		if (event.isCancelled() || !ctx.isWerewolvesEnabled() || event.getInfection() <= 0)
		{
			return;
		}

		VPlayer vPlayer = event.getVPlayer();
		if (vPlayer == null || vPlayer.getUuid() == null || !ctx.getWerewolfManager().isWerewolf(vPlayer.getUuid()))
		{
			return;
		}

		boolean newInfection = vPlayer.getInfection() <= 0;
		event.setInfection(0);

		Player player = vPlayer.getPlayer();
		if (newInfection && player != null)
		{
			player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfResistsVampirism, ChatColor.LIGHT_PURPLE));
		}
	}

	// Catches anything that turns a werewolf straight into a vampire without going through infection (e.g. commands)
	@EventHandler(priority = EventPriority.LOW)
	public void onVampireTypeChange(VampireTypeChangeEvent event)
	{
		if (event.isCancelled() || !ctx.isWerewolvesEnabled() || !event.isVampire())
		{
			return;
		}

		VPlayer vPlayer = event.getVPlayer();
		if (vPlayer != null && vPlayer.getUuid() != null && ctx.getWerewolfManager().isWerewolf(vPlayer.getUuid()))
		{
			event.setCancelled(true);
		}
	}

	// Vampires can't get the werewolf infection, whatever its source
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onWerewolfInfection(WerewolfInfectionEvent event)
	{
		if (isVampire(event.getPlayer()))
		{
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onSilverArmorCleanse(InfectionChangeEvent event)
	{
		if (event.isCancelled() || !ctx.isWerewolvesEnabled())
		{
			return;
		}

		VPlayer vPlayer = event.getVPlayer();
		Player player = vPlayer != null ? vPlayer.getPlayer() : null;
		if (player == null || vPlayer.isVampire() || !ctx.getWerewolfManager().isHuman(player))
		{
			return;
		}

		// Only a new infection is rolled against, not the infection growing or being cured afterwards
		if (vPlayer.getInfection() > 0 || event.getInfection() <= 0)
		{
			return;
		}

		int purity = ctx.getItemManager().getSilverArmorPurity(player);
		if (purity > 0 && this.random.nextInt(100) < purity)
		{
			event.setCancelled(true);
			player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverArmorCleansed, ChatColor.AQUA));
		}
	}

	private boolean isVampire(Player player)
	{
		try
		{
			return VampireAPI.isVampire(player);
		}
		catch (AssertionError e)
		{
			// VampireRevamp hasn't loaded this player's data yet
			return false;
		}
	}
}
