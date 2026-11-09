package com.dogonfire.werewolf.managers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.clanjhoo.vampire.VampireAPI;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.dogonfire.werewolf.events.WerewolfCureEvent;
import com.dogonfire.werewolf.events.WerewolfInfectionEvent;
import com.dogonfire.werewolf.events.WerewolfTransformEvent;
import com.dogonfire.werewolf.managers.ClanManager.ClanType;
import com.dogonfire.werewolf.storage.WerewolfRecord;
import com.dogonfire.werewolf.storage.WerewolfRepository;
import com.dogonfire.werewolf.storage.WolfState;
import com.dogonfire.werewolf.tasks.DisguiseTask;
import com.dogonfire.werewolf.tasks.UndisguiseTask;

public class WerewolfManager
{
	private final Werewolf plugin;
	private final WerewolfContext ctx;
	private final WerewolfRepository	werewolves;

	private Random					random							= new Random();
	private String					datePattern						= "HH:mm:ss dd-MM-yyyy";
	private HashMap<UUID, String>	originalGroup					= new HashMap<UUID, String>();
	// Who is in wolf form, with their list name from before; concurrent because the async chat thread reads it
	private final Map<UUID, String>	playerlistNames					= new ConcurrentHashMap<UUID, String>();
	// Start time of wolf forms entered with /ww transform; only these are cut short by the time limit
	private HashMap<UUID, Long>		controlledFormStarts			= new HashMap<UUID, Long>();
	private HashMap<UUID, Long>		lastFormChanges					= new HashMap<UUID, Long>();
	private HashMap<UUID, Long>		lastHowls						= new HashMap<UUID, Long>();
	private HashMap<UUID, Long>		lastGrowls						= new HashMap<UUID, Long>();
	// Set by onDisable: undisguise right away instead of in a task
	private boolean					shuttingDown					= false;

