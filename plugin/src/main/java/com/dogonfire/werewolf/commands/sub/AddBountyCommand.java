package com.dogonfire.werewolf.commands.sub;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww addbounty <amount>: pays into the werewolf bounty */
public class AddBountyCommand extends SubCommand
{
	private static final List<String> AMOUNTS = List.of("100", "200", "300", "500", "1000", "2000", "2500", "5000");

	public AddBountyCommand(WerewolfContext ctx)
	{
		super(ctx, "addbounty", "werewolf.addbounty", true, "/ww addbounty <amount>", LANGUAGESTRING.HelpAddBounty);
	}

	@Override
	public boolean acceptsArgs(int length)
	{
		return length == 2;
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (ctx.getHuntManager() == null)
		{
			send(player, ChatColor.RED + "Vault not detected. Werewolf hunts & bounties are disabled.");
			return;
		}

		if (ctx.getWerewolfManager().isWerewolf(player))
		{
			send(player, ChatColor.RED + "Werewolves cannot add to the Werewolf bounty!");
			return;
		}

		int bounty;
		try
		{
			bounty = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException ex)
		{
			send(player, ChatColor.RED + "Come on, that is not a valid bounty :/");
			return;
		}

		if (bounty <= 0)
		{
			send(player, ChatColor.RED + "How about adding a real amount?");
			return;
		}

		ctx.getHuntManager().addBounty(player.getName(), bounty);

		this.plugin.log(player.getName() + ": /werewolf addbounty " + bounty);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, Player player, String[] args)
	{
		List<String> result = new ArrayList<String>();
		if (args.length == 2)
		{
			StringUtil.copyPartialMatches(args[1], AMOUNTS, result);
		}
		return result;
	}
}
