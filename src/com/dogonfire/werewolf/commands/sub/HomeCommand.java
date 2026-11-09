package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.ClanManager.ClanType;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww home: teleports a werewolf to their clan home */
public class HomeCommand extends SubCommand
{
	public HomeCommand(Werewolf plugin)
	{
		super(plugin, "home", "werewolf.home", true, "/ww home", LANGUAGESTRING.HelpHome);
	}

	@Override
	public boolean isAvailable(Player player)
	{
		return this.plugin.getSettings().clans().enabled();
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!Werewolf.getWerewolfManager().isWerewolf(player))
		{
			send(player, LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED);
			return;
		}

		ClanType playerClan = Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId());

		Location location = Werewolf.getClanManager().getHomeForClan(playerClan);
		if (location == null)
		{
			return;
		}

		player.teleport(location);

		lang().setType(Werewolf.getClanManager().getClanName(playerClan));
		send(player, LANGUAGESTRING.YouTeleportedToClanHome, ChatColor.GREEN);
	}
}
