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
import com.dogonfire.werewolf.WerewolfContext;
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
	private final WerewolfContext ctx;
	private final InfoScreen infoScreen;
	private final List<SubCommand> subCommands = new ArrayList<SubCommand>();

	public WerewolfCommand(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.infoScreen = new InfoScreen(ctx);

		// Registration order is the order of the /ww help listing
		register(new HuntCommand(ctx));
		register(new TopCommand(ctx));
		register(new BountyCommand(ctx));
		register(new AddBountyCommand(ctx));
		register(new TransformCommand(ctx));
		register(new IntentCommand(ctx));
		register(new SetPhaseCommand(ctx));
		register(new CheckCommand(ctx));
		register(new InfectCommand(ctx));
		register(new CureCommand(ctx));
		register(new SetLevelCommand(ctx, false));
		register(new SetLevelCommand(ctx, true));
		register(new SetAlphaCommand(ctx));
		register(new ToggleCommand(ctx));
		register(GiveItemCommand.infectionPotion(ctx));
		register(GiveItemCommand.curePotion(ctx));
		register(GiveItemCommand.wolfbane(ctx));
		register(GiveItemCommand.silverSword(ctx));
		register(GiveItemCommand.loreBook(ctx));
		register(GiveItemCommand.silverArmor(ctx));
		register(GiveItemCommand.werewolfTracker(ctx));
		register(new TrackCommand(ctx));
		register(new ClanCommand(ctx));
		register(new HomeCommand(ctx));
		register(new CallCommand(ctx));
		register(new SetHomeCommand(ctx));
		// Not in the help listing
		register(new HelpCommand(ctx, this));
		register(new ReloadCommand(ctx));
		register(new SaveCommand(ctx));
		register(new ListCommand(ctx));
		register(GiveItemCommand.vampireTracker(ctx));
		register(new PluginToggleCommand(ctx, true));
		register(new PluginToggleCommand(ctx, false));
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
			this.plugin.sendInfo(player, ctx.getLanguageManager().getLanguageString(LANGUAGESTRING.InvalidCommand, ChatColor.RED));
			return true;
		}

		if (player == null && subCommand.isPlayerOnly(args))
		{
			this.plugin.log(CONSOLE_MESSAGE);
			return true;
		}

		if (!subCommand.isPermitted(player, args))
		{
			CommandUtil.sendNoPermission(ctx, player);
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
