package com.dogonfire.werewolf.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.sub.AddBountyCommand;
import com.dogonfire.werewolf.commands.sub.BountyCommand;
import com.dogonfire.werewolf.commands.sub.CallCommand;
import com.dogonfire.werewolf.commands.sub.CheckCommand;
import com.dogonfire.werewolf.commands.sub.ClanCommand;
import com.dogonfire.werewolf.commands.sub.CureCommand;
import com.dogonfire.werewolf.commands.sub.GiveItemCommand;
import com.dogonfire.werewolf.commands.sub.HelpCommand;
import com.dogonfire.werewolf.commands.sub.HomeCommand;
import com.dogonfire.werewolf.commands.sub.HuntCommand;
import com.dogonfire.werewolf.commands.sub.InfectCommand;
import com.dogonfire.werewolf.commands.sub.IntentCommand;
import com.dogonfire.werewolf.commands.sub.ListCommand;
import com.dogonfire.werewolf.commands.sub.PluginToggleCommand;
import com.dogonfire.werewolf.commands.sub.ReloadCommand;
import com.dogonfire.werewolf.commands.sub.SaveCommand;
import com.dogonfire.werewolf.commands.sub.SetAlphaCommand;
import com.dogonfire.werewolf.commands.sub.SetHomeCommand;
import com.dogonfire.werewolf.commands.sub.SetLevelCommand;
import com.dogonfire.werewolf.commands.sub.SetPhaseCommand;
import com.dogonfire.werewolf.commands.sub.ToggleCommand;
import com.dogonfire.werewolf.commands.sub.TopCommand;
import com.dogonfire.werewolf.commands.sub.TrackCommand;
import com.dogonfire.werewolf.commands.sub.TransformCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/**
 * /werewolf (/ww): shows the info screen without arguments, otherwise dispatches on args[0] to a registered {@link SubCommand}.
 * Console use and permission are checked here, before the subcommand runs.
 */
public class WerewolfCommand implements TabExecutor
{
	static final String CONSOLE_MESSAGE = "You cannot use this command from the console.";

	private final Werewolf plugin;
	private final InfoScreen infoScreen;
	private final List<SubCommand> subCommands = new ArrayList<SubCommand>();

	public WerewolfCommand(Werewolf plugin)
	{
		this.plugin = plugin;
		this.infoScreen = new InfoScreen(plugin);

		// Registration order is the order of the /ww help listing
		register(new HuntCommand(plugin));
		register(new TopCommand(plugin));
		register(new BountyCommand(plugin));
		register(new AddBountyCommand(plugin));
		register(new TransformCommand(plugin));
		register(new IntentCommand(plugin));
		register(new SetPhaseCommand(plugin));
		register(new CheckCommand(plugin));
		register(new InfectCommand(plugin));
		register(new CureCommand(plugin));
		register(new SetLevelCommand(plugin, false));
		register(new SetLevelCommand(plugin, true));
		register(new SetAlphaCommand(plugin));
		register(new ToggleCommand(plugin));
		register(GiveItemCommand.infectionPotion(plugin));
		register(GiveItemCommand.curePotion(plugin));
		register(GiveItemCommand.wolfbane(plugin));
		register(GiveItemCommand.silverSword(plugin));
		register(GiveItemCommand.loreBook(plugin));
		register(GiveItemCommand.silverArmor(plugin));
		register(GiveItemCommand.werewolfTracker(plugin));
		register(new TrackCommand(plugin));
		register(new ClanCommand(plugin));
		register(new HomeCommand(plugin));
		register(new CallCommand(plugin));
		register(new SetHomeCommand(plugin));
		// Not in the help listing
		register(new HelpCommand(plugin, this));
		register(new ReloadCommand(plugin));
		register(new SaveCommand(plugin));
		register(new ListCommand(plugin));
		register(GiveItemCommand.vampireTracker(plugin));
		register(new PluginToggleCommand(plugin, true));
		register(new PluginToggleCommand(plugin, false));
	}

	public void register(SubCommand subCommand)
	{
		this.subCommands.add(subCommand);
	}

	public List<SubCommand> getSubCommands()
	{
		return Collections.unmodifiableList(this.subCommands);
	}

	public SubCommand find(String label)
	{
		for (SubCommand subCommand : this.subCommands)
		{
			if (subCommand.matches(label))
			{
				return subCommand;
			}
		}
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player = sender instanceof Player p ? p : null;

		if (args.length == 0)
		{
			this.infoScreen.show(player);
			return true;
		}

		SubCommand subCommand = find(args[0]);
		if (subCommand == null || !subCommand.acceptsArgs(args.length))
		{
			// Unknown commands with many arguments show the plugin.yml usage
			if (args.length > 2)
			{
				return false;
			}
			this.plugin.sendInfo(player, Werewolf.getLanguageManager().getLanguageString(LANGUAGESTRING.InvalidCommand, ChatColor.RED));
			return true;
		}

		if (player == null && subCommand.isPlayerOnly(args))
		{
			this.plugin.log(CONSOLE_MESSAGE);
			return true;
		}

		if (!subCommand.isPermitted(player, args))
		{
			CommandUtil.sendNoPermission(this.plugin, player);
			return true;
		}

		subCommand.execute(sender, player, args);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{
		Player player = sender instanceof Player p ? p : null;
		List<String> result = new ArrayList<String>();

		if (args.length == 1)
		{
			List<String> names = new ArrayList<String>();
			for (SubCommand subCommand : this.subCommands)
			{
				if (subCommand.isListed(player))
				{
					names.add(subCommand.name());
				}
			}
			StringUtil.copyPartialMatches(args[0], names, result);
		}
		else if (args.length > 1)
		{
			SubCommand subCommand = find(args[0]);
			if (subCommand != null && subCommand.isPermitted(player, args))
			{
				List<String> suggestions = subCommand.tabComplete(sender, player, args);
				if (suggestions == null)
				{
					return null;
				}
				result.addAll(suggestions);
			}
		}

		Collections.sort(result);
		return result;
	}
}
