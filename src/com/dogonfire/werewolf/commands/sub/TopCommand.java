package com.dogonfire.werewolf.commands.sub;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww top: the ten werewolf hunters with the most kills */
public class TopCommand extends SubCommand
{
	private record Hunter(String name, int kills)
	{
	}

	public TopCommand(WerewolfContext ctx)
	{
		super(ctx, "top", "werewolf.top", false, "/ww top", LANGUAGESTRING.HelpTop);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (ctx.getHuntManager() == null)
		{
			send(player, ChatColor.RED + "Vault not detected. Werewolf hunts & bounties are disabled.");
			return;
		}

		List<Hunter> hunters = new ArrayList<Hunter>();
		for (String hunterName : ctx.getHuntManager().getHunters())
		{
			int kills = ctx.getHuntManager().getHunterKills(hunterName);
			if (kills > 0)
			{
				hunters.add(new Hunter(hunterName, kills));
			}
		}

		if (hunters.isEmpty())
		{
			send(player, text(LANGUAGESTRING.NoWerewolfHuntersInWorld, player != null ? ChatColor.RED : ChatColor.WHITE));
			return;
		}

		send(player, text(LANGUAGESTRING.TopWerewolfHunters, player != null ? ChatColor.RED : ChatColor.WHITE));

		hunters.sort(Comparator.comparingInt(Hunter::kills).reversed());
		if (hunters.size() > 10)
		{
			hunters = hunters.subList(0, 10);
		}

		int n = 1;
		for (Hunter hunter : hunters)
		{
			send(player, ChatColor.YELLOW + "" + n + ChatColor.AQUA + " - " + String.format("%-15s", hunter.name()) + hunter.kills() + " Kills");
			n++;
		}

		if (player != null)
		{
			this.plugin.log(player.getName() + ": /werewolf top");
		}
	}
}
