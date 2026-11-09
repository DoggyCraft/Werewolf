package com.dogonfire.werewolf.tasks;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;

public class InfoTask implements Runnable
{
	private final Werewolf plugin;
	private final WerewolfContext ctx;
	private UUID playerId = null;
	private String name = null;
	private LanguageManager.LANGUAGESTRING message = null;
	private int amount = 0;
	private ChatColor color;

	public InfoTask(WerewolfContext ctx, ChatColor color, UUID playerId, LanguageManager.LANGUAGESTRING m, int amount, String name)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.playerId = playerId;
		this.message = m;
		this.name = name;
		this.amount = amount;
		this.color = color;
	}

	public void run()
	{
		Player player = this.plugin.getServer().getPlayer(this.playerId);
		
		if (player == null)
		{
			return;
		}
		
		ctx.getLanguageManager().setPlayerName(this.name);
				
		ctx.getLanguageManager().setAmount("" + this.amount);

		String questionMessage = ctx.getLanguageManager().getLanguageString(this.message, this.color);

		player.sendMessage(

		this.color + questionMessage);
	}
}