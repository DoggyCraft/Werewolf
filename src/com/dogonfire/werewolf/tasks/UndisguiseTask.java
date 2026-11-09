package com.dogonfire.werewolf.tasks;

import java.util.UUID;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.managers.WerewolfScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.ScoreboardManager;

/**
 * Turns a werewolf back to human form a few ticks after {@code unsetWerewolfSkin} or a cure. For a cure
 * ({@code forever}) the data was already removed by {@code WerewolfManager.unmakeWerewolf}; this only does the
 * visual part and puts the player back in {@code originalGroup}.
 */
public class UndisguiseTask implements Runnable
{
	private final Werewolf	plugin;
	private final WerewolfContext ctx;
	private final UUID	playerId;
	private boolean	makeVisible;
	private boolean	forever;
	private String	originalGroup;

	public UndisguiseTask(WerewolfContext ctx, UUID playerId, boolean visible, boolean forever, String originalGroup)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.playerId = playerId;
		this.makeVisible = visible;
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
		ScoreboardManager manager;

		if (player != null)
		{
			ctx.getWolfFormManager().removeWolfForm(player, ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()));

			player.setPlayerListName(player.getDisplayName());

			// Before trying to undisguise, check if Disguises are enabled...
			if (this.makeVisible && ctx.isDisguisesEnabled())
			{
				ctx.getSkinManager().unsetWerewolfSkin(player);
			}
			if (ctx.isHealthBarEnabled())
			{
				manager = Bukkit.getScoreboardManager();
				player.setScoreboard(manager.getMainScoreboard());
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
			if ((groupName != null) && (!groupName.equals(this.plugin.getSettings().general().werewolfGroupName())))
			{
				this.plugin.logDebug("Putting " + player.getName() + " into group " + groupName);

				String playerName = plugin.getServer().getOfflinePlayer(playerId).getName();

				ctx.getPermissionsManager().setGroup(playerName, groupName);
			}
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
