package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.events.WerewolfTransformEvent;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww transform: a mature werewolf changes form at will, outside transformation nights */
public class TransformCommand extends SubCommand
{
	public TransformCommand(Werewolf plugin)
	{
		super(plugin, "transform", "werewolf.transform", true, "/ww transform", LANGUAGESTRING.HelpTransform);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!Werewolf.getWerewolfManager().isWerewolf(player))
		{
			send(player, LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED);
			return;
		}

		int needed = this.plugin.getSettings().maturity().controlledTransformation();
		int level = Werewolf.getWerewolfManager().getNumberOfTransformations(player.getUniqueId());
		if (level < needed)
		{
			lang().setAmount("" + (needed - level));
			send(player, LANGUAGESTRING.MustExperienceMoreFullMoons, ChatColor.RED);
			return;
		}

		if (Werewolf.getMoonManager().isTransformationNight(player.getWorld()))
		{
			send(player, LANGUAGESTRING.CannotControlTransformationTonight, ChatColor.RED);
			return;
		}

		if (Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			this.plugin.untransform(player, WerewolfTransformEvent.Reason.CONTROLLED);
			return;
		}

		long cooldownLeft = Werewolf.getWerewolfManager().getTransformCooldownLeft(player.getUniqueId());
		if (cooldownLeft > 0)
		{
			lang().setAmount("" + cooldownLeft);
			send(player, LANGUAGESTRING.CannotTransformSoSoon, ChatColor.RED);
			return;
		}

		this.plugin.transform(player, WerewolfTransformEvent.Reason.CONTROLLED);

		long timeLimit = Werewolf.getWerewolfManager().getTransformationTimeLimitSeconds(player.getUniqueId());
		if (timeLimit >= 0 && Werewolf.getWerewolfManager().isControlledForm(player.getUniqueId()))
		{
			lang().setAmount("" + timeLimit);
			send(player, LANGUAGESTRING.TransformationTimeLimit, ChatColor.LIGHT_PURPLE);
		}
	}
}
