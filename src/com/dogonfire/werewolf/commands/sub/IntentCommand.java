package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww intent: toggles a werewolf's intent to infect, which raises the infection chance of their bites */
public class IntentCommand extends SubCommand
{
	public IntentCommand(Werewolf plugin)
	{
		super(plugin, "intent", "werewolf.intent", true, "/ww intent", LANGUAGESTRING.HelpIntent);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!Werewolf.getWerewolfManager().isFullWerewolf(player.getUniqueId()))
		{
			send(player, LANGUAGESTRING.WerewolfCommandIsNotWerewolf, ChatColor.RED);
			return;
		}
		if (!Werewolf.getWerewolfManager().canUseIntent(player.getUniqueId()))
		{
			int fullMoons = this.plugin.getSettings().maturity().intent() - Werewolf.getWerewolfManager().getNumberOfTransformations(player.getUniqueId());
			lang().setAmount("" + fullMoons);
			send(player, LANGUAGESTRING.MustExperienceMoreFullMoons, ChatColor.RED);
			return;
		}

		boolean intent = !Werewolf.getWerewolfManager().hasIntent(player.getUniqueId());
		Werewolf.getWerewolfManager().setIntent(player.getUniqueId(), intent);

		send(player, intent ? LANGUAGESTRING.IntentEnabled : LANGUAGESTRING.IntentDisabled, ChatColor.LIGHT_PURPLE);
	}
}
