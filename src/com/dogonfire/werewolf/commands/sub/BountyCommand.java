package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww bounty: the current bounty for killing a werewolf */
public class BountyCommand extends SubCommand
{
	public BountyCommand(WerewolfContext ctx)
	{
		super(ctx, "bounty", "werewolf.bounty", false, "/ww bounty", LANGUAGESTRING.HelpBounty);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (ctx.getHuntManager() == null)
		{
			send(player, ChatColor.RED + "Vault not detected. Werewolf hunts & bounties are disabled.");
			return;
		}

		lang().setAmount(ctx.getHuntManager().getFormattedBounty());
		if (player == null)
		{
			this.plugin.log(text(LANGUAGESTRING.BountyTotal, ChatColor.WHITE));
		}
		else
		{
			player.sendMessage(text(LANGUAGESTRING.BountyTotal, ChatColor.AQUA));
			this.plugin.log(player.getName() + ": /werewolf bounty");
		}
	}
}
