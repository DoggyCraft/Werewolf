package com.dogonfire.werewolf.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.events.WerewolfCureEvent;
import com.dogonfire.werewolf.events.WerewolfInfectionEvent;
import com.dogonfire.werewolf.events.WerewolfTransformEvent;
import com.dogonfire.werewolf.managers.ClanManager;
import com.dogonfire.werewolf.managers.ItemManager;
import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.managers.MoonManager;
import com.dogonfire.werewolf.managers.ScentManager;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.StringUtil;

import com.dogonfire.werewolf.managers.ClanManager.ClanType;

public class Commands implements TabExecutor
{
	private Werewolf plugin;

	public Commands(Werewolf p)
	{
		this.plugin = p;
	}

	public boolean commandInfo(Player player)
	{
		if (player == null)
		{
			this.plugin.log("---------- " + this.plugin.getDescription().getFullName() + " ----------");

			Werewolf.getLanguageManager().setAmount("" + Werewolf.getWerewolfManager().getNumberOfWerewolves());

			this.plugin.log(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandNumberOfWerewolves, ChatColor.AQUA));

			this.plugin.log("");

			World world = plugin.getServer().getWorlds().get(0);
			Werewolf.getLanguageManager().setType(Werewolf.getMoonManager().getPhaseName(Werewolf.getMoonManager().getMoonPhase(world)));
			this.plugin.log(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandMoonPhase, ChatColor.AQUA));
			Werewolf.getLanguageManager().setAmount(Werewolf.getMoonManager().getNextFullMoonText(world));
			this.plugin.log(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandNextFullMoon, ChatColor.AQUA));

		}
		else
		{
			player.sendMessage(ChatColor.YELLOW + "---------- " + this.plugin.getDescription().getFullName() + " ----------");
			player.sendMessage(ChatColor.AQUA + "By DogOnFire");
			// player.sendMessage(ChatColor.AQUA +
			// Werewolf.getLanguageManager().getLanguageName() + " by " +
			// Werewolf.getLanguageManager().getAuthor());
			player.sendMessage("" + ChatColor.AQUA);

			Werewolf.getLanguageManager().setAmount("" + Werewolf.getWerewolfManager().getNumberOfWerewolves());
			player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandNumberOfWerewolves, ChatColor.AQUA));
			player.sendMessage("");

			if (Werewolf.getWerewolfManager().isWerewolf(player))
			{
				int numberOfTransformations = Werewolf.getWerewolfManager().getNumberOfTransformations(player.getUniqueId());

				Werewolf.getLanguageManager().setType(Werewolf.getClanManager().getClanName(player.getUniqueId()));
				Werewolf.getLanguageManager().setAmount("Level " + Werewolf.getWerewolfManager().getNumberOfTransformations(player.getUniqueId()));
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandIsWerewolf, ChatColor.WHITE));
				if (this.plugin.useClans && Werewolf.getClanManager().isAlpha(player.getUniqueId()))
				{
					String clanName = Werewolf.getClanManager().getClanName(Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId())) + "s";
					Werewolf.getLanguageManager().setType(clanName);
					player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandIsWerewolfAlpha, ChatColor.WHITE));
				}

				player.sendMessage("");

				Werewolf.getLanguageManager().setPlayerName(Werewolf.getWerewolfManager().getWerewolfName(player.getUniqueId()));
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YourWerewolfName, ChatColor.AQUA));

				player.sendMessage("");

				if (numberOfTransformations >= this.plugin.levelForIntent)
				{
					Werewolf.getLanguageManager().setType("/ww intent");
					LanguageManager.LANGUAGESTRING intentString = Werewolf.getWerewolfManager().hasIntent(player.getUniqueId()) ? LanguageManager.LANGUAGESTRING.WerewolfCommandIntentOn : LanguageManager.LANGUAGESTRING.WerewolfCommandIntentOff;
					player.sendMessage(Werewolf.getLanguageManager().getLanguageString(intentString, ChatColor.WHITE));
				}

				if (numberOfTransformations >= this.plugin.transformsForNoDropItems)
				{
					player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandNoDropItems, ChatColor.WHITE));
				}

				if (numberOfTransformations >= this.plugin.transformsForControlledTransformation)
				{
					player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandControlledTransformation, ChatColor.WHITE));
				}

				if (numberOfTransformations >= this.plugin.transformsForGoldImmunity)
				{
					player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandGoldImmunity, ChatColor.WHITE));
				}
			}
			else
			{
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED));
			}
			player.sendMessage("");

			Werewolf.getLanguageManager().setType(Werewolf.getMoonManager().getPhaseName(Werewolf.getMoonManager().getMoonPhase(player.getWorld())));
			player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandMoonPhase, ChatColor.AQUA));
			Werewolf.getLanguageManager().setAmount(Werewolf.getMoonManager().getNextFullMoonText(player.getWorld()));
			player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandNextFullMoon, ChatColor.AQUA));

			player.sendMessage("");

			if (Werewolf.getHuntManager() != null && Werewolf.getHuntManager().getBounty() > 0)
			{
				Werewolf.getLanguageManager().setAmount("" + Werewolf.getHuntManager().getBounty());
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CurrentBounty, ChatColor.AQUA));

				player.sendMessage("");
			}

			if (plugin.useClans && Werewolf.getWerewolfManager().isWerewolf(player))
			{
				Werewolf.getLanguageManager().setType("/ww clan");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpClan, ChatColor.AQUA));
			}

			Werewolf.getLanguageManager().setType("/ww help");
			player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandListCommands, ChatColor.AQUA));

			this.plugin.log(player.getName() + ": /werewolf");
		}
		return true;
	}

	public void commandReload(Player player)
	{
		if (player == null)
		{
			this.plugin.reloadSettings();
			this.plugin.log(this.plugin.getDescription().getFullName() + ": Reloaded configuration.");
		}
		else if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.reload")))
		{
			this.plugin.reloadSettings();
			player.sendMessage(ChatColor.YELLOW + this.plugin.getDescription().getFullName() + ": " + ChatColor.WHITE + "Reloaded configuration.");
		}
	}

	public void commandSave(Player player)
	{
		if (player == null)
		{
			Werewolf.getClanManager().save();
			Werewolf.getWerewolfManager().save();
			this.plugin.log(this.plugin.getDescription().getFullName() + ": Saved configuration(s).");
		}
		else if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.save")))
		{
			Werewolf.getClanManager().save();
			Werewolf.getWerewolfManager().save();
			player.sendMessage(ChatColor.YELLOW + this.plugin.getDescription().getFullName() + ": " + ChatColor.WHITE + "Saved configuration(s).");
		}
	}

	public boolean commandHelp(Player player)
	{
		if (player == null)
		{
			this.plugin.log("/werewolf - Show basic info");
			this.plugin.log("/howl - Howl as a Werewolf!");
			this.plugin.log("/growl - Growl as a Werewolf!");
			this.plugin.log("/werewolf top - View the top Werewolf hunters in " + this.plugin.serverName);
			this.plugin.log("/werewolf hunt - Toggles Werewolf hunt mode");
			this.plugin.log("/werewolf check <playername> - Check Werewolf status for a player");
			this.plugin.log("/werewolf bounty - Check the current bounty for killing a Werewolf");
			this.plugin.log("/werewolf addbounty - Add to the bounty for killing a Werewolf");
			this.plugin.log("/werewolf toggle - Toggles Werewolf status for yourself");
			this.plugin.log("/werewolf toggle <playername> - Toggles Werewolf status for another player");
			this.plugin.log("/werewolf infect <playername> [clan] - Infects a player with the Werewolf infection");
			this.plugin.log("/werewolf cure <playername> - Cures a player of being a Werewolf");
			this.plugin.log("/werewolf setlevel <playername> <level> - Sets a Werewolf's level");
			this.plugin.log("/werewolf addlevel <playername> <amount> - Adds levels to a Werewolf");
			this.plugin.log("/werewolf setalpha <playername> - Makes a Werewolf the alpha of their clan");
			this.plugin.log("/werewolf setphase <phase> [world] - Sets the moon phase of a world");
		}
		else
		{
			player.sendMessage(ChatColor.YELLOW + "---------- " + this.plugin.getDescription().getFullName() + " ----------");
			Werewolf.getLanguageManager().setType("/werewolf help");
			player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpWerewolf, ChatColor.AQUA));
			if (player.isOp() || Werewolf.getPermissionsManager().hasPermission(player, "werewolf.hunt"))
			{
				Werewolf.getLanguageManager().setType("/ww hunt");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpHunt, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.top")))
			{
				Werewolf.getLanguageManager().setType("/ww top");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpTop, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.bounty")))
			{
				Werewolf.getLanguageManager().setType("/ww bounty");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpBounty, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.addbounty")))
			{
				Werewolf.getLanguageManager().setType("/ww addbounty <amount>");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpAddBounty, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.transform")))
			{
				Werewolf.getLanguageManager().setType("/ww transform");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpTransform, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.intent")))
			{
				Werewolf.getLanguageManager().setType("/ww intent");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpIntent, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.setphase")))
			{
				Werewolf.getLanguageManager().setType("/ww setphase <phase> [world]");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpSetPhase, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.check")))
			{
				Werewolf.getLanguageManager().setType("/ww check <playername>");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpCheck, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.infectself")))
			{
				Werewolf.getLanguageManager().setType("/ww infect");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpInfectSelf, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.infect")))
			{
				Werewolf.getLanguageManager().setType("/ww infect <playername> [clan]");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpInfectOther, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.cureother")))
			{
				Werewolf.getLanguageManager().setType("/ww cure <playername>");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpCureOther, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.setlevel")))
			{
				Werewolf.getLanguageManager().setType("/ww setlevel <playername> <level>");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpSetLevel, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.addlevel")))
			{
				Werewolf.getLanguageManager().setType("/ww addlevel <playername> <amount>");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpAddLevel, ChatColor.AQUA));
			}
			if (this.plugin.useClans && (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.setalpha"))))
			{
				Werewolf.getLanguageManager().setType("/ww setalpha <playername>");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpSetAlpha, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.togglewerewolf")))
			{
				Werewolf.getLanguageManager().setType("/ww toggle <playername>");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpToggleOther, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.togglewerewolfself")))
			{
				Werewolf.getLanguageManager().setType("/ww toggle");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpToggleSelf, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.potion.infection.create")))
			{
				Werewolf.getLanguageManager().setType("/ww infectionpotion");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpInfectionPotion, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.potion.cure.create")))
			{
				Werewolf.getLanguageManager().setType("/ww curepotion");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpCurePotion, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.potion.wolfbane.create")))
			{
				Werewolf.getLanguageManager().setType("/ww wolfbane");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpWolfbanePotion, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.silversword.create")))
			{
				Werewolf.getLanguageManager().setType("/ww silversword");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpSilverSword, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.lorebook.create")))
			{
				Werewolf.getLanguageManager().setType("/ww lorebook");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpLoreBook, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.silverarmor.create")))
			{
				Werewolf.getLanguageManager().setType("/ww silverarmor");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpSilverArmor, ChatColor.AQUA));
			}
			if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.tracker.create")))
			{
				Werewolf.getLanguageManager().setType(this.plugin.vampireEnabled ? "/ww tracker|vampiretracker" : "/ww tracker");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpTracker, ChatColor.AQUA));
			}
			if (Werewolf.getWerewolfManager().isWerewolf(player) && (player.isOp() || Werewolf.getPermissionsManager().hasPermission(player, "werewolf.track")))
			{
				Werewolf.getLanguageManager().setType("/ww track");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpTrack, ChatColor.AQUA));
			}
			if (this.plugin.useClans)
			{
				if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.clan")))
				{
					Werewolf.getLanguageManager().setType("/ww clan");
					player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpClan, ChatColor.AQUA));
				}
				if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.home")))
				{
					Werewolf.getLanguageManager().setType("/ww home");
					player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpHome, ChatColor.AQUA));
				}

				if (Werewolf.getClanManager().isAlpha(player.getUniqueId()))
				{
					if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.alpha.call")))
					{
						Werewolf.getLanguageManager().setType("/ww call");
						player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpCall, ChatColor.AQUA));
					}
					if (player.isOp() || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.sethome")))
					{
						Werewolf.getLanguageManager().setType("/ww sethome");
						player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpSetHome, ChatColor.AQUA));
					}
				}
			}
			this.plugin.log(player.getName() + ": /werewolf help");
		}
		return true;
	}

	private boolean commandList(CommandSender sender)
	{
		if (sender == null)
		{
			return false;
		}
		if ((!sender.isOp()) && (!Werewolf.getPermissionsManager().hasPermission((Player) sender, "werewolf.list")))
		{
			sender.sendMessage(ChatColor.RED + "You do not have permission for that");
			return false;
		}
		if (!Werewolf.getWerewolfManager().isWerewolf((Player) sender))
		{
			sender.sendMessage(ChatColor.RED + "You are not a werewolf");
			return false;
		}

		Player player = (Player) sender;

		ClanManager.ClanType clan = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());

		List<UUID> list = Werewolf.getWerewolfManager().getWerewolfClanMembersRanked(clan);

		sender.sendMessage(ChatColor.GOLD + "--------- The Members of " + Werewolf.getClanManager().getClanName(clan) + " ---------");
		if (list.size() > 15)
		{
			list = list.subList(0, 15);
		}

		int n = 1;

		boolean playerShown = false;

		for (UUID memberId : list)
		{
			String memberName = plugin.getServer().getOfflinePlayer(memberId).getName();

			int numberOfTransformations = Werewolf.getWerewolfManager().getNumberOfTransformations(memberId);
			if (memberName.equals(sender.getName()))
			{
				playerShown = true;
				sender.sendMessage(ChatColor.GOLD +

						String.format("%2d", new Object[] { Integer.valueOf(n) }) + " - " +

						memberName + ChatColor.GOLD + new StringBuilder().append("      level ").append(numberOfTransformations).toString());
			}
			else
			{
				sender.sendMessage(ChatColor.YELLOW + String.format("%2d", new Object[] { Integer.valueOf(n) }) + ChatColor.AQUA + " - " + memberName + ChatColor.GOLD + new StringBuilder().append("      level ").append(numberOfTransformations).toString());
			}
			n++;
		}

		n = 1;

		if (!playerShown)
		{
			for (UUID memberId : list)
			{
				String memberName = plugin.getServer().getOfflinePlayer(memberId).getName();

				int numberOfTransformations = Werewolf.getWerewolfManager().getNumberOfTransformations(memberId);

				if (memberName.equals(sender.getName()))
				{
					playerShown = true;
					sender.sendMessage("" + ChatColor.GOLD + n + " - " + memberName + new StringBuilder().append("      level ").append(numberOfTransformations).toString());
				}
				n++;
			}
		}

		this.plugin.sendInfo(player, ChatColor.AQUA + "Use " + ChatColor.GOLD + "/ww clan" + ChatColor.AQUA + " to see info about your clan");

		return true;
	}

	private void commandTopHunters(Player player)
	{
		if (!this.plugin.vaultEnabled)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "Vault not detected. Werewolf hunts & bounties are disabled.");

		}
		else if ((player == null) || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.top")) || player.isOp())
		{
			List<Hunter> hunters = new ArrayList<Hunter>();

			Set<String> list = Werewolf.getHuntManager().getHunters();
			for (String hunterName : list)
			{
				int kills = Werewolf.getHuntManager().getHunterKills(hunterName);
				if (kills > 0)
				{
					hunters.add(new Hunter(hunterName, kills));
				}
			}

			if (hunters.size() == 0)
			{
				if (player != null)
				{
					this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoWerewolfHuntersInWorld, ChatColor.RED));
				}
				else
				{
					this.plugin.log(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoWerewolfHuntersInWorld, ChatColor.WHITE));
				}
				return;
			}

			if (player != null)
			{
				this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TopWerewolfHunters, ChatColor.RED));
			}
			else
			{
				this.plugin.log(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TopWerewolfHunters, ChatColor.WHITE));
			}

			Collections.sort(hunters, new TopHuntersComparator());

			int l = hunters.size();
			if (l > 10)
			{
				hunters = hunters.subList(0, 10);
			}
			int n = 1;
			for (Hunter hunter : hunters)
			{
				String message = "" + n + ChatColor.AQUA + " - " + String.format("%-15s", hunter.name) + new StringBuilder().append(hunter.kills).append(" Kills").toString();

				this.plugin.sendInfo(player, ChatColor.YELLOW + message);
				// plugin.sendInfo(player.getUniqueId(),
				// LanguageManager.LANGUAGESTRING., ChatColor.GREEN, "",
				// (int)this.plugin.infectionPrice, 1);

				n++;
			}
			if (player != null)
			{
				this.plugin.log(player.getName() + ": /werewolf top");
			}
		}
		else
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
		}
	}

	// The command that is called by a player when the player wants to transform
	// into a Werewolf manually.
	public boolean commandTransform(Player player)
	{
		if (player == null)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "This command cannot be used from console");
			return false;
		}
		if ((!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.transform")) && !player.isOp())
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().isWerewolf(player))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED));
			return false;
		}
		if (Werewolf.getWerewolfManager().getNumberOfTransformations(player.getUniqueId()) < this.plugin.transformsForControlledTransformation)
		{
			int fullMoons = this.plugin.transformsForControlledTransformation - Werewolf.getWerewolfManager().getNumberOfTransformations(player.getUniqueId());
			Werewolf.getLanguageManager().setAmount("" + fullMoons);
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.MustExperienceMoreFullMoons, ChatColor.RED));
			return false;
		}
		if (Werewolf.getMoonManager().isTransformationNight(player.getWorld()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CannotControlTransformationTonight, ChatColor.RED));
			return false;
		}
		if (Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			this.plugin.untransform(player, WerewolfTransformEvent.Reason.CONTROLLED);
		}
		else
		{
			long cooldownLeft = Werewolf.getWerewolfManager().getTransformCooldownLeft(player.getUniqueId());
			if (cooldownLeft > 0)
			{
				Werewolf.getLanguageManager().setAmount("" + cooldownLeft);
				this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CannotTransformSoSoon, ChatColor.RED));
				return false;
			}
			this.plugin.transform(player, WerewolfTransformEvent.Reason.CONTROLLED);

			long timeLimit = Werewolf.getWerewolfManager().getTransformationTimeLimitSeconds(player.getUniqueId());
			if (timeLimit >= 0 && Werewolf.getWerewolfManager().isControlledForm(player.getUniqueId()))
			{
				Werewolf.getLanguageManager().setAmount("" + timeLimit);
				this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TransformationTimeLimit, ChatColor.LIGHT_PURPLE));
			}
		}
		return true;
	}

	// Toggles a werewolf's intent to infect, which raises the infection chance of their bites
	public boolean commandIntent(Player player)
	{
		if (player == null)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "This command cannot be used from console");
			return false;
		}
		if ((!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.intent")) && !player.isOp())
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().isFullWerewolf(player.getUniqueId()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().canUseIntent(player.getUniqueId()))
		{
			int fullMoons = this.plugin.levelForIntent - Werewolf.getWerewolfManager().getNumberOfTransformations(player.getUniqueId());
			Werewolf.getLanguageManager().setAmount("" + fullMoons);
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.MustExperienceMoreFullMoons, ChatColor.RED));
			return false;
		}

		boolean intent = !Werewolf.getWerewolfManager().hasIntent(player.getUniqueId());
		Werewolf.getWerewolfManager().setIntent(player.getUniqueId(), intent);

		LanguageManager.LANGUAGESTRING message = intent ? LanguageManager.LANGUAGESTRING.IntentEnabled : LanguageManager.LANGUAGESTRING.IntentDisabled;
		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(message, ChatColor.LIGHT_PURPLE));
		return true;
	}

	// Admin command: /ww setphase <phase> [world]. Moves the world's time forward to the next night with that phase.
	public boolean commandSetPhase(CommandSender sender, String[] args)
	{
		Player player = (sender instanceof Player) ? (Player) sender : null;

		if (player != null && !player.isOp() && !Werewolf.getPermissionsManager().hasPermission(player, "werewolf.setphase"))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}

		MoonManager.MoonPhase phase = args.length >= 2 ? MoonManager.MoonPhase.fromString(args[1]) : null;
		if (phase == null)
		{
			List<String> phaseNames = new ArrayList<String>();
			for (MoonManager.MoonPhase p : MoonManager.MoonPhase.values())
			{
				phaseNames.add(p.name());
			}
			Werewolf.getLanguageManager().setType(String.join(", ", phaseNames));
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.InvalidMoonPhase, ChatColor.RED));
			return false;
		}

		World world;
		if (args.length >= 3)
		{
			world = this.plugin.getServer().getWorld(args[2]);
			if (world == null)
			{
				this.plugin.sendInfo(player, ChatColor.RED + "No world named " + args[2]);
				return false;
			}
		}
		else if (player != null)
		{
			world = player.getWorld();
		}
		else
		{
			world = this.plugin.getServer().getWorlds().get(0);
		}

		Werewolf.getMoonManager().setMoonPhase(world, phase);

		Werewolf.getLanguageManager().setPlayerName(world.getName());
		Werewolf.getLanguageManager().setType(Werewolf.getMoonManager().getPhaseName(Werewolf.getMoonManager().getMoonPhase(world)));
		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.MoonPhaseSet, ChatColor.AQUA));

		this.plugin.log(sender.getName() + " set the moon phase in " + world.getName() + " to " + phase.name());
		return true;
	}

	// The command that is runned by a Werewolf to get info about their current
	// clan and the other clans
	public boolean commandClan(Player player)
	{
		if (!this.plugin.useClans)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "Clans are not enabled.");
			return false;
		}
		if (player == null)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "This command cannot be used from console");
			return false;
		}
		if (!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.clan") && !player.isOp())
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().isWerewolf(player))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED));
			return false;
		}

		String clanName = Werewolf.getClanManager().getClanName(player.getUniqueId());
		String alphaName = "None!";
		ClanManager.ClanType playerClan = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());
		UUID alphaId = Werewolf.getClanManager().getAlpha(playerClan);

		if (alphaId != null)
		{
			alphaName = plugin.getServer().getOfflinePlayer(alphaId).getName();
		}

		this.plugin.sendInfo(player, ChatColor.GOLD + "------------ The " + clanName + " Clan ------------");

		this.plugin.sendInfo(player, "" + ChatColor.AQUA);
		this.plugin.sendInfo(player, "" + ChatColor.AQUA + " Clan Alpha : " + ChatColor.WHITE + alphaName);
		this.plugin.sendInfo(player, "" + ChatColor.AQUA);

		if (alphaId != null && !player.getUniqueId().equals(alphaId))
		{
			this.plugin.sendInfo(player, ChatColor.RED + "Kill " + ChatColor.GOLD + alphaName + ChatColor.RED + " to take over the position as alpha!");
			this.plugin.sendInfo(player, "" + ChatColor.AQUA);
		}

		this.plugin.sendInfo(player, ChatColor.GOLD + "------------ The Werewolf Clans ------------");

		List<ClanManager.ClanType> clanList = Werewolf.getClanManager().getClansRanked();

		int n = 1;
		for (ClanManager.ClanType clan : clanList)
		{
			if (clan.equals(playerClan))
			{
				this.plugin.sendInfo(player, "" + ChatColor.GOLD + n + ") " + ChatColor.GOLD + Werewolf.getClanManager().getClanName(clan) + ChatColor.AQUA + "  -  " + Werewolf.getWerewolfManager().getWerewolfClanMembers(clan).size() + " members  -  "
						+ String.format("%1$,.2f", new Object[] { Double.valueOf(Werewolf.getClanManager().getClanPoint(clan)) }) + " clan points");
			}
			else
			{
				this.plugin.sendInfo(player, "" + ChatColor.GOLD + n + ") " + ChatColor.WHITE + Werewolf.getClanManager().getClanName(clan) + ChatColor.AQUA + "  -  " + Werewolf.getWerewolfManager().getWerewolfClanMembers(clan).size() + " members  -  "
						+ String.format("%1$,.2f", new Object[] { Double.valueOf(Werewolf.getClanManager().getClanPoint(clan)) }) + " clan points");
			}
			n++;
		}

		this.plugin.sendInfo(player, "" + ChatColor.GOLD);

		if (Werewolf.getClanManager().isAlpha(playerClan, player.getUniqueId()))
		{
			if (player.isOp() || Werewolf.getPermissionsManager().hasPermission(player, "werewolf.sethome"))
			{
				Werewolf.getLanguageManager().setType("/ww sethome");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpSetHome, ChatColor.AQUA));
			}
		}
		else
		{
			if (player.isOp() || Werewolf.getPermissionsManager().hasPermission(player, "werewolf.home"))
			{
				Werewolf.getLanguageManager().setType("/ww home");
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.HelpHome, ChatColor.AQUA));
			}
		}

		this.plugin.sendInfo(player, ChatColor.AQUA + "Use " + ChatColor.GOLD + "/ww list" + ChatColor.AQUA + " to see the list of members in your clan");

		// this.plugin.sendInfo(player, "" + ChatColor.GOLD +
		// Werewolf.getClanManager().getClanName((ClanManager.ClanType)
		// clanList.get(0)) + ChatColor.DARK_RED + " has the Blood Rage!");

		return true;
	}

	// The command that is run by the clan alpha to set the home for the clan
	private boolean commandSetHome(Player player)
	{
		if (!this.plugin.useClans)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "Clans are not enabled.");
			return false;
		}
		if (player == null)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "This command cannot be used from console");
			return false;
		}

		if (!player.isOp() && !Werewolf.getPermissionsManager().hasPermission(player, "werewolf.sethome"))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().isWerewolf(player))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED));
			return false;
		}

		ClanType clan = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());

		if (!Werewolf.getClanManager().isAlpha(clan, player.getUniqueId()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouAreNotTheAlphaOfTheClan, ChatColor.RED));
			return false;
		}

		Werewolf.getClanManager().setHomeForClan(clan, player.getLocation());

		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ClanSetHome, ChatColor.GREEN));

		return true;
	}

	// The command that is run by werewolves to get to their clan home
	private boolean commandHome(Player player)
	{
		if (!player.isOp() && !Werewolf.getPermissionsManager().hasPermission(player, "werewolf.home"))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}

		if (!Werewolf.getWerewolfManager().isWerewolf(player))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED));
			return false;
		}

		ClanManager.ClanType playerClan = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());

		Location location = Werewolf.getClanManager().getHomeForClan(playerClan);
		if (location == null)
		{
			return false;
		}

		player.teleport(location);

		Werewolf.getLanguageManager().setType(Werewolf.getClanManager().getClanName(playerClan));
		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouTeleportedToClanHome, ChatColor.GREEN));

		return true;
	}

	// The command that is run manually to toggle the player who ran the command
	// to/from a werewolf
	public boolean commandToggleSelfWerewolf(Player player, String[] args)
	{
		if (player == null)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "This command cannot be used from console");
			return false;
		}

		if ((!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.togglewerewolfself")) && !player.isOp())
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}

		if (!Werewolf.getWerewolfManager().isWerewolf(player))
		{
			Random random = new Random();

			if (!Werewolf.getWerewolfManager().makeWerewolf(player, true, ClanManager.ClanType.values()[random.nextInt(ClanManager.ClanType.values().length)], WerewolfInfectionEvent.Source.COMMAND))
			{
				Werewolf.getLanguageManager().setPlayerName(player.getName());
				this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED));
				return true;
			}

			Werewolf.getWerewolfManager().setInfectedThisNight(player.getUniqueId(), player.getName(), false);

			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NowAWerewolf, ChatColor.AQUA));
		}
		else
		{
			if (!Werewolf.getWerewolfManager().unmakeWerewolf(player.getUniqueId(), WerewolfCureEvent.Cause.COMMAND))
			{
				Werewolf.getLanguageManager().setPlayerName(player.getName());
				this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED));
				return true;
			}
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NotAWerewolfAnymore, ChatColor.AQUA));
		}

		if (player != null)
		{
			this.plugin.log(player.getName() + ": /werewolf toggleself");
		}
		return true;
	}

	// The command that is run manually to toggle if a player is a werewolf
	public boolean commandTogglePlayerWerewolf(Player player, String[] args)
	{
		if ((player == null) || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.togglewerewolf")) || player.isOp())
		{
			try
			{
				Player newWerewolf = this.plugin.getServer().getPlayer(args[1]);

				if (newWerewolf == null)
				{
					throw new Exception();
				}

				if (!Werewolf.getWerewolfManager().isWerewolf(newWerewolf))
				{
					Random random = new Random();
					if (!Werewolf.getWerewolfManager().makeWerewolf(newWerewolf, true, ClanManager.ClanType.values()[random.nextInt(ClanManager.ClanType.values().length)], WerewolfInfectionEvent.Source.COMMAND))
					{
						Werewolf.getLanguageManager().setPlayerName(newWerewolf.getName());
						this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED));
						return true;
					}
					Werewolf.getWerewolfManager().setInfectedThisNight(newWerewolf.getUniqueId(), newWerewolf.getName(), false);
					if (player != null)
					{
						player.sendMessage(ChatColor.AQUA + newWerewolf.getName() + " is now a werewolf! ");

						Werewolf.getLanguageManager().setPlayerName(player.getName());
					}
					else
					{
						this.plugin.log(newWerewolf.getName() + " is now a werewolf! ");

						Werewolf.getLanguageManager().setPlayerName("CONSOLE");
					}

					newWerewolf.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.MadeWerewolf, ChatColor.AQUA));
				}
				else
				{
					if (!Werewolf.getWerewolfManager().unmakeWerewolf(newWerewolf.getUniqueId(), WerewolfCureEvent.Cause.COMMAND))
					{
						Werewolf.getLanguageManager().setPlayerName(newWerewolf.getName());
						this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED));
						return true;
					}
					if (player != null)
					{
						player.sendMessage(ChatColor.AQUA + newWerewolf.getName() + " is no longer a werewolf ...");
						Werewolf.getLanguageManager().setPlayerName(player.getName());
					}
					else
					{
						this.plugin.log(newWerewolf.getName() + " is no longer a werewolf ...");
						Werewolf.getLanguageManager().setPlayerName("CONSOLE");
					}
					newWerewolf.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UnMadeWerewolf, ChatColor.AQUA));
				}

				if (player == null)
				{
					return true;
				}

				this.plugin.log(player.getName() + ": /werewolf toggle " + newWerewolf.getName());
			}
			catch (Exception e)
			{
				this.plugin.sendInfo(player, ChatColor.RED + "Invalid player name...");
			}
		}
		else
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
		}

		return true;
	}

	// Command to infect a player with the werewolf infection: /ww infect <player> [clan]
	public boolean commandInfect(Player player, String[] args)
	{
		if (!hasCommandPermission(player, "werewolf.infect"))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return true;
		}

		Player newInfection = this.plugin.getServer().getPlayer(args[1]);
		if (newInfection == null)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "Invalid player name...");
			return true;
		}

		ClanManager.ClanType clan;
		if (args.length >= 3)
		{
			clan = parseClan(args[2]);
			if (clan == null)
			{
				Werewolf.getLanguageManager().setType(String.join(", ", getClanNames()));
				this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminInvalidClan, ChatColor.RED));
				return true;
			}
		}
		else
		{
			clan = ClanManager.ClanType.values()[new Random().nextInt(ClanManager.ClanType.values().length)];
		}

		if (Werewolf.getWerewolfManager().isWerewolf(newInfection))
		{
			this.plugin.sendInfo(player, ChatColor.RED + newInfection.getName() + " already is a werewolf! Please cure them first...");
			return true;
		}

		if (!Werewolf.getWerewolfManager().makeWerewolf(newInfection, false, clan, WerewolfInfectionEvent.Source.COMMAND))
		{
			Werewolf.getLanguageManager().setPlayerName(newInfection.getName());
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED));
			return true;
		}

		this.plugin.sendInfo(player, ChatColor.GREEN + newInfection.getName() + " now has the werewolf infection!");
		Werewolf.getLanguageManager().setPlayerName(player == null ? "CONSOLE" : player.getName());
		newInfection.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.Infected, ChatColor.AQUA));

		this.plugin.log((player == null ? "CONSOLE" : player.getName()) + ": /werewolf infect " + newInfection.getName() + " " + clan.name());
		return true;
	}

	private boolean hasCommandPermission(Player player, String permission)
	{
		return player == null || player.isOp() || Werewolf.getPermissionsManager().hasPermission(player, permission);
	}

	private void addIfPermitted(List<String> suggestions, Player player, String subcommand, String permission)
	{
		if (hasCommandPermission(player, permission))
		{
			suggestions.add(subcommand);
		}
	}

	private ClanManager.ClanType parseClan(String name)
	{
		String wanted = ChatColor.stripColor(name).replace(" ", "");
		for (ClanManager.ClanType clan : ClanManager.ClanType.values())
		{
			String clanName = Werewolf.getClanManager().getClanName(clan);
			if (clan.name().equalsIgnoreCase(wanted) || (clanName != null && ChatColor.stripColor(clanName).replace(" ", "").equalsIgnoreCase(wanted)))
			{
				return clan;
			}
		}
		return null;
	}

	private List<String> getClanNames()
	{
		List<String> names = new ArrayList<String>();
		for (ClanManager.ClanType clan : ClanManager.ClanType.values())
		{
			String clanName = Werewolf.getClanManager().getClanName(clan);
			names.add(clanName != null ? ChatColor.stripColor(clanName).replace(" ", "") : clan.name());
		}
		return names;
	}

	/**
	 * Finds a player by name, online or not: online players, then werewolves.yml, then players the server has seen.
	 * Unknown names give null rather than a made-up offline player.
	 */
	private UUID findPlayerId(String name)
	{
		Player online = this.plugin.getServer().getPlayerExact(name);
		if (online != null)
		{
			return online.getUniqueId();
		}

		UUID werewolfId = Werewolf.getWerewolfManager().findWerewolfByName(name);
		if (werewolfId != null)
		{
			return werewolfId;
		}

		for (OfflinePlayer offline : this.plugin.getServer().getOfflinePlayers())
		{
			if (name.equalsIgnoreCase(offline.getName()))
			{
				return offline.getUniqueId();
			}
		}

		online = this.plugin.getServer().getPlayer(name);
		return online != null ? online.getUniqueId() : null;
	}

	// Looks up the werewolf named in args[1] for an admin command, telling the sender if there is none
	private UUID findWerewolfForCommand(Player player, String name)
	{
		UUID playerId = findPlayerId(name);
		if (playerId == null)
		{
			Werewolf.getLanguageManager().setPlayerName(name);
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminUnknownPlayer, ChatColor.RED));
			return null;
		}

		if (!Werewolf.getWerewolfManager().isWerewolf(playerId))
		{
			Werewolf.getLanguageManager().setPlayerName(name);
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminNotAWerewolf, ChatColor.RED));
			return null;
		}

		return playerId;
	}

	private void sendUsage(Player player, String usage, LanguageManager.LANGUAGESTRING help)
	{
		Werewolf.getLanguageManager().setType(usage);
		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(help, ChatColor.RED));
	}

	// Admin commands: /ww setlevel <player> <level> and /ww addlevel <player> <amount>
	public boolean commandSetLevel(Player player, String[] args, boolean add)
	{
		String permission = add ? "werewolf.addlevel" : "werewolf.setlevel";
		if (!hasCommandPermission(player, permission))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return true;
		}

		if (args.length < 3)
		{
			if (add)
			{
				sendUsage(player, "/ww addlevel <player> <amount>", LanguageManager.LANGUAGESTRING.HelpAddLevel);
			}
			else
			{
				sendUsage(player, "/ww setlevel <player> <level>", LanguageManager.LANGUAGESTRING.HelpSetLevel);
			}
			return true;
		}

		Integer amount = null;
		try
		{
			amount = Integer.valueOf(args[2]);
		}
		catch (NumberFormatException ignored)
		{
		}
		// Levels can't go below 0; addlevel may take some away
		if (amount == null || (!add && amount < 0))
		{
			Werewolf.getLanguageManager().setType(args[2]);
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminInvalidNumber, ChatColor.RED));
			return true;
		}

		UUID playerId = findWerewolfForCommand(player, args[1]);
		if (playerId == null)
		{
			return true;
		}

		int level = add ? Werewolf.getWerewolfManager().getNumberOfTransformations(playerId) + amount : amount;
		Werewolf.getWerewolfManager().setNumberOfTransformations(playerId, level);

		String name = Werewolf.getWerewolfManager().getPlayerName(playerId);
		Werewolf.getLanguageManager().setPlayerName(name);
		Werewolf.getLanguageManager().setAmount("" + Werewolf.getWerewolfManager().getNumberOfTransformations(playerId));
		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminLevelSet, ChatColor.GREEN));

		this.plugin.log((player == null ? "CONSOLE" : player.getName()) + ": /werewolf " + args[0] + " " + name + " " + args[2]);
		return true;
	}

	// Admin command: /ww setalpha <player>, for the player's own clan
	public boolean commandSetAlpha(Player player, String[] args)
	{
		if (!hasCommandPermission(player, "werewolf.setalpha"))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return true;
		}

		if (!this.plugin.useClans)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "Clans are not enabled.");
			return true;
		}

		if (args.length < 2)
		{
			sendUsage(player, "/ww setalpha <player>", LanguageManager.LANGUAGESTRING.HelpSetAlpha);
			return true;
		}

		UUID playerId = findWerewolfForCommand(player, args[1]);
		if (playerId == null)
		{
			return true;
		}

		String name = Werewolf.getWerewolfManager().getPlayerName(playerId);
		ClanManager.ClanType clan = Werewolf.getWerewolfManager().getWerewolfClan(playerId);
		Werewolf.getLanguageManager().setPlayerName(name);

		if (!Werewolf.getClanManager().assignAlphaInClan(clan, playerId))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED));
			return true;
		}

		Werewolf.getWerewolfManager().sendMessageToClan(clan, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NewClanAlpha, ChatColor.WHITE));

		Werewolf.getLanguageManager().setPlayerName(name);
		Werewolf.getLanguageManager().setType(Werewolf.getClanManager().getClanName(clan));
		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminAlphaSet, ChatColor.GREEN));

		this.plugin.log((player == null ? "CONSOLE" : player.getName()) + ": /werewolf setalpha " + name);
		return true;
	}

	// Admin command: /ww cure <player>. Unlike /ww toggle it never turns anyone into a werewolf, and works offline
	public boolean commandCure(Player player, String[] args)
	{
		if (!hasCommandPermission(player, "werewolf.cureother"))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return true;
		}

		if (args.length < 2)
		{
			sendUsage(player, "/ww cure <player>", LanguageManager.LANGUAGESTRING.HelpCureOther);
			return true;
		}

		UUID playerId = findWerewolfForCommand(player, args[1]);
		if (playerId == null)
		{
			return true;
		}

		String name = Werewolf.getWerewolfManager().getPlayerName(playerId);
		Werewolf.getLanguageManager().setPlayerName(name);

		if (!Werewolf.getWerewolfManager().unmakeWerewolf(playerId, WerewolfCureEvent.Cause.COMMAND))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminChangeCancelled, ChatColor.RED));
			return true;
		}

		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AdminCured, ChatColor.GREEN));

		Player target = this.plugin.getServer().getPlayer(playerId);
		if (target != null)
		{
			Werewolf.getLanguageManager().setPlayerName(player == null ? "CONSOLE" : player.getName());
			target.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UnMadeWerewolf, ChatColor.AQUA));
		}

		this.plugin.log((player == null ? "CONSOLE" : player.getName()) + ": /werewolf cure " + name);
		return true;
	}

	// Command to infect yourself with the werewolf infection
	public boolean commandInfectSelf(Player player, String[] args)
	{
		if (player == null)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "This command cannot be used from console");
			return false;
		}
		if ((Werewolf.getPermissionsManager().hasPermission(player, "werewolf.infectself")) || player.isOp())
		{
			if (!Werewolf.getWerewolfManager().isWerewolf(player))
			{
				Random random = new Random();
				if (Werewolf.getWerewolfManager().makeWerewolf(player, false, ClanManager.ClanType.values()[random.nextInt(ClanManager.ClanType.values().length)], WerewolfInfectionEvent.Source.COMMAND))
				{
					player.sendMessage(ChatColor.AQUA + "You now have the werewolf infection!");
				}
				else
				{
					player.sendMessage(ChatColor.RED + "Could not make you a werewolf!");
				}
			}
			else
			{
				player.sendMessage(ChatColor.RED + "You are already a werewolf! Please toggle your status first...");
			}
			this.plugin.log(player.getName() + ": /werewolf infect");
		}
		else
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
		}
		return true;
	}

	// Command to check if a player is a werewolf
	public boolean commandCheck(Player player, String[] args)
	{
		if (player == null || Werewolf.getPermissionsManager().hasPermission(player, "werewolf.check") || player.isOp())
		{
			try
			{
				Player checkPlayer = this.plugin.getServer().getPlayer(args[1]);

				if (checkPlayer != null)
				{
					if (Werewolf.getWerewolfManager().isFullWerewolf(checkPlayer.getUniqueId()))
					{
						if (player != null)
						{
							int level = Werewolf.getWerewolfManager().getNumberOfTransformations(player.getUniqueId());

							Werewolf.getLanguageManager().setPlayerName(player.getName());
							Werewolf.getLanguageManager().setType(String.valueOf(level));
							this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.PlayerIsAFullWerewolf, ChatColor.AQUA));
							// player.sendMessage(ChatColor.AQUA +
							// checkPlayer.getName() + " is a full werewolf");
						}
						else
						{
							this.plugin.log(checkPlayer.getName() + " is a full werewolf");
						}
					}
					else if (Werewolf.getWerewolfManager().isInfectedWerewolf(checkPlayer.getUniqueId()))
					{
						if (player != null)
						{
							Werewolf.getLanguageManager().setPlayerName(player.getName());
							this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.PlayerIsAnInfectedWerewolf, ChatColor.AQUA));
							// player.sendMessage(ChatColor.AQUA +
							// checkPlayer.getName() + " is a infected
							// werewolf");
						}
						else
						{
							this.plugin.log(checkPlayer.getName() + " is a infected werewolf");
						}
					}
					else if (player != null)
					{
						Werewolf.getLanguageManager().setPlayerName(player.getName());
						this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.PlayerIsNotAWerewolf, ChatColor.AQUA));
						// player.sendMessage(ChatColor.AQUA +
						// checkPlayer.getName() + " is not a werewolf");
					}
					else
					{
						this.plugin.log(checkPlayer.getName() + " is not a werewolf");
					}

					if (player != null)
					{
						this.plugin.log(player.getName() + ": /werewolf check " + checkPlayer.getName());
					}
				}
			}
			catch (Exception ex)
			{
				this.plugin.log("'" + player.getName() + "' made a command error");
			}
		}
		else
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
		}
		return true;
	}

	// Command to check werewolf bounties
	public boolean commandBounty(Player player)
	{
		if (!this.plugin.vaultEnabled)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "Vault not detected. Werewolf hunts & bounties are disabled.");
		}
		else if ((player == null) || (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.bounty")) || player.isOp())
		{
			Werewolf.getLanguageManager().setAmount(Werewolf.getHuntManager().getFormattedBounty());
			if (player == null)
			{
				this.plugin.log(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.BountyTotal, ChatColor.WHITE));
			}
			else
			{
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.BountyTotal, ChatColor.AQUA));
			}
			if (player != null)
			{
				this.plugin.log(player.getName() + ": /werewolf bounty");
			}
		}
		else
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
		}
		return true;
	}

	// Command to add bounties to a werewolf
	public boolean commandAddBounty(Player player, String[] args)
	{
		if (!this.plugin.vaultEnabled)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "Vault not detected. Werewolf hunts & bounties are disabled.");
		}
		else if (player == null || Werewolf.getPermissionsManager().hasPermission(player, "werewolf.addbounty") || player.isOp())
		{
			try
			{
				if (player == null)
				{
					return true;
				}

				if (Werewolf.getWerewolfManager().isWerewolf(player))
				{
					this.plugin.sendInfo(player, ChatColor.RED + "Werewolves cannot add to the Werewolf bounty!");
					return false;
				}
				else
				{
					int bounty = Integer.parseInt(args[1]);

					if (bounty <= 0)
					{
						this.plugin.sendInfo(player, ChatColor.RED + "How about adding a real amount?");

						return false;
					}

					Werewolf.getHuntManager().addBounty(player.getName(), bounty);

					this.plugin.log(player.getName() + ": /werewolf addbounty " + bounty);

					return true;
				}
			}
			catch (NumberFormatException ex)
			{
				this.plugin.sendInfo(player, ChatColor.RED + "Come on, that is not a valid bounty :/");

				return false;
			}
		}
		else
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));

			return false;
		}

		return false;
	}

	// Command to hunt a werewolf
	public boolean commandHuntWerewolf(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}
		if (!this.plugin.vaultEnabled)
		{
			this.plugin.sendInfo(player, ChatColor.RED + "Vault not detected. Werewolf hunts & bounties are disabled.");
			return false;
		}

		if (!this.plugin.isWerewolvesAllowedInWorld(player))
		{
			Werewolf.getLanguageManager().setType(player.getWorld().getName());
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoWerewolvesInThisWorld, ChatColor.RED));
			return false;
		}

		if ((player != null) && (!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.hunt")) && (!player.isOp()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}

		if (Werewolf.getWerewolfManager().isWerewolf(player))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolvesCannotHuntWerewolves, ChatColor.RED));
			return false;
		}

		if (!Werewolf.getHuntManager().isHunting(player.getUniqueId()))
		{
			if (Werewolf.getWerewolfManager().getOnlineWerewolvesInWolfForm(player.getWorld()).size() == 0)
			{
				this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoWerewolvesOnline, ChatColor.RED));
				return false;
			}

			if (player.getInventory().getItemInMainHand().getType() != Material.AIR)
			{
				this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouMustHaveYourHandsFree, ChatColor.RED));
				return false;
			}

			player.getInventory().setItemInMainHand(new ItemStack(Material.COMPASS));

			Werewolf.getLanguageManager().setPlayerName(player.getName());
			this.plugin.getServer().broadcastMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.PlayerIsHuntingWerewolves, ChatColor.GOLD));

			Werewolf.getHuntManager().setHunting(player.getUniqueId(), true);
		}
		else
		{
			if (player.getInventory().getItemInMainHand().getType() != Material.COMPASS)
			{
				this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TakeCompassInHands, ChatColor.RED));
				return false;
			}
			player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
			Werewolf.getLanguageManager().setPlayerName(player.getName());
			this.plugin.getServer().broadcastMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.PlayerIsNoLongerHuntingWerewolves, ChatColor.GOLD));
			Werewolf.getHuntManager().setHunting(player.getUniqueId(), false);
		}
		if (player != null)
		{
			this.plugin.log(player.getName() + ": /werewolf hunt");
		}
		return true;
	}

	// Command for a werewolf to growl
	public boolean commandGrowl(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}
		if ((!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.growl")) && (!player.isOp()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().isWolfForm(player.getUniqueId()))
		{
			this.plugin.sendInfo(player, ChatColor.RED + "You are not in wolf form");
			return false;
		}
		long cooldownLeft = Werewolf.getWerewolfManager().getGrowlCooldownLeft(player.getUniqueId());
		if (cooldownLeft > 0)
		{
			Werewolf.getLanguageManager().setAmount("" + cooldownLeft);
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AbilityOnCooldown, ChatColor.RED));
			return false;
		}
		Werewolf.getWerewolfManager().setLastGrowl(player.getUniqueId());
		Werewolf.getWerewolfManager().growl(player);

		return true;
	}

	// Command for a werewolf to howl
	public boolean commandHowl(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}
		if ((!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.howl")) && (!player.isOp()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().isWolfForm(player.getUniqueId()))
		{
			this.plugin.sendInfo(player, ChatColor.RED + "You are not in wolf form");
			return false;
		}
		long cooldownLeft = Werewolf.getWerewolfManager().getHowlCooldownLeft(player.getUniqueId());
		if (cooldownLeft > 0)
		{
			Werewolf.getLanguageManager().setAmount("" + cooldownLeft);
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AbilityOnCooldown, ChatColor.RED));
			return false;
		}
		Werewolf.getWerewolfManager().setLastHowl(player.getUniqueId());

		Werewolf.getWerewolfManager().howl(player);

		if (Werewolf.getClanManager().isAlpha(player.getUniqueId()))
		{
			for (Entity entity : player.getNearbyEntities(17.5D, 17.5D, 17.5D))
			{
				if ((entity instanceof LivingEntity))
				{
					LivingEntity livingEntity = (LivingEntity) entity;

					livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 3));
				}
			}
		}
		return true;
	}

	// Command for an alpha to call a werewolf to him
	public boolean commandCall(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}
		if ((!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.alpha.call")) && (!player.isOp()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().isWolfForm(player.getUniqueId()))
		{
			this.plugin.sendInfo(player, "You are not in wolf form");
			return false;
		}
		ClanManager.ClanType playerClan = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());
		if (!Werewolf.getClanManager().isAlpha(playerClan, player.getUniqueId()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouAreNotTheAlphaOfTheClan, ChatColor.RED));
			return false;
		}
		Werewolf.getClanManager().setLastCall(playerClan);

		Werewolf.getWerewolfManager().howl(player);
		for (String playerName : Werewolf.getWerewolfManager().getWerewolfClanMembers(playerClan))
		{
			Player clanPlayer = this.plugin.getServer().getPlayer(playerName);
			if (clanPlayer != null)
			{
				Werewolf.getLanguageManager().setPlayerName(player.getName());
				this.plugin.sendInfo(clanPlayer, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouAreBeingCalled, ChatColor.RED));

				Werewolf.getLanguageManager().setPlayerName(clanPlayer.getName());
				this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouCalledClanMember, ChatColor.RED));
			}
		}
		return true;
	}

	// Command to accept the call
	public boolean commandAcceptCall(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}
		if ((!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.acceptcall")) && (!player.isOp()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().isWolfForm(player.getUniqueId()))
		{
			this.plugin.sendInfo(player, ChatColor.RED + "You are not in wolf form");
			return false;
		}
		if (Werewolf.getClanManager().isAlpha(player.getUniqueId()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ClanAlphaCannotAnswerCall, ChatColor.RED));
			return false;
		}
		ClanManager.ClanType playerClan = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());
		if (!Werewolf.getClanManager().hasRecentCall(playerClan))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoRecentAlphaCall, ChatColor.RED));
			return false;
		}

		UUID alphaId = Werewolf.getClanManager().getAlpha(playerClan);
		Player alphaPlayer = this.plugin.getServer().getPlayer(alphaId);

		player.teleport(alphaPlayer);

		return true;
	}

	// Command to get an infection potion
	public boolean commandInfectionPotion(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}
		if ((!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.potion.infection.create")) && (!player.isOp()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if ((player.getInventory().getItemInMainHand() != null) && (player.getInventory().getItemInMainHand().getType() != Material.AIR))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouMustHaveYourHandsFree, ChatColor.RED));
			return false;
		}

		ItemStack potionItem = Werewolf.getItemManager().newInfectionPotion();

		player.getInventory().setItemInMainHand(potionItem);

		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CreatedWerewolfInfectionPotion, ChatColor.GREEN));

		return true;
	}

	// Command to get a cure potion
	public boolean commandCurePotion(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}
		if ((!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.potion.cure.create")) && (!player.isOp()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if ((player.getInventory().getItemInMainHand() != null) && (player.getInventory().getItemInMainHand().getType() != Material.AIR))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouMustHaveYourHandsFree, ChatColor.RED));
			return false;
		}

		ItemStack potionItem = Werewolf.getItemManager().newCurePotion();

		player.getInventory().setItemInMainHand(potionItem);

		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CreatedWerewolfCurePotion, ChatColor.GREEN));

		return true;
	}

	// Command to get the wolfbane potion
	public boolean commandWolfbanePotion(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}
		if ((!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.potion.wolfbane.create")) && (!player.isOp()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if ((player.getInventory().getItemInMainHand() != null) && (player.getInventory().getItemInMainHand().getType() != Material.AIR))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouMustHaveYourHandsFree, ChatColor.RED));
			return false;
		}

		ItemStack potionItem = Werewolf.getItemManager().newWolfbanePotion();

		player.getInventory().setItemInMainHand(potionItem);

		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CreatedWerewolfWolfbanePotion, ChatColor.GREEN));

		return true;
	}

	// Command to get a silver sword
	public boolean commandSilverSword(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}

		if (!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.silversword.create") && !player.isOp())
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}

		if ((player.getInventory().getItemInMainHand() != null) && (player.getInventory().getItemInMainHand().getType() != Material.AIR))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouMustHaveYourHandsFree, ChatColor.RED));
			return false;
		}

		ItemStack swordItem = Werewolf.getItemManager().newSilverSword(1);

		player.getInventory().setItemInMainHand(swordItem);

		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CreatedWerewolfSilverSword, ChatColor.GREEN));

		return true;
	}

	// Admin commands giving hunting gear: /ww tracker, /ww vampiretracker, /ww silverarmor
	private boolean commandGiveItems(Player player, String permission, LanguageManager.LANGUAGESTRING createdMessage, ItemStack... items)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}

		if (!Werewolf.getPermissionsManager().hasPermission(player, permission) && !player.isOp())
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}

		for (ItemStack leftover : player.getInventory().addItem(items).values())
		{
			player.getWorld().dropItemNaturally(player.getLocation(), leftover);
		}

		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(createdMessage, ChatColor.GREEN));

		return true;
	}

	public boolean commandSilverArmor(Player player)
	{
		ItemManager.ArmorPiece[] pieces = ItemManager.ArmorPiece.values();
		ItemStack[] items = new ItemStack[pieces.length];
		for (int i = 0; i < pieces.length; i++)
		{
			items[i] = Werewolf.getItemManager().newSilverArmor(pieces[i]);
		}
		return commandGiveItems(player, "werewolf.silverarmor.create", LanguageManager.LANGUAGESTRING.CreatedSilverArmor, items);
	}

	// Toggles following the scent trail of the player the werewolf last sniffed
	public boolean commandTrack(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}
		if (!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.track") && !player.isOp())
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().isWerewolf(player))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED));
			return false;
		}

		ScentManager scent = Werewolf.getScentManager();
		if (scent.isTracking(player.getUniqueId()))
		{
			scent.stopTracking(player);
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ScentTrackStopped, ChatColor.LIGHT_PURPLE));
			return true;
		}
		if (!scent.canScentTrack(player.getUniqueId()))
		{
			int fullMoons = this.plugin.transformsForScentTrack - Werewolf.getWerewolfManager().getNumberOfTransformations(player.getUniqueId());
			Werewolf.getLanguageManager().setAmount("" + fullMoons);
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.MustExperienceMoreFullMoons, ChatColor.RED));
			return false;
		}
		if (!Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ScentTrackNotInWolfForm, ChatColor.RED));
			return false;
		}

		UUID targetId = scent.getTarget(player.getUniqueId());
		if (targetId == null)
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ScentNoTarget, ChatColor.RED));
			return false;
		}
		Player target = this.plugin.getServer().getPlayer(targetId);
		if (target == null)
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ScentTargetNotFound, ChatColor.RED));
			return false;
		}
		if (!target.getWorld().equals(player.getWorld()))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ScentTargetOtherWorld, ChatColor.RED));
			return false;
		}

		scent.startTracking(player);
		Werewolf.getLanguageManager().setPlayerName(target.getName());
		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.ScentTrackStarted, ChatColor.LIGHT_PURPLE));
		return true;
	}

	public boolean commandLoreBook(Player player)
	{
		if (player == null)
		{
			this.plugin.log("You cannot use this command from the console.");
			return false;
		}

		if (!Werewolf.getPermissionsManager().hasPermission(player, "werewolf.lorebook.create") && !player.isOp())
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
			return false;
		}

		if ((player.getInventory().getItemInMainHand() != null) && (player.getInventory().getItemInMainHand().getType() != Material.AIR))
		{
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouMustHaveYourHandsFree, ChatColor.RED));
			return false;
		}

		ItemStack bookItem = Werewolf.getItemManager().newLoreBook();

		player.getInventory().setItemInMainHand(bookItem);

		this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CreatedWerewolfLoreBook, ChatColor.GREEN));

		return true;
	}

	// What happens when a command is run?
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player = null;
		if (sender instanceof Player)
		{
			player = (Player) sender;
		}

		if (cmd.getName().equalsIgnoreCase("growl"))
		{
			commandGrowl(player);
		}
		if (cmd.getName().equalsIgnoreCase("howl"))
		{
			commandHowl(player);
		}

		if (cmd.getName().equalsIgnoreCase("werewolf"))
		{
			if (args.length == 0)
			{
				commandInfo(player);
				return true;
			}
			if (args[0].equalsIgnoreCase("setphase"))
			{
				commandSetPhase(sender, args);
				return true;
			}
			if (args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("addlevel"))
			{
				commandSetLevel(player, args, args[0].equalsIgnoreCase("addlevel"));
				return true;
			}
			if (args[0].equalsIgnoreCase("setalpha"))
			{
				commandSetAlpha(player, args);
				return true;
			}
			if (args[0].equalsIgnoreCase("cure"))
			{
				commandCure(player, args);
				return true;
			}
			if (args[0].equalsIgnoreCase("infect") && args.length >= 2)
			{
				commandInfect(player, args);
				return true;
			}
			if (args.length == 2)
			{
				if (args[0].equalsIgnoreCase("toggle"))
				{
					commandTogglePlayerWerewolf(player, args);
				}
				else if (args[0].equalsIgnoreCase("addbounty"))
				{
					commandAddBounty(player, args);
				}
				else if (args[0].equalsIgnoreCase("check"))
				{
					commandCheck(player, args);
				}
				else
				{
					this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.InvalidCommand, ChatColor.RED));
				}
				return true;
			}

			if (args.length == 1)
			{
				if (args[0].equalsIgnoreCase("reload"))
				{
					commandReload(player);
				}
				else if (args[0].equalsIgnoreCase("save"))
				{
					commandSave(player);
				}
				else if (args[0].equalsIgnoreCase("help"))
				{
					commandHelp(player);
				}
				else if (args[0].equalsIgnoreCase("clan"))
				{
					commandClan(player);
				}
				else if (args[0].equalsIgnoreCase("list"))
				{
					commandList(player);
				}
				else if (args[0].equalsIgnoreCase("transform"))
				{
					commandTransform(player);
				}
				else if (args[0].equalsIgnoreCase("intent"))
				{
					commandIntent(player);
				}
				else if (args[0].equalsIgnoreCase("toggle"))
				{
					commandToggleSelfWerewolf(player, args);
				}
				else if (args[0].equalsIgnoreCase("infect"))
				{
					commandInfectSelf(player, args);
				}
				else if (args[0].equalsIgnoreCase("bounty"))
				{
					commandBounty(player);
				}
				else if (args[0].equalsIgnoreCase("home"))
				{
					commandHome(player);
				}
				else if (args[0].equalsIgnoreCase("sethome"))
				{
					commandSetHome(player);
				}
				else if (args[0].equalsIgnoreCase("call"))
				{
					commandCall(player);
				}
				else if (args[0].equalsIgnoreCase("top"))
				{
					commandTopHunters(player);
				}
				else if (args[0].equalsIgnoreCase("hunt"))
				{
					commandHuntWerewolf(player);
				}
				else if (args[0].equalsIgnoreCase("infectionpotion"))
				{
					commandInfectionPotion(player);
				}
				else if (args[0].equalsIgnoreCase("curepotion"))
				{
					commandCurePotion(player);
				}
				else if (args[0].equalsIgnoreCase("wolfbane"))
				{
					commandWolfbanePotion(player);
				}
				else if (args[0].equalsIgnoreCase("silversword"))
				{
					commandSilverSword(player);
				}
				else if (args[0].equalsIgnoreCase("lorebook"))
				{
					commandLoreBook(player);
				}
				else if (args[0].equalsIgnoreCase("track"))
				{
					commandTrack(player);
				}
				else if (args[0].equalsIgnoreCase("tracker"))
				{
					commandGiveItems(player, "werewolf.tracker.create", LanguageManager.LANGUAGESTRING.CreatedWerewolfTracker, Werewolf.getItemManager().newWerewolfTracker());
				}
				else if (args[0].equalsIgnoreCase("vampiretracker"))
				{
					commandGiveItems(player, "werewolf.tracker.create", LanguageManager.LANGUAGESTRING.CreatedWerewolfTracker, Werewolf.getItemManager().newVampireTracker());
				}
				else if (args[0].equalsIgnoreCase("silverarmor"))
				{
					commandSilverArmor(player);
				}
				else if (args[0].equalsIgnoreCase("on"))
				{
					if (player != null && !player.isOp())
					{
						this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
						return false;
					}
					Werewolf.pluginEnabled = true;

					this.plugin.log(" has been enabled");
					if (player != null)
					{
						player.sendMessage(ChatColor.AQUA + "Werewolves are now enabled");
					}
					for (World world : this.plugin.getServer().getWorlds())
					{
						this.plugin.loadSettings();
						if (Werewolf.getMoonManager().isNight(world))
						{
							for (Player werewolf : world.getPlayers())
							{
								Werewolf.getWerewolfManager().setWerewolfSkin(werewolf, WerewolfTransformEvent.Reason.COMMAND);
							}
						}
					}
				}
				else if (args[0].equalsIgnoreCase("off"))
				{
					if (player != null && !player.isOp())
					{
						this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.NoPermissionForCommand, ChatColor.RED));
						return false;
					}

					Werewolf.pluginEnabled = false;
					this.plugin.log(" has been disabled");

					if (player != null)
					{
						player.sendMessage(ChatColor.AQUA + "Werewolves are now disabled");
					}

					for (World world : this.plugin.getServer().getWorlds())
					{
						this.plugin.saveSettings();

						for (Player werewolfPlayer : world.getPlayers())
						{
							Werewolf.getWerewolfManager().unsetWerewolfSkin(werewolfPlayer.getUniqueId(), true, WerewolfTransformEvent.Reason.COMMAND);
						}
					}
				}
				else
				{
					this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.InvalidCommand, ChatColor.RED));
				}
				return true;
			}
			return false;
		}
		return true;
	}

	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{
		Objects.requireNonNull(sender, "Sender cannot be null");
		Objects.requireNonNull(args, "Arguments cannot be null");
		Objects.requireNonNull(alias, "Alias cannot be null");

		List<String> result = new ArrayList<String>();

		Player player = null;
		if (sender instanceof Player)
		{
			player = (Player) sender;
		}

		// Each suggestion is checked against the same permission node as the command itself
		if (args.length == 1 && (cmd.getName().equalsIgnoreCase("werewolf") || cmd.getName().equalsIgnoreCase("ww")))
		{
			List<String> arg1 = new ArrayList<String>();
			arg1.add("help");
			addIfPermitted(arg1, player, "top", "werewolf.top");
			addIfPermitted(arg1, player, "hunt", "werewolf.hunt");
			addIfPermitted(arg1, player, "check", "werewolf.check");
			addIfPermitted(arg1, player, "bounty", "werewolf.bounty");
			addIfPermitted(arg1, player, "addbounty", "werewolf.addbounty");
			if (hasCommandPermission(player, "werewolf.togglewerewolf") || hasCommandPermission(player, "werewolf.togglewerewolfself"))
			{
				arg1.add("toggle");
			}
			if (hasCommandPermission(player, "werewolf.infect") || hasCommandPermission(player, "werewolf.infectself"))
			{
				arg1.add("infect");
			}
			addIfPermitted(arg1, player, "cure", "werewolf.cureother");
			addIfPermitted(arg1, player, "setlevel", "werewolf.setlevel");
			addIfPermitted(arg1, player, "addlevel", "werewolf.addlevel");
			if (this.plugin.useClans)
			{
				addIfPermitted(arg1, player, "setalpha", "werewolf.setalpha");
			}
			addIfPermitted(arg1, player, "setphase", "werewolf.setphase");
			addIfPermitted(arg1, player, "list", "werewolf.list");
			addIfPermitted(arg1, player, "reload", "werewolf.reload");
			addIfPermitted(arg1, player, "save", "werewolf.save");
			if (player == null || player.isOp())
			{
				arg1.add("on");
				arg1.add("off");
			}
			if (player != null)
			{
				addIfPermitted(arg1, player, "transform", "werewolf.transform");
				addIfPermitted(arg1, player, "intent", "werewolf.intent");
				addIfPermitted(arg1, player, "infectionpotion", "werewolf.potion.infection.create");
				addIfPermitted(arg1, player, "curepotion", "werewolf.potion.cure.create");
				addIfPermitted(arg1, player, "wolfbane", "werewolf.potion.wolfbane.create");
				addIfPermitted(arg1, player, "silversword", "werewolf.silversword.create");
				addIfPermitted(arg1, player, "lorebook", "werewolf.lorebook.create");
				addIfPermitted(arg1, player, "track", "werewolf.track");
				addIfPermitted(arg1, player, "tracker", "werewolf.tracker.create");
				if (this.plugin.vampireEnabled)
				{
					addIfPermitted(arg1, player, "vampiretracker", "werewolf.tracker.create");
				}
				addIfPermitted(arg1, player, "silverarmor", "werewolf.silverarmor.create");
				if (this.plugin.useClans)
				{
					addIfPermitted(arg1, player, "clan", "werewolf.clan");
					addIfPermitted(arg1, player, "home", "werewolf.home");
					if (Werewolf.getClanManager().isAlpha(player.getUniqueId()))
					{
						addIfPermitted(arg1, player, "call", "werewolf.alpha.call");
						addIfPermitted(arg1, player, "sethome", "werewolf.sethome");
					}
				}
			}
			StringUtil.copyPartialMatches(args[0], arg1, result);
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("addbounty") && hasCommandPermission(player, "werewolf.addbounty"))
		{
			List<String> arg2 = Arrays.asList("100", "200", "300", "500", "1000", "2000", "2500", "5000");
			StringUtil.copyPartialMatches(args[1], arg2, result);
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("setphase") && hasCommandPermission(player, "werewolf.setphase"))
		{
			List<String> phases = new ArrayList<String>();
			for (MoonManager.MoonPhase phase : MoonManager.MoonPhase.values())
			{
				phases.add(phase.name());
			}
			StringUtil.copyPartialMatches(args[1], phases, result);
		}
		else if (args.length == 3 && args[0].equalsIgnoreCase("setphase") && hasCommandPermission(player, "werewolf.setphase"))
		{
			List<String> worlds = new ArrayList<String>();
			for (World world : this.plugin.getServer().getWorlds())
			{
				worlds.add(world.getName());
			}
			StringUtil.copyPartialMatches(args[2], worlds, result);
		}
		else if (args.length == 2 && ((args[0].equalsIgnoreCase("check") && hasCommandPermission(player, "werewolf.check"))
				|| (args[0].equalsIgnoreCase("infect") && hasCommandPermission(player, "werewolf.infect"))
				|| (args[0].equalsIgnoreCase("toggle") && hasCommandPermission(player, "werewolf.togglewerewolf"))))
		{
			return null;
		}
		else if (args.length == 3 && args[0].equalsIgnoreCase("infect") && hasCommandPermission(player, "werewolf.infect"))
		{
			StringUtil.copyPartialMatches(args[2], getClanNames(), result);
		}
		else if (args.length == 2 && ((args[0].equalsIgnoreCase("cure") && hasCommandPermission(player, "werewolf.cureother"))
				|| (args[0].equalsIgnoreCase("setlevel") && hasCommandPermission(player, "werewolf.setlevel"))
				|| (args[0].equalsIgnoreCase("addlevel") && hasCommandPermission(player, "werewolf.addlevel"))
				|| (args[0].equalsIgnoreCase("setalpha") && hasCommandPermission(player, "werewolf.setalpha"))))
		{
			// Werewolves, including offline ones
			Set<String> names = new HashSet<String>(Werewolf.getWerewolfManager().getWerewolfPlayerNames());
			for (Player online : this.plugin.getServer().getOnlinePlayers())
			{
				if (Werewolf.getWerewolfManager().isWerewolf(online))
				{
					names.add(online.getName());
				}
			}
			StringUtil.copyPartialMatches(args[1], names, result);
		}
		else if (args.length == 3 && ((args[0].equalsIgnoreCase("setlevel") && hasCommandPermission(player, "werewolf.setlevel"))
				|| (args[0].equalsIgnoreCase("addlevel") && hasCommandPermission(player, "werewolf.addlevel"))))
		{
			StringUtil.copyPartialMatches(args[2], Arrays.asList("1", "5", "10", "20", "50"), result);
		}

		Collections.sort(result);
		return result;
	}

	public class Hunter
	{
		public String	name;
		public int		kills;

		Hunter(String hunterName, int k)
		{
			this.name = hunterName;
			this.kills = k;
		}
	}

	public class TopHuntersComparator implements Comparator<Commands.Hunter>
	{
		public int compare(Commands.Hunter object1, Commands.Hunter object2)
		{
			Commands.Hunter h1 = object1;
			Commands.Hunter h2 = object2;

			return h2.kills - h1.kills;
		}
	}
}