package com.dogonfire.werewolf.managers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Set;


import java.util.UUID;

import com.dogonfire.werewolf.tasks.CompassTrackerTask;
import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.storage.HunterRecord;
import com.dogonfire.werewolf.storage.HunterRepository;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class HuntManager
{
	private Werewolf plugin;
	private int					totalBounty			= 0;
	private final HunterRepository	hunters;
	private Random				random				= new Random();

	public HuntManager(Werewolf p, HunterRepository hunters)
	{
		this.plugin = p;
		this.hunters = hunters;
	}

	public boolean isHunting(UUID playerId)
	{
		return CompassTrackerTask.hasWatcher(playerId);
	}

	public void setHunting(UUID playerId, boolean hunting)
	{
		if (hunting)
		{
			CompassTrackerTask.addWatcher(playerId);
		}
		else
		{
			CompassTrackerTask.removeWatcher(playerId);
		}
	}

	public void addBounty(String playerName, int bounty)
	{
		Player player = this.plugin.getServer().getPlayer(playerName);
		if (!Werewolf.getEconomy().has(player, bounty))
		{
			player.sendMessage(ChatColor.RED + "You do not have that much.");
			return;
		}
		Werewolf.getEconomy().withdrawPlayer(player, bounty);
		this.totalBounty += bounty;

		Werewolf.getLanguageManager().setPlayerName(playerName);
		Werewolf.getLanguageManager().setAmount(Werewolf.getEconomy().format(bounty));
		this.plugin.getServer().broadcastMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.BountyPlayerAdded, ChatColor.AQUA));

		Werewolf.getLanguageManager().setAmount(getFormattedBounty());
		this.plugin.getServer().broadcastMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.BountyTotal, ChatColor.AQUA));

		this.plugin.log(playerName + " added " + Werewolf.getEconomy().format(bounty) + " to the Werewolf bounty. Bounty is now " + Werewolf.getEconomy().format(this.totalBounty));
	}

	public void autoAddBounty()
	{
		if (this.random.nextInt(100 + this.totalBounty / 5) > 0)
		{
			return;
		}
		
		if (this.totalBounty >= this.plugin.getSettings().general().autoBountyMaximum())
		{
			return;
		}
		
		if (Werewolf.getWerewolfManager().getOnlineWerewolvesInWolfForm().size() == 0)
		{
			return;
		}
		
		int bounty = 10 + this.random.nextInt(10) * 10;

		this.totalBounty += bounty;
		if (this.totalBounty >= this.plugin.getSettings().general().autoBountyMaximum())
		{
			this.totalBounty = this.plugin.getSettings().general().autoBountyMaximum();
		}
		Werewolf.getLanguageManager().setAmount(Werewolf.getEconomy().format(bounty));
		this.plugin.getServer().broadcastMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.BountyServerAdded, ChatColor.AQUA));

		Werewolf.getLanguageManager().setAmount(getFormattedBounty());
		this.plugin.getServer().broadcastMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.BountyTotal, ChatColor.AQUA));
	}

	public int getBounty()
	{
		return this.totalBounty;
	}

	public String getFormattedBounty()
	{
		return Werewolf.getEconomy().format(this.totalBounty);
	}

	public void handleKill(UUID killerId, String killerName)
	{
		if(Werewolf.getWerewolfManager().isWerewolf(killerId))
		{
			return;
		}
		
		// Hunt mode (/ww hunt) or a tracker locked onto a target both count as hunting
		if(!Werewolf.getHuntManager().isHunting(killerId) && !Werewolf.getTrackerManager().hasTarget(killerId))
		{
			return;
		}

		String message;
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		
		if (this.totalBounty > 0)
		{
			Werewolf.getLanguageManager().setPlayerName(killerName);
			Werewolf.getLanguageManager().setAmount(Werewolf.getEconomy().format(this.totalBounty));
			message = Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.KilledWerewolfBounty, ChatColor.AQUA);
		}
		else
		{
			message = Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.KilledWerewolfNoBounty, ChatColor.AQUA);
		}
		
		this.plugin.getServer().broadcastMessage(message);
		this.plugin.log(message);

		Player killer = this.plugin.getServer().getPlayer(killerName);
		Werewolf.getEconomy().depositPlayer(killer, this.totalBounty);
		this.totalBounty = 0;

		HunterRecord hunter = this.hunters.getOrCreate(killerName);
		hunter.setKills(hunter.getKills() + 1);
		hunter.setLastKill(formatter.format(thisDate));
	}

	public int getHunterKills(String hunterName)
	{
		HunterRecord hunter = this.hunters.get(hunterName);
		return hunter != null ? hunter.getKills() : 0;
	}

	public Set<String> getHunters()
	{
		return this.hunters.names();
	}
}
