package com.dogonfire.werewolf.managers;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import com.dogonfire.werewolf.WerewolfContext;

/** The hunting sidebar shown to werewolves in wolf form. Only exists while Scoreboards.Enabled is on */
public class WerewolfScoreboardManager
{
	private final WerewolfContext			ctx;
	private final ScoreboardManager			manager;
	private final HashMap<UUID, Objective>	playerHuntingObjectives	= new HashMap<UUID, Objective>();

	public WerewolfScoreboardManager(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.manager = Bukkit.getScoreboardManager();
	}

	/** Scoreboards were switched on by /ww reload: werewolves already in wolf form get theirs now */
	public void start()
	{
		for (Player player : Bukkit.getOnlinePlayers())
		{
			if (this.ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
			{
				newPlayerHuntingScoreboard(player);
			}
		}
	}

	/** Takes the hunting scoreboard away from everyone who has one */
	public void stop()
	{
		for (UUID playerId : this.playerHuntingObjectives.keySet())
		{
			Player player = Bukkit.getPlayer(playerId);
			if (player != null)
			{
				player.setScoreboard(this.manager.getMainScoreboard());
			}
		}
		this.playerHuntingObjectives.clear();
	}

	public void newPlayerHuntingScoreboard(Player player)
	{
		Scoreboard board = this.manager.getNewScoreboard();

		Objective objective = board.registerNewObjective("WerewolfHunting", "dummy", ChatColor.DARK_AQUA + "Hunting Status");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		this.playerHuntingObjectives.put(player.getUniqueId(), objective);

		player.setScoreboard(board);

		setMobKillsForPlayer(player, 0);
		setHumanKillsForPlayer(player, 0);

		if (this.ctx.isVampireEnabled())
		{
			setVampireKillsForPlayer(player, 0);
		}
	}

	public void removePlayerHuntingScoreboard(Player player)
	{
		this.playerHuntingObjectives.remove(player.getUniqueId());
		player.setScoreboard(this.manager.getNewScoreboard());
	}

	public void setMobKillsForPlayer(Player player, int kills)
	{
		setScore(player, ChatColor.RED + "Mobs killed:", kills);
	}

	public void setHumanKillsForPlayer(Player player, int kills)
	{
		setScore(player, ChatColor.RED + "Humans slain:", kills);
	}

	public void setVampireKillsForPlayer(Player player, int kills)
	{
		setScore(player, ChatColor.RED + "Vampires slain:", kills);
	}

	// Players who transformed while scoreboards were off have no objective
	private void setScore(Player player, String entry, int kills)
	{
		Objective objective = this.playerHuntingObjectives.get(player.getUniqueId());
		if (objective != null)
		{
			objective.getScore(entry).setScore(kills);
		}
	}
}
