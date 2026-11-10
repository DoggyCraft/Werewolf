package com.dogonfire.werewolf.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;

/** /ww hunt: points the compass of every hunter at the nearest werewolf. Owned by HuntManager; runs while anyone hunts */
public class CompassTrackerTask implements Runnable
{
	private static final long		UPDATE_RATE	= 100L;

	private final WerewolfContext	ctx;
	private final Werewolf			plugin;
	private final List<UUID>		watchList	= new ArrayList<UUID>();
	private BukkitTask				task;

	public CompassTrackerTask(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	private void start()
	{
		if (this.task == null)
		{
			this.task = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, this, 40L, UPDATE_RATE);
		}
	}

	public void stop()
	{
		if (this.task != null)
		{
			this.task.cancel();
			this.task = null;
		}
	}

	public boolean hasWatcher(UUID watcherId)
	{
		return this.watchList.contains(watcherId);
	}

	public void addWatcher(UUID watcherId)
	{
		this.watchList.add(watcherId);
		start();
	}

	public void removeWatcher(UUID watcherId)
	{
		this.watchList.remove(watcherId);
		if (this.watchList.isEmpty())
		{
			stop();
		}
	}

	@Override
	public void run()
	{
		for (UUID playerId : this.watchList)
		{
			Player watcher = this.plugin.getServer().getPlayer(playerId);
			if (watcher != null)
			{
				Player watched = this.ctx.getWerewolfManager().getNearestWerewolf(watcher.getUniqueId());
				watcher.setCompassTarget(watched != null ? watched.getLocation() : watcher.getLocation());
			}
		}
	}
}
