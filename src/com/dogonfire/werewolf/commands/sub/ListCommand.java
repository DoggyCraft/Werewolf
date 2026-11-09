package com.dogonfire.werewolf.commands.sub;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.ClanManager.ClanType;

/** /ww list: the top members of the player's clan */
public class ListCommand extends SubCommand
{
	public ListCommand(WerewolfContext ctx)
	{
		super(ctx, "list", "werewolf.list", true, null, null);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!ctx.getWerewolfManager().isWerewolf(player))
		{
			player.sendMessage(ChatColor.RED + "You are not a werewolf");
			return;
		}

		ClanType clan = ctx.getWerewolfManager().getWerewolfClan(player.getUniqueId());
		List<UUID> list = ctx.getWerewolfManager().getWerewolfClanMembersRanked(clan);

		player.sendMessage(ChatColor.GOLD + "--------- The Members of " + ctx.getClanManager().getClanName(clan) + " ---------");
		if (list.size() > 15)
		{
			list = list.subList(0, 15);
		}

		int n = 1;
		for (UUID memberId : list)
		{
			String memberName = this.plugin.getServer().getOfflinePlayer(memberId).getName();
			int numberOfTransformations = ctx.getWerewolfManager().getNumberOfTransformations(memberId);
			String rank = String.format("%2d", n);

			if (Objects.equals(memberName, player.getName()))
			{
				player.sendMessage(ChatColor.GOLD + rank + " - " + memberName + ChatColor.GOLD + "      level " + numberOfTransformations);
			}
			else
			{
				player.sendMessage(ChatColor.YELLOW + rank + ChatColor.AQUA + " - " + memberName + ChatColor.GOLD + "      level " + numberOfTransformations);
			}
			n++;
		}

		this.plugin.sendInfo(player, ChatColor.AQUA + "Use " + ChatColor.GOLD + "/ww clan" + ChatColor.AQUA + " to see info about your clan");
	}
}
