package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.events.WerewolfTransformEvent;

/** /ww on and /ww off: switches werewolf transformations on or off for the whole server. Ops and the console only */
public class PluginToggleCommand extends SubCommand
{
	private final boolean enable;

	public PluginToggleCommand(Werewolf plugin, boolean enable)
	{
		super(plugin, enable ? "on" : "off", null, false, null, null);
		this.enable = enable;
	}

	@Override
	public boolean isPermitted(Player player, String[] args)
	{
		return player == null || player.isOp();
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		Werewolf.pluginEnabled = this.enable;
		this.plugin.log(this.enable ? " has been enabled" : " has been disabled");

		if (player != null)
		{
			player.sendMessage(ChatColor.AQUA + (this.enable ? "Werewolves are now enabled" : "Werewolves are now disabled"));
		}

		for (World world : this.plugin.getServer().getWorlds())
		{
			if (this.enable)
			{
				if (Werewolf.getMoonManager().isNight(world))
				{
					for (Player werewolf : world.getPlayers())
					{
						Werewolf.getWerewolfManager().setWerewolfSkin(werewolf, WerewolfTransformEvent.Reason.COMMAND);
					}
				}
			}
			else
			{
				for (Player werewolf : world.getPlayers())
				{
					Werewolf.getWerewolfManager().unsetWerewolfSkin(werewolf.getUniqueId(), true, WerewolfTransformEvent.Reason.COMMAND);
				}
			}
		}
	}
}
