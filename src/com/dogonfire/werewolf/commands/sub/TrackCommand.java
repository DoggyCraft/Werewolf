package com.dogonfire.werewolf.commands.sub;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;
import com.dogonfire.werewolf.managers.ScentManager;

/** /ww track: toggles following the scent trail of the player the werewolf last sniffed */
public class TrackCommand extends SubCommand
{
	public TrackCommand(WerewolfContext ctx)
	{
		super(ctx, "track", "werewolf.track", true, "/ww track", LANGUAGESTRING.HelpTrack);
	}

	@Override
	public List<HelpEntry> help(Player player)
	{
		return ctx.getWerewolfManager().isWerewolf(player) ? super.help(player) : List.of();
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!ctx.getWerewolfManager().isWerewolf(player))
		{
			send(player, LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED);
			return;
		}

		ScentManager scent = ctx.getScentManager();
		if (scent.isTracking(player.getUniqueId()))
		{
			scent.stopTracking(player);
			send(player, LANGUAGESTRING.ScentTrackStopped, ChatColor.LIGHT_PURPLE);
			return;
		}
		if (!scent.canScentTrack(player.getUniqueId()))
		{
			int fullMoons = this.plugin.getSettings().maturity().scentTrack() - ctx.getWerewolfManager().getNumberOfTransformations(player.getUniqueId());
			lang().setAmount("" + fullMoons);
			send(player, LANGUAGESTRING.MustExperienceMoreFullMoons, ChatColor.RED);
			return;
		}
		if (!ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			send(player, LANGUAGESTRING.ScentTrackNotInWolfForm, ChatColor.RED);
			return;
		}

		UUID targetId = scent.getTarget(player.getUniqueId());
		if (targetId == null)
		{
			send(player, LANGUAGESTRING.ScentNoTarget, ChatColor.RED);
			return;
		}
		Player target = this.plugin.getServer().getPlayer(targetId);
		if (target == null)
		{
			send(player, LANGUAGESTRING.ScentTargetNotFound, ChatColor.RED);
			return;
		}
		if (!target.getWorld().equals(player.getWorld()))
		{
			send(player, LANGUAGESTRING.ScentTargetOtherWorld, ChatColor.RED);
			return;
		}

		scent.startTracking(player);
		lang().setPlayerName(target.getName());
		send(player, LANGUAGESTRING.ScentTrackStarted, ChatColor.LIGHT_PURPLE);
	}
}
