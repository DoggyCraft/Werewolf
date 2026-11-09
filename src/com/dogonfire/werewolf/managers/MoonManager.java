package com.dogonfire.werewolf.managers;

import com.dogonfire.werewolf.api.MoonPhase;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import com.dogonfire.werewolf.tasks.CentralMessageTask;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;

/**
 * Moon phase and night timing for each world. The phase follows vanilla: it is the world's day number
 * (full time / 24000) modulo 8, with day 0 being a full moon.
 */
public class MoonManager
{
	public static final long	DAY_LENGTH	= 24000L;

	private final Werewolf				plugin;
	private final WerewolfContext ctx;
	private final HashMap<UUID, Long>	lastFullMoonAnnouncementTimes	= new HashMap<UUID, Long>();
	private final HashMap<UUID, Long>	lastWorldUpdates				= new HashMap<UUID, Long>();

	public MoonManager(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	public long getDay(World world)
	{
		return world.getFullTime() / DAY_LENGTH;
	}

	public MoonPhase getMoonPhase(World world)
	{
		return MoonPhase.values()[(int) (getDay(world) % 8L)];
	}

	public String getPhaseName(MoonPhase phase)
	{
		return plugin.getSettings().moon().phaseNames().get(phase);
	}

	/** 0 when tonight is a full moon */
	public int getDaysToNextFullMoon(World world)
	{
		return (8 - getMoonPhase(world).ordinal()) % 8;
	}

	public boolean isNight(World world)
	{
		long time = world.getFullTime() % DAY_LENGTH;

		return (time > plugin.getSettings().moon().nightStart()) && (time < plugin.getSettings().moon().nightEnd());
	}

	/** Whether this world's current day has a full moon, regardless of the time of day */
	public boolean isFullMoon(World world)
	{
		return getMoonPhase(world) == MoonPhase.FullMoon;
	}

	public boolean isFullMoonNight(World world)
	{
		return isNight(world) && isFullMoon(world);
	}

	/**
	 * Whether the moon forces werewolves outside to transform right now: a full moon night, or any night when
	 * Settings.OnlyTransformDuringFullMoon is off.
	 */
	public boolean isTransformationNight(World world)
	{
		if (plugin.getSettings().general().onlyTransformDuringFullMoon())
		{
			return isFullMoonNight(world);
		}
		return isNight(world);
	}

	public boolean isFullMoonDusk(World world)
	{
		long time = world.getFullTime() % DAY_LENGTH;

		return (time > 10000L) && (time < 14000L) && isFullMoon(world);
	}

	/** Unique id for the current night in a world, used to grant at most one level per full moon */
	public String getNightKey(World world)
	{
		return world.getName() + ":" + getDay(world);
	}

	/**
	 * Moves the world's time forward to the start of the next night with the given phase (tonight if today
	 * already has that phase). The moon phase is derived from the day number, so the time has to change.
	 */
	public void setMoonPhase(World world, MoonPhase phase)
	{
		long fullTime = world.getFullTime();
		long day = fullTime / DAY_LENGTH;
		long targetDay = day + ((phase.ordinal() - (day % 8L)) + 8L) % 8L;

		if (targetDay == day && isNight(world))
		{
			return;
		}

		world.setFullTime(targetDay * DAY_LENGTH + plugin.getSettings().moon().nightStart() + 1L);
	}

	public String getNextFullMoonText(World world)
	{
		switch (getDaysToNextFullMoon(world))
		{
			case 0:
				return ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.Today, ChatColor.GOLD);
			case 1:
				return ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.Tomorrow, ChatColor.GOLD);
			case 2:
				return ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In2Days, ChatColor.GOLD);
			case 3:
				return ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In3Days, ChatColor.GOLD);
			case 4:
				return ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In4Days, ChatColor.GOLD);
			case 5:
				return ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In5Days, ChatColor.GOLD);
			case 6:
				return ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In6Days, ChatColor.GOLD);
			default:
				return ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In7Days, ChatColor.GOLD);
		}
	}

	/** Called from WerewolfManager.update(): announces a rising full moon in each world at dusk */
	public void update()
	{
		for (World world : plugin.getServer().getWorlds())
		{
			Long lastUpdate = this.lastWorldUpdates.get(world.getUID());
			if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < 10000L)
			{
				continue;
			}
			this.lastWorldUpdates.put(world.getUID(), System.currentTimeMillis());

			if (!isFullMoonDusk(world))
			{
				continue;
			}

			this.plugin.logDebug("Is FullMoonDusk in " + world.getName());

			Long lastAnnouncement = this.lastFullMoonAnnouncementTimes.get(world.getUID());
			if (lastAnnouncement != null && System.currentTimeMillis() - lastAnnouncement <= 600000L)
			{
				continue;
			}

			Set<UUID> werewolfPlayers = ctx.getWerewolfManager().getOnlineWerewolves(world);
			int numberOfTurningWerewolves = werewolfPlayers.size();

			this.plugin.logDebug("Number Of Turning Werewolves is " + numberOfTurningWerewolves);

			if (numberOfTurningWerewolves > 0)
			{
				ctx.getLanguageManager().setAmount("" + numberOfTurningWerewolves);
				ctx.getLanguageManager().setPlayerName(world.getName());

				String fullMoonText = ChatColor.BOLD + ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.FullMoonIsRising, ChatColor.GOLD);
				String numberOfWerewolvesText = ChatColor.BOLD + ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.FullMoonNumberOfWerewolves, ChatColor.GOLD);

				announce(world, fullMoonText, Sound.AMBIENT_CAVE, 20);
				announce(world, numberOfWerewolvesText, Sound.AMBIENT_CAVE, 140);

				this.lastFullMoonAnnouncementTimes.put(world.getUID(), System.currentTimeMillis());
			}
		}
	}

	private void announce(World world, String messageText, Sound sound, long delay)
	{
		this.plugin.getServer().getScheduler().runTaskLater(this.plugin, new CentralMessageTask(world, messageText, sound), delay);
	}
}
