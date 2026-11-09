package com.dogonfire.werewolf.commands.sub;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.CommandUtil;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.ClanManager;
import com.dogonfire.werewolf.managers.ClanManager.ClanType;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww clan: info about the werewolf's clan and the clan ranking */
public class ClanCommand extends SubCommand
{
	public ClanCommand(Werewolf plugin)
	{
		super(plugin, "clan", "werewolf.clan", true, "/ww clan", LANGUAGESTRING.HelpClan);
	}

	@Override
	public boolean isAvailable(Player player)
	{
		return this.plugin.getSettings().clans().enabled();
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!this.plugin.getSettings().clans().enabled())
		{
			send(player, ChatColor.RED + "Clans are not enabled.");
			return;
		}
		if (!Werewolf.getWerewolfManager().isWerewolf(player))
		{
			send(player, LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED);
			return;
		}

		ClanManager clans = Werewolf.getClanManager();
		String clanName = clans.getClanName(player.getUniqueId());
		String alphaName = "None!";
		ClanType playerClan = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());
		UUID alphaId = clans.getAlpha(playerClan);

		if (alphaId != null)
		{
			alphaName = this.plugin.getServer().getOfflinePlayer(alphaId).getName();
		}

		send(player, ChatColor.GOLD + "------------ The " + clanName + " Clan ------------");

		send(player, "" + ChatColor.AQUA);
		send(player, "" + ChatColor.AQUA + " Clan Alpha : " + ChatColor.WHITE + alphaName);
		send(player, "" + ChatColor.AQUA);

		if (alphaId != null && !player.getUniqueId().equals(alphaId))
		{
			send(player, ChatColor.RED + "Kill " + ChatColor.GOLD + alphaName + ChatColor.RED + " to take over the position as alpha!");
			send(player, "" + ChatColor.AQUA);
		}

		send(player, ChatColor.GOLD + "------------ The Werewolf Clans ------------");

		List<ClanType> clanList = clans.getClansRanked();

		int n = 1;
		for (ClanType clan : clanList)
		{
			ChatColor nameColor = clan.equals(playerClan) ? ChatColor.GOLD : ChatColor.WHITE;
			send(player, "" + ChatColor.GOLD + n + ") " + nameColor + clans.getClanName(clan) + ChatColor.AQUA + "  -  " + Werewolf.getWerewolfManager().getWerewolfClanMembers(clan).size() + " members  -  "
					+ String.format("%1$,.2f", clans.getClanPoint(clan)) + " clan points");
			n++;
		}

		send(player, "" + ChatColor.GOLD);

		if (clans.isAlpha(playerClan, player.getUniqueId()))
		{
			if (CommandUtil.hasPermission(player, "werewolf.sethome"))
			{
				lang().setType("/ww sethome");
				player.sendMessage(text(LANGUAGESTRING.HelpSetHome, ChatColor.AQUA));
			}
		}
		else if (CommandUtil.hasPermission(player, "werewolf.home"))
		{
			lang().setType("/ww home");
			player.sendMessage(text(LANGUAGESTRING.HelpHome, ChatColor.AQUA));
		}

		send(player, ChatColor.AQUA + "Use " + ChatColor.GOLD + "/ww list" + ChatColor.AQUA + " to see the list of members in your clan");
	}
}
