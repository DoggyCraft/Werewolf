package com.dogonfire.werewolf.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;

/** Runs a {@link SubCommand} as its own command (/growl, /howl), with the same console and permission checks as /ww */
public class SingleCommandExecutor implements TabExecutor
{
	private final Werewolf plugin;
	private final WerewolfContext ctx;
	private final SubCommand command;

	public SingleCommandExecutor(WerewolfContext ctx, SubCommand command)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.command = command;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player = sender instanceof Player p ? p : null;

		String[] fullArgs = new String[args.length + 1];
		fullArgs[0] = this.command.name();
		System.arraycopy(args, 0, fullArgs, 1, args.length);

		if (player == null && this.command.isPlayerOnly(fullArgs))
		{
			this.plugin.log(WerewolfCommand.CONSOLE_MESSAGE);
			return true;
		}

		if (!this.command.isPermitted(player, fullArgs))
		{
			CommandUtil.sendNoPermission(ctx, player);
			return true;
		}

		this.command.execute(sender, player, fullArgs);
		return true;
	}

	// These commands take no arguments, so suggest nothing rather than player names
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{
		return new ArrayList<String>();
	}
}
