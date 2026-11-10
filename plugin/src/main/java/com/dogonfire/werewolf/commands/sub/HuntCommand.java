package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww hunt: toggles werewolf hunt mode, which hands the player a compass */
public class HuntCommand extends SubCommand
{
	public HuntCommand(WerewolfContext ctx)
	{
		super(ctx, "hunt", "werewolf.hunt", true, "/ww hunt", LANGUAGESTRING.HelpHunt);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (ctx.getHuntManager() == null)
		{
			send(player, ChatColor.RED + "Vault not detected. Werewolf hunts & bounties are disabled.");
			return;
		}

		if (!ctx.getWerewolfManager().isWerewolvesAllowedInWorld(player))
		{
			lang().setType(player.getWorld().getName());
			send(player, LANGUAGESTRING.NoWerewolvesInThisWorld, ChatColor.RED);
			return;
		}

		if (ctx.getWerewolfManager().isWerewolf(player))
		{
			send(player, LANGUAGESTRING.WerewolvesCannotHuntWerewolves, ChatColor.RED);
			return;
		}

		if (!ctx.getHuntManager().isHunting(player.getUniqueId()))
		{
			if (ctx.getWerewolfManager().getOnlineWerewolvesInWolfForm(player.getWorld()).size() == 0)
			{
				send(player, LANGUAGESTRING.NoWerewolvesOnline, ChatColor.RED);
				return;
			}

			if (player.getInventory().getItemInMainHand().getType() != Material.AIR)
			{
				send(player, LANGUAGESTRING.YouMustHaveYourHandsFree, ChatColor.RED);
				return;
			}

			player.getInventory().setItemInMainHand(new ItemStack(Material.COMPASS));

			lang().setPlayerName(player.getName());
			this.plugin.getServer().broadcastMessage(text(LANGUAGESTRING.PlayerIsHuntingWerewolves, ChatColor.GOLD));

			ctx.getHuntManager().setHunting(player.getUniqueId(), true);
		}
		else
		{
			if (player.getInventory().getItemInMainHand().getType() != Material.COMPASS)
			{
				send(player, LANGUAGESTRING.TakeCompassInHands, ChatColor.RED);
				return;
			}
			player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
			lang().setPlayerName(player.getName());
			this.plugin.getServer().broadcastMessage(text(LANGUAGESTRING.PlayerIsNoLongerHuntingWerewolves, ChatColor.GOLD));
			ctx.getHuntManager().setHunting(player.getUniqueId(), false);
		}

		this.plugin.log(player.getName() + ": /werewolf hunt");
	}
}