	public WerewolfManager(WerewolfContext ctx, WerewolfRepository werewolves)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.werewolves = werewolves;
	}

	public int getNumberOfWerewolves()
	{
		return this.werewolves.size();
	}

	public Player getNearestWerewolf(UUID playerId)
	{
		Player player = this.plugin.getServer().getPlayer(playerId);
		Player minWerewolf = null;

		float minDist = 999999.0F;
		if (player == null)
		{
			return null;
		}

		for (Player onlinePlayer : this.plugin.getServer().getOnlinePlayers())
		{
			if (hasWerewolfSkin(onlinePlayer.getUniqueId()) && ((player.getWorld() == onlinePlayer.getWorld()) && (player.getEntityId() != onlinePlayer.getEntityId())))
			{
				float dist = (float) onlinePlayer.getLocation().toVector().subtract(player.getLocation().toVector()).length();
				if (dist < minDist)
				{
					minWerewolf = onlinePlayer;
					minDist = dist;
				}
			}
		}

		return minWerewolf;
	}

	public String getPlayerListName(Player player)
	{
		return (String) this.playerlistNames.get(player.getUniqueId());
	}

	public void pushPlayerData(Player player)
	{
		String groupName = "NoGroup";

		this.playerlistNames.put(player.getUniqueId(), ChatColor.stripColor(player.getPlayerListName()));
		this.originalGroup.put(player.getUniqueId(), groupName);

		setWolfForm(player.getUniqueId(), player.getName());
	}

	public void popPlayerData(UUID playerId)
	{
		if (this.playerlistNames.containsKey(playerId))
		{
			this.playerlistNames.remove(playerId);
		}

		if (this.originalGroup.containsKey(playerId))
		{
			this.originalGroup.remove(playerId);
		}
	}

	public String getOriginalPermissionGroup(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		return record != null ? record.getOriginalGroup() : null;
	}

	public void setOriginalPermissionGroup(UUID playerId, String groupName)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record != null)
		{
			record.setOriginalGroup(groupName);
		}
	}

	public Collection<UUID> getOnlineWerewolvesInWolfForm()
	{
		Set<UUID> werewolfPlayers = new HashSet<UUID>();

		for (World world : plugin.getServer().getWorlds())
		{
			for (UUID playerId : getOnlineWerewolves(world))
			{
				if (this.isWerewolf(playerId) && this.isWolfForm(playerId))
				{
					werewolfPlayers.add(playerId);
				}
			}
		}

		return werewolfPlayers;
	}

	public Collection<UUID> getOnlineWerewolvesInWolfForm(World world)
	{
		Set<UUID> werewolfPlayers = new HashSet<UUID>();

		for (UUID playerId : getOnlineWerewolves(world))
		{
			if (this.isWerewolf(playerId) && this.isWolfForm(playerId))
			{
				werewolfPlayers.add(playerId);
			}
		}

		return werewolfPlayers;
	}

	public Set<UUID> getOnlineWerewolves(World world)
	{
		Set<UUID> werewolfPlayers = new HashSet<UUID>();

		for (Player player : world.getPlayers())
		{
			if (isWerewolf(player))
			{
				werewolfPlayers.add(player.getUniqueId());
			}
		}
		return werewolfPlayers;
	}

	/** Everyone in werewolves.yml */
	public Set<UUID> getAllWerewolves()
	{
		return new HashSet<UUID>(this.werewolves.ids());
	}

	public Set<UUID> getWerewolvesInClan(ClanManager.ClanType clanType)
	{
		Set<UUID> werewolfList = new HashSet<UUID>();
		for (WerewolfRecord record : this.werewolves.inClan(clanType))
		{
			werewolfList.add(record.getId());
		}
		return werewolfList;
	}

	public void sendMessageToClan(ClanType clan, String message)
	{
		for (UUID playerId : getWerewolvesInClan(clan))
		{
			Player player = plugin.getServer().getPlayer(playerId);
			if (player != null)
			{
				player.sendMessage(message);
			}
		}
	}

	/** The player's name as last seen by this plugin, for werewolves who may be offline */
	public String getPlayerName(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		String name = record != null ? record.getPlayerName() : null;
		if (name == null)
		{
			name = plugin.getServer().getOfflinePlayer(playerId).getName();
		}
		return name != null ? name : playerId.toString();
	}

	/** Names of all werewolves in werewolves.yml, online or not */
	public List<String> getWerewolfPlayerNames()
	{
		List<String> names = new ArrayList<String>();
		for (WerewolfRecord record : this.werewolves.all())
		{
			if (record.getPlayerName() != null)
			{
				names.add(record.getPlayerName());
			}
		}
		return names;
	}

	/** Finds a werewolf (online or not) by the name stored in werewolves.yml, or null */
	public UUID findWerewolfByName(String name)
	{
		WerewolfRecord record = this.werewolves.findByName(name);
		return record != null ? record.getId() : null;
	}

	public boolean makeWerewolf(Player player, boolean turnNow, ClanManager.ClanType clan, WerewolfInfectionEvent.Source source)
	{
		WerewolfInfectionEvent event = new WerewolfInfectionEvent(player, clan, source, turnNow);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled())
		{
			return false;
		}
		clan = event.getClan();
		if (clan == null)
		{
			clan = ClanType.values()[this.random.nextInt(ClanType.values().length)];
		}

		// The only place a werewolves.yml entry is created; every other setter only changes existing werewolves
		this.werewolves.getOrCreate(player.getUniqueId()).setPlayerName(player.getName());

		if (turnNow)
		{
			setHumanForm(player.getUniqueId(), player.getName());

			setInfectedThisNight(player.getUniqueId(), player.getName(), false);

			setWerewolfClan(player.getUniqueId(), clan);

			setWerewolfName(player.getUniqueId());

			setWerewolfSkin(player, WerewolfTransformEvent.Reason.INFECTION);

			setWolfForm(player.getUniqueId(), player.getName());

			this.plugin.log(player.getName() + " was made an full werewolf");
		}
		else
		{
			setLastTransformation(player.getUniqueId());

			setInfectedThisNight(player.getUniqueId(), player.getName(), true);

			setWerewolfClan(player.getUniqueId(), clan);

			setWerewolfName(player.getUniqueId());

			setInfectedWerewolf(player.getUniqueId(), player.getName());

			this.plugin.log(player.getUniqueId() + " was made a infected werewolf");
		}
		return true;
	}

	public boolean unmakeWerewolf(UUID playerId, WerewolfCureEvent.Cause cause)
	{
		WerewolfCureEvent event = new WerewolfCureEvent(playerId, cause);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled())
		{
			return false;
		}

		Player player = this.plugin.getServer().getPlayer(playerId);

		if (player != null)
		{
			player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 1, false, false));
		}

		// The data goes now, so a re-infection right after is kept; only the skin change waits for the UndisguiseTask
		String originalGroup = getOriginalPermissionGroup(playerId);
		setNotWerewolf(playerId);
		popPlayerData(playerId);
		this.controlledFormStarts.remove(playerId);

		this.undisguiseCuredWerewolf(playerId, originalGroup);

		ctx.getClanManager().removeAlpha(playerId);

		this.plugin.log(plugin.getServer().getOfflinePlayer(playerId).getName() + " was unmade from being a werewolf");
		return true;
	}

	public void setWerewolfSkin(Player player, WerewolfTransformEvent.Reason reason)
	{
		if (!ctx.isWerewolvesEnabled())
		{
			return;
		}

		if (!isWerewolf(player))
		{
			return;
		}

		if (hasWerewolfSkin(player.getUniqueId()))
		{
			return;
		}

		if (isInfectedThisNight(player.getUniqueId()))
		{
			return;
		}

		WerewolfTransformEvent event = new WerewolfTransformEvent(player, getWerewolfClan(player.getUniqueId()), true, reason);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled())
		{
			return;
		}

		UUID playerId = player.getUniqueId();
		this.lastFormChanges.put(playerId, System.currentTimeMillis());
		if (reason == WerewolfTransformEvent.Reason.CONTROLLED)
		{
			this.controlledFormStarts.put(playerId, System.currentTimeMillis());
		}
		else
		{
			this.controlledFormStarts.remove(playerId);
		}

		if (reason == WerewolfTransformEvent.Reason.MOON || reason == WerewolfTransformEvent.Reason.INFECTION)
		{
			grantFullMoonLevel(player);
		}

		this.disguiseWerewolf(player);
	}

	/**
	 * Re-applies the skin and wolf form stats of a werewolf already in wolf form, e.g. after becoming or losing alpha.
	 * Fires a {@link WerewolfTransformEvent} with {@link WerewolfTransformEvent.Reason#REFRESH}.
	 */
	public boolean refreshWerewolfSkin(Player player)
	{
		if (!ctx.isWerewolvesEnabled() || !player.isOnline() || !hasWerewolfSkin(player.getUniqueId()))
		{
			return false;
		}

		WerewolfTransformEvent event = new WerewolfTransformEvent(player, getWerewolfClan(player.getUniqueId()), true, WerewolfTransformEvent.Reason.REFRESH);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled())
		{
			return false;
		}

		if (ctx.isDisguisesEnabled())
		{
			ctx.getSkinManager().refreshWerewolfSkin(player, player.getPlayerListName());
		}
		ctx.getWolfFormManager().refreshWolfForm(player);
		return true;
	}

	/** Refreshes the skin next tick if the player is online, alive and in wolf form (e.g. after an alpha change is stored) */
	public void refreshWerewolfSkinLater(final UUID playerId)
	{
		if (playerId == null)
		{
			return;
		}

		this.plugin.getServer().getScheduler().runTask(this.plugin, new Runnable()
		{
			public void run()
			{
				Player player = plugin.getServer().getPlayer(playerId);
				if (player != null && !player.isDead() && hasWerewolfSkin(playerId))
				{
					refreshWerewolfSkin(player);
				}
			}
		});
	}

	public void unsetWerewolfSkin(UUID playerId, boolean makeVisible, WerewolfTransformEvent.Reason reason)
	{
		String playerName = plugin.getServer().getOfflinePlayer(playerId).getName();

		if (!isWerewolf(playerId))
		{
			this.plugin.logDebug("unsetWerewolfSkin(): " + playerName + " does not have werewolf skin");
			return;
		}

		setInfectedThisNight(playerId, playerName, false);

		if (!hasWerewolfSkin(playerId))
		{
			this.plugin.logDebug("unsetWerewolfSkin(): " + playerName + " does not have a werewolf skin");
			return;
		}

		Player onlinePlayer = this.plugin.getServer().getPlayer(playerId);
		if (onlinePlayer != null)
		{
			WerewolfTransformEvent event = new WerewolfTransformEvent(onlinePlayer, getWerewolfClan(playerId), false, reason);
			plugin.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled())
			{
				return;
			}
		}

		this.controlledFormStarts.remove(playerId);
		if (reason != WerewolfTransformEvent.Reason.FORCED)
		{
			this.lastFormChanges.put(playerId, System.currentTimeMillis());
		}

		if (isFullWerewolf(playerId))
		{
			Player player = this.plugin.getServer().getPlayer(playerId);
			if (player != null)
			{
				String message = ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.Untransform, ChatColor.LIGHT_PURPLE);
				player.sendMessage(message);
			}
		}

		this.plugin.log(playerName + " turned into human form!");

		this.undisguiseWerewolf(playerId, makeVisible);
	}

	public void transform(Player player, WerewolfTransformEvent.Reason reason)
	{
		if (!isWerewolvesAllowedInWorld(player))
		{
			return;
		}

		if (!hasWerewolfSkin(player.getUniqueId()))
		{
			playSmoke(player);
			setWerewolfSkin(player, reason);
		}
		else
		{
			this.plugin.log("Could not transform " + player.getName() + ": Not a werewolf!");
		}
	}

	public void untransform(Player player, WerewolfTransformEvent.Reason reason)
	{
		playSmoke(player);
		unsetWerewolfSkin(player.getUniqueId(), true, reason);
	}

	private void playSmoke(Player player)
	{
		World world = player.getWorld();
		world.playEffect(player.getLocation(), Effect.SMOKE, 100);
		world.playEffect(player.getLocation().add(new Vector(0, 1, 0)), Effect.SMOKE, 100);
		world.playEffect(player.getLocation().add(new Vector(0, 2, 0)), Effect.SMOKE, 100);
	}

	/** onDisable: everyone in wolf form turns human right away, since tasks scheduled now would never run */
	public void untransformAllForShutdown()
	{
		this.shuttingDown = true;
		for (Player player : this.plugin.getServer().getOnlinePlayers())
		{
			if (!isWerewolf(player) || !hasWerewolfSkin(player.getUniqueId()))
			{
				continue;
			}

			try
			{
				untransform(player, WerewolfTransformEvent.Reason.FORCED);
			}
			catch (RuntimeException e)
			{
				this.plugin.getLogger().log(Level.WARNING, "Could not turn " + player.getName() + " back to human form", e);
			}
		}
	}

	/** Starts the transformation into wolf form (skin, stats, messages) next tick */
	public void disguiseWerewolf(Player player)
	{
		this.plugin.getServer().getScheduler().runTaskLater(this.plugin, new DisguiseTask(this.ctx, player), 1L);
	}

	/** Back to human form (the werewolf's data is kept) */
	public void undisguiseWerewolf(UUID playerId, boolean makeVisible)
	{
		runUndisguise(new UndisguiseTask(this.ctx, playerId, makeVisible, false, null));
	}

	/**
	 * The visual part of a cure: the caller has already removed the player's data. {@code originalGroup} is the
	 * permission group to put them back in, read before the data was removed.
	 */
	public void undisguiseCuredWerewolf(UUID playerId, String originalGroup)
	{
		runUndisguise(new UndisguiseTask(this.ctx, playerId, true, true, originalGroup));
	}

	private void runUndisguise(UndisguiseTask task)
	{
		if (this.shuttingDown)
		{
			task.run();
		}
		else
		{
			this.plugin.getServer().getScheduler().runTaskLater(this.plugin, task, 8L);
		}
	}

	public boolean isWerewolvesAllowedInWorld(Player player)
	{
		return this.plugin.getSettings().allowedWorlds().contains(player.getWorld().getName());
	}

	public boolean isUnderOpenSky(Player player)
	{
		return player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getLocation().getBlockY();
	}

	/** VampireRevamp's VampireAPI is only called when that plugin is enabled */
	public boolean isVampire(Player player)
	{
		if (!this.ctx.isVampireEnabled())
		{
			return false;
		}

		try
		{
			return VampireAPI.isVampire(player);
		}
		catch (AssertionError e)
		{
			// VampireRevamp hasn't loaded this player's data yet
			return false;
		}
	}

	public boolean isVampire(UUID playerId)
	{
		if (!this.ctx.isVampireEnabled())
		{
			return false;
		}

		// VampireAPI only works on online players
		Player player = this.plugin.getServer().getPlayer(playerId);
		return player != null && isVampire(player);
	}

	public boolean isHuman(UUID playerId)
	{
		return !isWerewolf(playerId) && !isVampire(playerId);
	}

	public boolean isHuman(Player player)
	{
		return isHuman(player.getUniqueId());
	}

	public boolean canTransform(Player player)
	{
		return !hasWerewolfSkin(player.getUniqueId()) && !isInfectedThisNight(player.getUniqueId()) && ctx.getMoonManager().isTransformationNight(player.getWorld()) && this.isWerewolvesAllowedInWorld(player) && this.isUnderOpenSky(player);
	}

	/** Morning reverts every wolf form except ones entered with /ww transform, which the time limit handles */
	public boolean canUntransform(Player player)
	{
		return (hasWerewolfSkin(player.getUniqueId()) || isInfectedThisNight(player.getUniqueId())) && !ctx.getMoonManager().isNight(player.getWorld()) && !isControlledForm(player.getUniqueId());
	}

	public boolean isControlledForm(UUID playerId)
	{
		return this.controlledFormStarts.containsKey(playerId);
	}

	/** Seconds a /ww transform wolf form may last at this werewolf's level, or -1 if there is no limit */
	public long getTransformationTimeLimitSeconds(UUID playerId)
	{
		if (!this.plugin.getSettings().transformation().limit())
		{
			return -1;
		}

		int level = getNumberOfTransformations(playerId);
		double base = this.plugin.getSettings().transformation().base();
		double modifier = this.plugin.getSettings().transformation().modifier();

		double seconds = switch (this.plugin.getSettings().transformation().equation())
		{
			case LINEAR -> base + modifier * level;
			case EXPONENTIAL -> base + Math.pow(level, modifier);
			case CONSTANT -> base;
		};

		return Math.max(0L, (long) seconds);
	}

	/** Reverts a /ww transform wolf form whose time is up. The limit is paused while the moon forces the transformation. */
	private boolean checkTransformationTimeLimit(Player player)
	{
		Long start = this.controlledFormStarts.get(player.getUniqueId());
		if (start == null || !hasWerewolfSkin(player.getUniqueId()))
		{
			return false;
		}

		long limit = getTransformationTimeLimitSeconds(player.getUniqueId());
		if (limit < 0 || System.currentTimeMillis() - start < limit * 1000L)
		{
			return false;
		}

		if (ctx.getMoonManager().isTransformationNight(player.getWorld()))
		{
			return false;
		}

		this.untransform(player, WerewolfTransformEvent.Reason.TIME_LIMIT);
		if (isControlledForm(player.getUniqueId()))
		{
			// A listener cancelled the untransformation
			return false;
		}

		player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TransformationTimeExpired, ChatColor.LIGHT_PURPLE));
		return true;
	}

	/** Gives a level for this transformation night, at most once per night even across relogs and restarts */
	public void grantFullMoonLevel(Player player)
	{
		World world = player.getWorld();
		if (!ctx.getMoonManager().isTransformationNight(world))
		{
			return;
		}

		WerewolfRecord record = this.werewolves.get(player.getUniqueId());
		String nightKey = ctx.getMoonManager().getNightKey(world);
		if (record == null || nightKey.equals(record.getLastLevelNight()))
		{
			return;
		}

		record.setLastLevelNight(nightKey);
		incrementNumberOfFullMoonTransformations(player.getUniqueId());

		ctx.getLanguageManager().setAmount("" + getNumberOfTransformations(player.getUniqueId()));
		player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.FullMoonLevelGained, ChatColor.LIGHT_PURPLE));
	}

	/** Seconds left before the werewolf can /ww transform into wolf form again, 0 if ready */
	public long getTransformCooldownLeft(UUID playerId)
	{
		return getCooldownLeft(this.lastFormChanges, playerId, this.plugin.getSettings().cooldowns().transform());
	}

	public long getHowlCooldownLeft(UUID playerId)
	{
		return getCooldownLeft(this.lastHowls, playerId, this.plugin.getSettings().cooldowns().howl());
	}

	public long getGrowlCooldownLeft(UUID playerId)
	{
		return getCooldownLeft(this.lastGrowls, playerId, this.plugin.getSettings().cooldowns().growl());
	}

	private long getCooldownLeft(HashMap<UUID, Long> lastUses, UUID playerId, int cooldownSeconds)
	{
		Long lastUse = lastUses.get(playerId);
		if (lastUse == null || cooldownSeconds <= 0)
		{
			return 0;
		}

		long msLeft = cooldownSeconds * 1000L - (System.currentTimeMillis() - lastUse);
		return msLeft <= 0 ? 0 : (msLeft + 999L) / 1000L;
	}

	/** Starts the /ww transform cooldown over from now, as if the werewolf had just changed form (wolfbane) */
	public void restartTransformCooldown(UUID playerId)
	{
		this.lastFormChanges.put(playerId, System.currentTimeMillis());
	}

	public void setLastHowl(UUID playerId)
	{
		this.lastHowls.put(playerId, System.currentTimeMillis());
	}

	public void setLastGrowl(UUID playerId)
	{
		this.lastGrowls.put(playerId, System.currentTimeMillis());
	}

	public boolean isGoldImmune(UUID playerId)
	{
		return getNumberOfTransformations(playerId) >= this.plugin.getSettings().maturity().goldImmunity();
	}

	public boolean canUseIntent(UUID playerId)
	{
		return getNumberOfTransformations(playerId) >= this.plugin.getSettings().maturity().intent();
	}

	public boolean hasIntent(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		return record != null && record.hasIntent();
	}

	public void setIntent(UUID playerId, boolean intent)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record != null)
		{
			record.setIntent(intent);
		}
	}

	/** Chance that a bite from this werewolf infects; intent raises it once the werewolf is mature enough */
	public double getBiteInfectionRisk(UUID werewolfId)
	{
		if (this.plugin.getSettings().infection().werewolfBiteRisk() <= 0)
		{
			return 0;
		}

		if (hasIntent(werewolfId) && canUseIntent(werewolfId))
		{
			return this.plugin.getSettings().infection().werewolfBiteIntentRisk();
		}

		return this.plugin.getSettings().infection().werewolfBiteRisk();
	}

	public boolean isWerewolf(Player player)
	{
		return isWerewolf(player.getUniqueId());
	}

	public boolean isWerewolf(UUID playerId)
	{
		if (hasWerewolfSkin(playerId))
		{
			return true;
		}

		return getWolfState(playerId) != null;
	}

	/** The stored WolfState, or null for players who are not werewolves */
	private WolfState getWolfState(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		return record != null ? record.getWolfState() : null;
	}

	public boolean isFullWerewolf(UUID playerId)
	{
		WolfState wolfState = getWolfState(playerId);
		return (wolfState == WolfState.WolfForm) || (wolfState == WolfState.HumanForm);
	}

	public boolean isInfectedWerewolf(UUID playerId)
	{
		return getWolfState(playerId) == WolfState.Infected;
	}

	public boolean isWolfForm(UUID playerId)
	{
		return getWolfState(playerId) == WolfState.WolfForm;
	}


	/** The werewolf's name, given (and stored) the first time it is asked for; "" for players who are not werewolves */
	public String getWerewolfName(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record == null)
		{
			return "";
		}

		if (record.getWerewolfName() == null || record.getWerewolfName().isEmpty())
		{
			record.setWerewolfName(newWerewolfName());
		}

		return record.getWerewolfName();
	}

	/**
	 * The werewolf's clan. Every werewolf has one (given in makeWerewolf, or at load for old data without one); players
	 * who are not werewolves get WerewolfBite. Never changes any data.
	 */
	public ClanManager.ClanType getWerewolfClan(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record == null || record.getClan() == null)
		{
			return ClanManager.ClanType.WerewolfBite;
		}

		return record.getClan();
	}

	public UUID getAlphaCandidate(ClanManager.ClanType clan)
	{
		return getAlphaCandidate(clan, null);
	}

	/** The clan member best suited to be alpha: full werewolves before infected ones, then the highest level. Skips {@code exclude}. */
	public UUID getAlphaCandidate(ClanManager.ClanType clan, UUID exclude)
	{
		UUID best = null;
		boolean bestFull = false;
		int bestLevel = -1;

		for (UUID playerId : getWerewolfClanMembersRanked(clan))
		{
			if (playerId.equals(exclude))
			{
				continue;
			}

			boolean full = isFullWerewolf(playerId);
			int level = getNumberOfTransformations(playerId);
			if (best == null || (full && !bestFull) || (full == bestFull && level > bestLevel))
			{
				best = playerId;
				bestFull = full;
				bestLevel = level;
			}
		}

		return best;
	}

	public class TransformationsComparator implements Comparator<UUID>
	{
		public int compare(UUID member1, UUID member2)
		{
			return getNumberOfTransformations(member2) - getNumberOfTransformations(member1);
		}
	}

	/** UUIDs (as strings) of everyone in werewolves.yml in this clan */
	public List<String> getWerewolfClanMembers(ClanManager.ClanType clan)
	{
		List<String> clanMembers = new ArrayList<String>();
		for (WerewolfRecord record : this.werewolves.inClan(clan))
		{
			clanMembers.add(record.getId().toString());
		}
		return clanMembers;
	}

	public List<UUID> getWerewolfClanMembersRanked(ClanManager.ClanType clan)
	{
		List<UUID> clanMembers = new ArrayList<UUID>();
		for (WerewolfRecord record : this.werewolves.inClan(clan))
		{
			clanMembers.add(record.getId());
		}

		Collections.sort(clanMembers, new TransformationsComparator());

		return clanMembers;
	}

	public boolean isInfectedThisNight(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		return record != null && record.isInfectedThisNight();
	}

	public void setInfectedThisNight(UUID playerId, String playerName, boolean thisNight)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record == null)
		{
			return;
		}

		record.setPlayerName(playerName);
		record.setInfectedThisNight(thisNight);
	}

	public void setWerewolfClan(UUID playerId, ClanManager.ClanType clan)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record == null)
		{
			return;
		}

		ClanManager.ClanType oldClan = record.getClan();
		record.setClan(clan);

		// Leaving a clan as its alpha hands the old clan to its next best member; an empty clan gets this player
		if (oldClan != null && oldClan != clan)
		{
			ctx.getClanManager().checkAlpha(oldClan);
		}
		ctx.getClanManager().checkAlpha(clan);
	}

	public void incrementNumberOfFullMoonTransformations(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record != null)
		{
			record.setTransformations(record.getTransformations() + 1);
		}
	}

	public void setLastTransformation(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record != null)
		{
			record.setLastTransform(new SimpleDateFormat(this.datePattern).format(new Date()));
		}
	}

	public int getNumberOfTransformations(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		return record != null ? record.getTransformations() : 0;
	}

	/** Sets a werewolf's level (admin commands); works for offline werewolves too */
	public void setNumberOfTransformations(UUID playerId, int transformations)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record != null)
		{
			record.setTransformations(Math.max(0, transformations));
		}
	}


	public boolean hasToDropItems(UUID playerId)
	{
		return getNumberOfTransformations(playerId) < this.plugin.getSettings().maturity().noDropItems();
	}

	public boolean hasRecentTransformAutoCure(UUID playerId)
	{
		DateFormat formatter = new SimpleDateFormat(this.datePattern);
		Date thisDate = new Date();
		Date transformDate = null;

		WerewolfRecord record = this.werewolves.get(playerId);
		String transformDateString = record != null ? record.getLastTransform() : null;
		try
		{
			transformDate = formatter.parse(transformDateString);
		}
		catch (Exception ex)
		{
			this.plugin.log(playerId.toString() + " has invalid LastTransform date. Resetting.");
			transformDate = new Date();
			transformDate.setTime(0L);
		}

		long diff = thisDate.getTime() - transformDate.getTime();
		long diffDays = diff / 86400000L;

		return diffDays < this.plugin.getSettings().infection().autoCureDays();
	}

	public boolean hasWerewolfSkin(UUID playerId)
	{
		return this.playerlistNames.containsKey(playerId);
	}

	private String newWerewolfName()
	{
		String givenName = "";
		String surname = "";

		// Generate a random givenName
		List<String> strings = plugin.getSettings().names().givenNames();
		if (strings.size() == 0)
		{
			this.plugin.log("No given name strings found in the config!");
		}
		else
		{
			givenName = (String) strings.toArray()[this.random.nextInt(strings.size())];
		}

		// Generate a random surname
		List<String> strings2 = plugin.getSettings().names().surnames();
		if (strings2.size() == 0)
		{
			this.plugin.log("No surname strings found in the config!");
		}
		else
		{
			surname = (String) strings2.toArray()[this.random.nextInt(strings2.size())];
		}

		String fullName = givenName + surname;

		if (fullName.isEmpty())
		{
			fullName = "Werewolf";
		}

		return fullName;
	}

	public void setWerewolfName(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record != null)
		{
			record.setWerewolfName(newWerewolfName());
		}
	}

	public void setHumanForm(UUID playerId, String playerName)
	{
		setWolfState(playerId, playerName, WolfState.HumanForm);
	}

	public void setWolfForm(UUID playerId, String playerName)
	{
		setWolfState(playerId, playerName, WolfState.WolfForm);
	}

	public void setInfectedWerewolf(UUID playerId, String playerName)
	{
		WerewolfRecord record = setWolfState(playerId, playerName, WolfState.Infected);
		if (record != null)
		{
			record.setInfectedDate(new SimpleDateFormat("HH:mm dd-MM-yyyy").format(new Date()));
		}
	}

	// Only for players in werewolves.yml, so a late task can't turn someone who was just cured back into a werewolf
	private WerewolfRecord setWolfState(UUID playerId, String playerName, WolfState wolfState)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record != null)
		{
			record.setPlayerName(playerName);
			record.setWolfState(wolfState);
		}
		return record;
	}

	/** Removes the player from werewolves.yml */
	public void setNotWerewolf(UUID playerId)
	{
		this.werewolves.remove(playerId);
	}

	public void howl(Player player)
	{
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_BIG_AMBIENT, 10.0F, 1.0F);
	}

	public void growl(Player player)
	{
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 10.0F, 1.0F);
	}

	public void sendWerewolfUrges(Player player)
	{
		String message = "";

		if (this.random.nextInt(20 + 10 * this.getNumberOfTransformations(player.getUniqueId())) > 0)
		{
			return;
		}

		if (!isWerewolf(player))
		{
			return;
		}

		if (this.hasWerewolfSkin(player.getUniqueId()))
		{
			message = ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UrgesInfectedThisNight, ChatColor.LIGHT_PURPLE);
		}
		else if (isInfectedWerewolf(player.getUniqueId()))
		{
			if (isInfectedThisNight(player.getUniqueId()))
			{
				message = ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UrgesInfectedThisNight, ChatColor.LIGHT_PURPLE);
			}
			else
			{
				message = ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UrgesInfectedHumanForm, ChatColor.LIGHT_PURPLE);
			}
		}
		else if (ctx.getMoonManager().isNight(player.getWorld()) && player.getWorld().getHighestBlockYAt(player.getLocation()) > player.getLocation().getBlockY())
		{
			message = ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UrgesInside, ChatColor.LIGHT_PURPLE);
		}

		if (!message.isEmpty())
		{
			player.sendMessage(message);
		}
	}

	public void update()
	{
		if (!ctx.isWerewolvesEnabled())
		{
			return;
		}

		ctx.getMoonManager().update();

		ctx.getClanManager().checkAlphas();

		if (this.plugin.getSettings().infection().autoCureDays() > 0 && this.random.nextInt(100) == 0 && this.getAllWerewolves().size() > 0)
		{
			int r = this.random.nextInt(this.getAllWerewolves().size());
			UUID werewolfPlayerId = (UUID) this.getAllWerewolves().toArray()[r];

			if (!hasRecentTransformAutoCure(werewolfPlayerId))
			{
				String werewolfPlayerName = plugin.getServer().getOfflinePlayer(werewolfPlayerId).getName();
				if (unmakeWerewolf(werewolfPlayerId, WerewolfCureEvent.Cause.AUTO_CURE))
				{
					this.plugin.log(werewolfPlayerName + " has not transformed for " + this.plugin.getSettings().infection().autoCureDays() + " days. Removing their werewolf infection.");
				}
			}
		}

		if (this.plugin.getServer().getOnlinePlayers().size() == 0)
		{
			return;
		}

		Set<Player> changedForm = new HashSet<Player>();

		for (Player player : new ArrayList<Player>(this.plugin.getServer().getOnlinePlayers()))
		{
			if (!isWerewolf(player))
			{
				continue;
			}

			// VampireIntegration stops werewolves becoming vampires (and the reverse), so this only catches hybrids made
			// before that, or while one of the plugins was missing: the werewolf side goes
			if (this.isVampire(player))
			{
				this.plugin.log(player.getName() + " is a Vampire! Removing their Werewolf infection...");
				unmakeWerewolf(player.getUniqueId(), WerewolfCureEvent.Cause.VAMPIRISM);
				changedForm.add(player);
			}
			else if (canTransform(player))
			{
				this.transform(player, WerewolfTransformEvent.Reason.MOON);
				changedForm.add(player);
			}
			else if (canUntransform(player))
			{
				this.untransform(player, WerewolfTransformEvent.Reason.MOON);
				changedForm.add(player);
			}
			else if (checkTransformationTimeLimit(player))
			{
				changedForm.add(player);
			}
			else if (hasWerewolfSkin(player.getUniqueId()) && this.isWerewolvesAllowedInWorld(player) && this.isUnderOpenSky(player))
			{
				// Already in wolf form when the moon rose (e.g. after /ww transform): still counts for this night's level
				grantFullMoonLevel(player);
			}

			if (!changedForm.contains(player) && hasWerewolfSkin(player.getUniqueId()))
			{
				ctx.getWolfFormManager().refreshWolfForm(player);
			}
		}

		// Urges are rare flavour messages, so only one random player gets a chance per update
		Player player = (Player) this.plugin.getServer().getOnlinePlayers().toArray()[this.random.nextInt(this.plugin.getServer().getOnlinePlayers().size())];

		if (this.plugin.getSettings().general().werewolfUrges() && !changedForm.contains(player) && isWerewolf(player))
		{
			sendWerewolfUrges(player);
		}

		HuntManager huntManager = this.ctx.getHuntManager();
		if (this.plugin.getSettings().general().autoBounty() && huntManager != null)
		{
			huntManager.autoAddBounty();
		}
	}
}