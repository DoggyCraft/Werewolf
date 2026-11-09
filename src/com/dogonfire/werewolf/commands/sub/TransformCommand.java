package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.api.event.WerewolfTransformEvent;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww transform: a mature werewolf changes form at will, outside transformation nights */
public class TransformCommand extends SubCommand
{
	public TransformCommand(WerewolfContext ctx)
	{
		super(ctx, "transform", "werewolf.transform", true, "/ww transform", LANGUAGESTRING.HelpTransform);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!ctx.getWerewolfManager().isWerewolf(player))
		{
			send(player, LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED);
			return;
		}

		int needed = this.plugin.getSettings().maturity().controlledTransformation();
		int level = ctx.getWerewolfManager().getNumberOfTransformations(player.getUniqueId());
		if (level < needed)
		{
			lang().setAmount("" + (needed - level));
			send(player, LANGUAGESTRING.MustExperienceMoreFullMoons, ChatColor.RED);
			return;
		}

		if (ctx.getMoonManager().isTransformationNight(player.getWorld()))
		{
			send(player, LANGUAGESTRING.CannotControlTransformationTonight, ChatColor.RED);
			return;
		}

		if (ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			ctx.getWerewolfManager().untransform(player, WerewolfTransformEvent.Reason.CONTROLLED);
			return;
		}

		long cooldownLeft = ctx.getWerewolfManager().getTransformCooldownLeft(player.getUniqueId());
		if (cooldownLeft > 0)
		{
			lang().setAmount("" + cooldownLeft);
			send(player, LANGUAGESTRING.CannotTransformSoSoon, ChatColor.RED);
			return;
		}

		ctx.getWerewolfManager().transform(player, WerewolfTransformEvent.Reason.CONTROLLED);

		long timeLimit = ctx.getWerewolfManager().getTransformationTimeLimitSeconds(player.getUniqueId());
		if (timeLimit >= 0 && ctx.getWerewolfManager().isControlledForm(player.getUniqueId()))
		{
			lang().setAmount("" + timeLimit);
			send(player, LANGUAGESTRING.TransformationTimeLimit, ChatColor.LIGHT_PURPLE);
		}
	}
}
