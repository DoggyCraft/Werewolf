package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.api.event.WerewolfTransformEvent;

/** /ww on and /ww off: switches werewolf transformations on or off for the whole server. Ops and the console only */
public class PluginToggleCommand extends SubCommand
{
	private final boolean enable;

	public PluginToggleCommand(WerewolfContext ctx, boolean enable)
	{
		super(ctx, enable ? "on" : "off", null, false, null, null);
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
		this.ctx.setWerewolvesEnabled(this.enable);
		this.plugin.log(this.enable ? " has been enabled" : " has been disabled");

		if (player != null)
		{
			player.sendMessage(ChatColor.AQUA + (this.enable ? "Werewolves are now enabled" : "Werewolves are now disabled"));
		}

		for (World world : this.plugin.getServer().getWorlds())
		{
			if (this.enable)
			{
				if (ctx.getMoonManager().isNight(world))
				{
					for (Player werewolf : world.getPlayers())
					{
						ctx.getWerewolfManager().setWerewolfSkin(werewolf, WerewolfTransformEvent.Reason.COMMAND);
					}
				}
			}
			else
			{
				for (Player werewolf : world.getPlayers())
				{
					ctx.getWerewolfManager().unsetWerewolfSkin(werewolf.getUniqueId(), WerewolfTransformEvent.Reason.COMMAND);
				}
			}
		}
	}
}
