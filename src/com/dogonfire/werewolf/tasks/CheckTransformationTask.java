package com.dogonfire.werewolf.tasks;

import java.util.UUID;

import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.api.event.WerewolfTransformEvent;


public class CheckTransformationTask implements Runnable
{
	private UUID		playerId;
	private final Werewolf 	plugin;
	private final WerewolfContext ctx;

	public CheckTransformationTask(WerewolfContext ctx, UUID playerId)
	{
		this.playerId = playerId;
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	public void run()
	{
		Player player = plugin.getServer().getPlayer(playerId);
		
		if(player==null)
		{
			return;
		}		
		
		if (ctx.getWerewolfManager().canTransform(player))
		{
			ctx.getWerewolfManager().transform(player, WerewolfTransformEvent.Reason.MOON);
		}
		else if (ctx.getWerewolfManager().canUntransform(player))
		{
			ctx.getWerewolfManager().untransform(player, WerewolfTransformEvent.Reason.MOON);
		}
	}
}