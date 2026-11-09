package com.dogonfire.werewolf.tasks;

import java.util.UUID;

import com.dogonfire.werewolf.Werewolf;
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
	public Werewolf	plugin;
	public UUID	playerId;
	private boolean	makeVisible;
	private boolean	forever;
	private String	originalGroup;

	public UndisguiseTask(Werewolf instance, UUID playerId, boolean visible, boolean forever, String originalGroup)
	{
		this.plugin = instance;
		this.playerId = playerId;
		this.makeVisible = visible;
		this.forever = forever;
		this.originalGroup = originalGroup;
	}

	public void run()
	{
		// Infected again and already back in wolf form since the cure: leave the new wolf form alone
		if (this.forever && Werewolf.getWerewolfManager().hasWerewolfSkin(this.playerId))
		{
			return;
		}

		Player player = this.plugin.getServer().getPlayer(this.playerId);
		ScoreboardManager manager;

		if (player != null)
		{
			Werewolf.getWolfFormManager().removeWolfForm(player, Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()));

			player.setPlayerListName(player.getDisplayName());

			// Before trying to undisguise, check if Disguises are enabled...
			if (this.makeVisible && plugin.disguisesEnabled)
			{
				Werewolf.getSkinManager().unsetWerewolfSkin(player);
			}
			if (this.plugin.healthBarEnabled)
			{
				manager = Bukkit.getScoreboardManager();
				player.setScoreboard(manager.getMainScoreboard());
			}

			if(plugin.getSettings().general().scoreboardsEnabled())
			{
				Werewolf.getWerewolfScoreboardManager().removePlayerHuntingScoreboard(player);
			}
		}

		Werewolf.getWerewolfManager().popPlayerData(this.playerId);

		if (this.plugin.getSettings().general().werewolfGroupEnabled() && player != null)
		{
			String groupName = this.forever ? this.originalGroup : Werewolf.getWerewolfManager().getOriginalPermissionGroup(playerId);
			if ((groupName != null) && (!groupName.equals(this.plugin.getSettings().general().werewolfGroupName())))
			{
				this.plugin.logDebug("Putting " + player.getName() + " into group " + groupName);

				String playerName = plugin.getServer().getOfflinePlayer(playerId).getName();

				Werewolf.getPermissionsManager().setGroup(playerName, groupName);
			}
		}

		if (!this.forever && player != null)
		{
			// Does nothing if the player was cured meanwhile
			Werewolf.getWerewolfManager().setHumanForm(this.playerId, player.getName());
		}

		Werewolf.getWerewolfManager().clearNumberOfPackWolvesForPlayer(this.playerId);

		if(player!=null)
		{
			Werewolf.getStatisticsManager().clearStatistics(player.getUniqueId());
		}
	}
}
