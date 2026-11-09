package com.dogonfire.werewolf.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** What /ww without arguments shows */
public class InfoScreen
{
	private final Werewolf plugin;
	private final WerewolfContext ctx;

	public InfoScreen(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	public void show(Player player)
	{
		LanguageManager lang = ctx.getLanguageManager();

		if (player == null)
		{
			this.plugin.log("---------- " + this.plugin.getDescription().getFullName() + " ----------");

			lang.setAmount("" + ctx.getWerewolfManager().getNumberOfWerewolves());
			this.plugin.log(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandNumberOfWerewolves, ChatColor.AQUA));

			this.plugin.log("");

			World world = this.plugin.getServer().getWorlds().get(0);
			lang.setType(ctx.getMoonManager().getPhaseName(ctx.getMoonManager().getMoonPhase(world)));
			this.plugin.log(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandMoonPhase, ChatColor.AQUA));
			lang.setAmount(ctx.getMoonManager().getNextFullMoonText(world));
			this.plugin.log(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandNextFullMoon, ChatColor.AQUA));
			return;
		}

		player.sendMessage(ChatColor.YELLOW + "---------- " + this.plugin.getDescription().getFullName() + " ----------");
		player.sendMessage(ChatColor.AQUA + "By DogOnFire");
		player.sendMessage("" + ChatColor.AQUA);

		lang.setAmount("" + ctx.getWerewolfManager().getNumberOfWerewolves());
		player.sendMessage(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandNumberOfWerewolves, ChatColor.AQUA));
		player.sendMessage("");

		if (ctx.getWerewolfManager().isWerewolf(player))
		{
			int numberOfTransformations = ctx.getWerewolfManager().getNumberOfTransformations(player.getUniqueId());

			lang.setType(ctx.getClanManager().getClanName(player.getUniqueId()));
			lang.setAmount("Level " + numberOfTransformations);
			player.sendMessage(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandIsWerewolf, ChatColor.WHITE));
			if (this.plugin.getSettings().clans().enabled() && ctx.getClanManager().isAlpha(player.getUniqueId()))
			{
				String clanName = ctx.getClanManager().getClanName(ctx.getWerewolfManager().getWerewolfClan(player.getUniqueId())) + "s";
				lang.setType(clanName);
				player.sendMessage(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandIsWerewolfAlpha, ChatColor.WHITE));
			}

			player.sendMessage("");

			lang.setPlayerName(ctx.getWerewolfManager().getWerewolfName(player.getUniqueId()));
			player.sendMessage(lang.getLanguageString(LANGUAGESTRING.YourWerewolfName, ChatColor.AQUA));

			player.sendMessage("");

			if (numberOfTransformations >= this.plugin.getSettings().maturity().intent())
			{
				lang.setType("/ww intent");
				LANGUAGESTRING intentString = ctx.getWerewolfManager().hasIntent(player.getUniqueId()) ? LANGUAGESTRING.WerewolfCommandIntentOn : LANGUAGESTRING.WerewolfCommandIntentOff;
				player.sendMessage(lang.getLanguageString(intentString, ChatColor.WHITE));
			}

			if (numberOfTransformations >= this.plugin.getSettings().maturity().noDropItems())
			{
				player.sendMessage(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandNoDropItems, ChatColor.WHITE));
			}

			if (numberOfTransformations >= this.plugin.getSettings().maturity().controlledTransformation())
			{
				player.sendMessage(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandControlledTransformation, ChatColor.WHITE));
			}

			if (numberOfTransformations >= this.plugin.getSettings().maturity().goldImmunity())
			{
				player.sendMessage(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandGoldImmunity, ChatColor.WHITE));
			}
		}
		else
		{
			player.sendMessage(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED));
		}
		player.sendMessage("");

		lang.setType(ctx.getMoonManager().getPhaseName(ctx.getMoonManager().getMoonPhase(player.getWorld())));
		player.sendMessage(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandMoonPhase, ChatColor.AQUA));
		lang.setAmount(ctx.getMoonManager().getNextFullMoonText(player.getWorld()));
		player.sendMessage(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandNextFullMoon, ChatColor.AQUA));

		player.sendMessage("");

		if (ctx.getHuntManager() != null && ctx.getHuntManager().getBounty() > 0)
		{
			lang.setAmount("" + ctx.getHuntManager().getBounty());
			player.sendMessage(lang.getLanguageString(LANGUAGESTRING.CurrentBounty, ChatColor.AQUA));

			player.sendMessage("");
		}

		if (this.plugin.getSettings().clans().enabled() && ctx.getWerewolfManager().isWerewolf(player))
		{
			lang.setType("/ww clan");
			player.sendMessage(lang.getLanguageString(LANGUAGESTRING.HelpClan, ChatColor.AQUA));
		}

		lang.setType("/ww help");
		player.sendMessage(lang.getLanguageString(LANGUAGESTRING.WerewolfCommandListCommands, ChatColor.AQUA));

		this.plugin.log(player.getName() + ": /werewolf");
	}
}
