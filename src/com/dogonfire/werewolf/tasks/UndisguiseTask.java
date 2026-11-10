package com.dogonfire.werewolf.tasks;

import java.util.UUID;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.managers.WerewolfScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Turns a werewolf back to human form a few ticks after {@code unsetWerewolfSkin} or a cure (or right away when the
 * player quits or the plugin is disabled). For a cure ({@code forever}) the data was already removed by
 * {@code WerewolfManager.unmakeWerewolf}; this only does the visual part and puts the player back in
 * {@code originalGroup}.
 */
public class UndisguiseTask implements Runnable
{
	private final Werewolf	plugin;
	private final WerewolfContext ctx;
	private final UUID	playerId;
	private boolean	forever;
	private String	originalGroup;

	public UndisguiseTask(WerewolfContext ctx, UUID playerId, boolean forever, String originalGroup)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.playerId = playerId;
		this.forever = forever;
		this.originalGroup = originalGroup;
	}

	public void run()
	{
		// Infected again and already back in wolf form since the cure: leave the new wolf form alone
		if (this.forever && ctx.getWerewolfManager().hasWerewolfSkin(this.playerId))
		{
			return;
		}

		Player player = this.plugin.getServer().getPlayer(this.playerId);

		if (player != null)
		{
			ctx.getWolfFormManager().removeWolfForm(player, ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()));

			player.setPlayerListName(player.getDisplayName());

			// Always, also for a dead player: a skin left on would survive the respawn
			ctx.getSkinManager().unsetWerewolfSkin(player);

			if (ctx.isHealthBarEnabled())
			{
				player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
			}

			WerewolfScoreboardManager scoreboards = ctx.getWerewolfScoreboardManager();
			if (scoreboards != null)
			{
				scoreboards.removePlayerHuntingScoreboard(player);
			}
		}

		ctx.getWerewolfManager().popPlayerData(this.playerId);

		if (this.plugin.getSettings().general().werewolfGroupEnabled() && player != null)
		{
			String groupName = this.forever ? this.originalGroup : ctx.getWerewolfManager().getOriginalPermissionGroup(playerId);
			ctx.getPermissionsManager().leaveWerewolfGroup(player, this.plugin.getSettings().general().werewolfGroupName(), groupName);
		}

		if (!this.forever && player != null)
		{
			// Does nothing if the player was cured meanwhile
			ctx.getWerewolfManager().setHumanForm(this.playerId, player.getName());
		}

		if(player!=null)
		{
			ctx.getStatisticsManager().clearStatistics(player.getUniqueId());
		}
	}
}
