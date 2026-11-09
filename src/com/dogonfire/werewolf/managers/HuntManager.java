package com.dogonfire.werewolf.managers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.storage.HunterRecord;
import com.dogonfire.werewolf.storage.HunterRepository;
import com.dogonfire.werewolf.tasks.CompassTrackerTask;

import net.milkbowl.vault.economy.Economy;

/**
 * Werewolf hunting and the bounty. Only exists when Vault is installed. The economy provider is looked up on every use
 * (it may enable after us), and without one bounties can't be paid in or out but hunting still counts kills.
 */
public class HuntManager
{
	private final Werewolf				plugin;
	private final WerewolfContext		ctx;
	private final HunterRepository		hunters;
	private final CompassTrackerTask	compassTracker;
	private final Random				random		= new Random();
	private int							totalBounty	= 0;
	private boolean						warnedNoEconomy;

	public HuntManager(WerewolfContext ctx, HunterRepository hunters)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.hunters = hunters;
		this.compassTracker = new CompassTrackerTask(ctx);
	}

	/** onDisable: stops the hunters' compass updates */
	public void stop()
	{
		this.compassTracker.stop();
	}

	public boolean isHunting(UUID playerId)
	{
		return this.compassTracker.hasWatcher(playerId);
	}

	public void setHunting(UUID playerId, boolean hunting)
	{
		if (hunting)
		{
			this.compassTracker.addWatcher(playerId);
		}
		else
		{
			this.compassTracker.removeWatcher(playerId);
		}
	}

	// Null (and logged once) when no economy plugin has registered with Vault
	private Economy economy()
	{
		Economy economy = this.ctx.getEconomy();
		if (economy == null && !this.warnedNoEconomy)
		{
			this.warnedNoEconomy = true;
			this.plugin.getLogger().warning("Vault has no economy provider; werewolf bounties are not paid in or out.");
		}
		else if (economy != null)
		{
			this.warnedNoEconomy = false;
		}
		return economy;
	}

	private String format(Economy economy, int amount)
	{
		return economy != null ? economy.format(amount) : String.valueOf(amount);
	}

	public void addBounty(String playerName, int bounty)
	{
		Player player = this.plugin.getServer().getPlayer(playerName);
		if (player == null)
		{
			return;
		}

		Economy economy = economy();
		if (economy == null)
		{
			player.sendMessage(this.ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.EconomyUnavailable, ChatColor.RED));
			return;
		}

		if (!economy.has(player, bounty))
		{
			player.sendMessage(ChatColor.RED + "You do not have that much.");
			return;
		}
		economy.withdrawPlayer(player, bounty);
		this.totalBounty += bounty;

		LanguageManager lang = this.ctx.getLanguageManager();
		lang.setPlayerName(playerName);
		lang.setAmount(economy.format(bounty));
		this.plugin.getServer().broadcastMessage(lang.getLanguageString(LanguageManager.LANGUAGESTRING.BountyPlayerAdded, ChatColor.AQUA));

		lang.setAmount(getFormattedBounty());
		this.plugin.getServer().broadcastMessage(lang.getLanguageString(LanguageManager.LANGUAGESTRING.BountyTotal, ChatColor.AQUA));

		this.plugin.log(playerName + " added " + economy.format(bounty) + " to the Werewolf bounty. Bounty is now " + economy.format(this.totalBounty));
	}

	public void autoAddBounty()
	{
		if (this.random.nextInt(100 + this.totalBounty / 5) > 0)
		{
			return;
		}

		int maximum = this.plugin.getSettings().general().autoBountyMaximum();
		if (this.totalBounty >= maximum)
		{
			return;
		}

		if (this.ctx.getWerewolfManager().getOnlineWerewolvesInWolfForm().size() == 0)
		{
			return;
		}

		// Nobody could be paid
		Economy economy = economy();
		if (economy == null)
		{
			return;
		}

		int bounty = 10 + this.random.nextInt(10) * 10;

		this.totalBounty = Math.min(this.totalBounty + bounty, maximum);

		LanguageManager lang = this.ctx.getLanguageManager();
		lang.setAmount(economy.format(bounty));
		this.plugin.getServer().broadcastMessage(lang.getLanguageString(LanguageManager.LANGUAGESTRING.BountyServerAdded, ChatColor.AQUA));

		lang.setAmount(getFormattedBounty());
		this.plugin.getServer().broadcastMessage(lang.getLanguageString(LanguageManager.LANGUAGESTRING.BountyTotal, ChatColor.AQUA));
	}

	public int getBounty()
	{
		return this.totalBounty;
	}

	public String getFormattedBounty()
	{
		return format(this.ctx.getEconomy(), this.totalBounty);
	}

	public void handleKill(UUID killerId, String killerName)
	{
		if (this.ctx.getWerewolfManager().isWerewolf(killerId))
		{
			return;
		}

		// Hunt mode (/ww hunt) or a tracker locked onto a target both count as hunting
		if (!isHunting(killerId) && !this.ctx.getTrackerManager().hasTarget(killerId))
		{
			return;
		}

		DateFormat formatter = new SimpleDateFormat("HH:mm dd-MM-yyyy");
		LanguageManager lang = this.ctx.getLanguageManager();
		Economy economy = this.totalBounty > 0 ? economy() : null;
		Player killer = this.plugin.getServer().getPlayer(killerId);

		// Without an economy (or the killer) the bounty can't be paid, so it stays for the next kill
		String message;
		if (economy != null && killer != null)
		{
			lang.setPlayerName(killerName);
			lang.setAmount(economy.format(this.totalBounty));
			message = lang.getLanguageString(LanguageManager.LANGUAGESTRING.KilledWerewolfBounty, ChatColor.AQUA);
			economy.depositPlayer(killer, this.totalBounty);
			this.totalBounty = 0;
		}
		else
		{
			message = lang.getLanguageString(LanguageManager.LANGUAGESTRING.KilledWerewolfNoBounty, ChatColor.AQUA);
		}

		this.plugin.getServer().broadcastMessage(message);
		this.plugin.log(message);

		HunterRecord hunter = this.hunters.getOrCreate(killerName);
		hunter.setKills(hunter.getKills() + 1);
		hunter.setLastKill(formatter.format(new Date()));
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
