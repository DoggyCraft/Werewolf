package com.dogonfire.werewolf.commands.sub;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;
import com.dogonfire.werewolf.managers.MoonManager;

/** /ww setphase <phase> [world]: moves the world's time forward to the next night with that moon phase */
public class SetPhaseCommand extends SubCommand
{
	public SetPhaseCommand(Werewolf plugin)
	{
		super(plugin, "setphase", "werewolf.setphase", false, "/ww setphase <phase> [world]", LANGUAGESTRING.HelpSetPhase);
	}

	@Override
	public boolean acceptsArgs(int length)
	{
		return true;
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		MoonManager.MoonPhase phase = args.length >= 2 ? MoonManager.MoonPhase.fromString(args[1]) : null;
		if (phase == null)
		{
			lang().setType(String.join(", ", phaseNames()));
			send(player, LANGUAGESTRING.InvalidMoonPhase, ChatColor.RED);
			return;
		}

		World world;
		if (args.length >= 3)
		{
			world = this.plugin.getServer().getWorld(args[2]);
			if (world == null)
			{
				send(player, ChatColor.RED + "No world named " + args[2]);
				return;
			}
		}
		else if (player != null)
		{
			world = player.getWorld();
		}
		else
		{
			world = this.plugin.getServer().getWorlds().get(0);
		}

		Werewolf.getMoonManager().setMoonPhase(world, phase);

		lang().setPlayerName(world.getName());
		lang().setType(Werewolf.getMoonManager().getPhaseName(Werewolf.getMoonManager().getMoonPhase(world)));
		send(player, LANGUAGESTRING.MoonPhaseSet, ChatColor.AQUA);

		this.plugin.log(sender.getName() + " set the moon phase in " + world.getName() + " to " + phase.name());
	}

	private static List<String> phaseNames()
	{
		List<String> names = new ArrayList<String>();
		for (MoonManager.MoonPhase phase : MoonManager.MoonPhase.values())
		{
			names.add(phase.name());
		}
		return names;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, Player player, String[] args)
	{
		List<String> result = new ArrayList<String>();
		if (args.length == 2)
		{
			StringUtil.copyPartialMatches(args[1], phaseNames(), result);
		}
		else if (args.length == 3)
		{
			List<String> worlds = new ArrayList<String>();
			for (World world : this.plugin.getServer().getWorlds())
			{
				worlds.add(world.getName());
			}
			StringUtil.copyPartialMatches(args[2], worlds, result);
		}
		return result;
	}
}
