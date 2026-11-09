package com.dogonfire.werewolf.commands.sub;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.ClanManager.ClanType;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww call: the clan alpha howls to call the online clan members */
public class CallCommand extends SubCommand
{
	public CallCommand(WerewolfContext ctx)
	{
		super(ctx, "call", "werewolf.alpha.call", true, "/ww call", LANGUAGESTRING.HelpCall);
	}

	@Override
	public boolean isAvailable(Player player)
	{
		return this.plugin.getSettings().clans().enabled() && player != null && ctx.getClanManager().isAlpha(player.getUniqueId());
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!ctx.getWerewolfManager().isWolfForm(player.getUniqueId()))
		{
			send(player, "You are not in wolf form");
			return;
		}
		ClanType playerClan = ctx.getWerewolfManager().getWerewolfClan(player.getUniqueId());
		if (!ctx.getClanManager().isAlpha(playerClan, player.getUniqueId()))
		{
			send(player, LANGUAGESTRING.YouAreNotTheAlphaOfTheClan, ChatColor.RED);
			return;
		}
		ctx.getClanManager().setLastCall(playerClan);

		ctx.getWerewolfManager().howl(player);
		for (UUID memberId : ctx.getWerewolfManager().getWerewolfClanMembersRanked(playerClan))
		{
			Player clanPlayer = this.plugin.getServer().getPlayer(memberId);
			if (clanPlayer != null)
			{
				lang().setPlayerName(player.getName());
				send(clanPlayer, LANGUAGESTRING.YouAreBeingCalled, ChatColor.RED);

				lang().setPlayerName(clanPlayer.getName());
				send(player, LANGUAGESTRING.YouCalledClanMember, ChatColor.RED);
			}
		}
	}
}
