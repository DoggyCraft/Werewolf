package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww home: teleports a werewolf to their clan home */
public class HomeCommand extends SubCommand
{
	public HomeCommand(WerewolfContext ctx)
	{
		super(ctx, "home", "werewolf.home", true, "/ww home", LANGUAGESTRING.HelpHome);
	}

	@Override
	public boolean isAvailable(Player player)
	{
		return this.plugin.getSettings().clans().enabled();
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!ctx.getWerewolfManager().isWerewolf(player))
		{
			send(player, LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED);
			return;
		}

		Clan playerClan = ctx.getWerewolfManager().getWerewolfClan(player.getUniqueId());

		Location location = ctx.getClanManager().getHomeForClan(playerClan);
		if (location == null)
		{
			lang().setType(ctx.getClanManager().getClanName(playerClan));
			send(player, LANGUAGESTRING.ClanHomeNotSet, ChatColor.RED);
			return;
		}

		player.teleport(location);

		lang().setType(ctx.getClanManager().getClanName(playerClan));
		send(player, LANGUAGESTRING.YouTeleportedToClanHome, ChatColor.GREEN);
	}
}
