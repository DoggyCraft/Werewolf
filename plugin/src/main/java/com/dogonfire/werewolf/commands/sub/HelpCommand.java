package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.commands.WerewolfCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /ww help: lists the commands the player can use */
public class HelpCommand extends SubCommand
{
	private final WerewolfCommand root;

	public HelpCommand(WerewolfContext ctx, WerewolfCommand root)
	{
		super(ctx, "help", null, false, null, null);
		this.root = root;
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (player == null)
		{
			this.plugin.log("/werewolf - Show basic info");
			this.plugin.log("/howl - Howl as a Werewolf!");
			this.plugin.log("/growl - Growl as a Werewolf!");
			this.plugin.log("/werewolf top - View the top Werewolf hunters in " + this.plugin.getSettings().general().serverName());
			this.plugin.log("/werewolf hunt - Toggles Werewolf hunt mode");
			this.plugin.log("/werewolf check <playername> - Check Werewolf status for a player");
			this.plugin.log("/werewolf bounty - Check the current bounty for killing a Werewolf");
			this.plugin.log("/werewolf addbounty - Add to the bounty for killing a Werewolf");
			this.plugin.log("/werewolf toggle - Toggles Werewolf status for yourself");
			this.plugin.log("/werewolf toggle <playername> - Toggles Werewolf status for another player");
			this.plugin.log("/werewolf infect <playername> [clan] - Infects a player with the Werewolf infection");
			this.plugin.log("/werewolf cure <playername> - Cures a player of being a Werewolf");
			this.plugin.log("/werewolf setlevel <playername> <level> - Sets a Werewolf's level");
			this.plugin.log("/werewolf addlevel <playername> <amount> - Adds levels to a Werewolf");
			this.plugin.log("/werewolf setalpha <playername> - Makes a Werewolf the alpha of their clan");
			this.plugin.log("/werewolf setphase <phase> [world] - Sets the moon phase of a world");
			return;
		}

		player.sendMessage(ChatColor.YELLOW + "---------- " + this.plugin.getDescription().getFullName() + " ----------");
		lang().setType("/werewolf help");
		player.sendMessage(text(LANGUAGESTRING.HelpWerewolf, ChatColor.AQUA));

		for (SubCommand subCommand : this.root.getSubCommands())
		{
			for (HelpEntry entry : subCommand.help(player))
			{
				lang().setType(entry.usage());
				player.sendMessage(text(entry.key(), ChatColor.AQUA));
			}
		}

		this.plugin.log(player.getName() + ": /werewolf help");
	}
}
