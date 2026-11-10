package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /growl, for werewolves in wolf form */
public class GrowlCommand extends SubCommand
{
	public GrowlCommand(WerewolfContext ctx)
	{
		super(ctx, "growl", "werewolf.growl", true, null, null);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!ctx.getWerewolfManager().isWolfForm(player.getUniqueId()))
		{
			send(player, ChatColor.RED + "You are not in wolf form");
			return;
		}
		long cooldownLeft = ctx.getWerewolfManager().getGrowlCooldownLeft(player.getUniqueId());
		if (cooldownLeft > 0)
		{
			lang().setAmount("" + cooldownLeft);
			send(player, LANGUAGESTRING.AbilityOnCooldown, ChatColor.RED);
			return;
		}
		ctx.getWerewolfManager().setLastGrowl(player.getUniqueId());
		ctx.getWerewolfManager().growl(player);
	}
}
