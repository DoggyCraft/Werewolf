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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.dogonfire.werewolf.api.event.WerewolfCureEvent;
import com.dogonfire.werewolf.api.event.WerewolfInfectionEvent;
import com.dogonfire.werewolf.api.event.WerewolfLevelChangeEvent;
import com.dogonfire.werewolf.api.event.WerewolfTransformEvent;
import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.api.Form;
import com.dogonfire.werewolf.api.WerewolfProfile;
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
	// Wolf forms entered through the API: kept until untransformed (not reverted at morning, no time limit)
	private final Set<UUID>			heldForms						= new HashSet<UUID>();
	// Snapshot of every werewolf for other threads (PlaceholderAPI, the API); rebuilt on the main thread on every change
	private final Map<UUID, WerewolfProfile>	profiles				= new ConcurrentHashMap<UUID, WerewolfProfile>();
	private HashMap<UUID, Long>		lastFormChanges					= new HashMap<UUID, Long>();
	private HashMap<UUID, Long>		lastHowls						= new HashMap<UUID, Long>();
	private HashMap<UUID, Long>		lastGrowls						= new HashMap<UUID, Long>();
	// Set by onDisable: undisguise right away instead of in a task
	private boolean					shuttingDown					= false;
	// Set while a player quits or joins: undisguise in this tick
	private boolean					untransformImmediately			= false;

	public WerewolfManager(WerewolfContext ctx, WerewolfRepository werewolves)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.werewolves = werewolves;
		this.werewolves.setChangeListener(this::refreshProfile);
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

		refreshProfile(playerId);
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

	public Set<UUID> getWerewolvesInClan(Clan clanType)
	{
		Set<UUID> werewolfList = new HashSet<UUID>();
		for (WerewolfRecord record : this.werewolves.inClan(clanType))
		{
			werewolfList.add(record.getId());
		}
		return werewolfList;
	}

	public void sendMessageToClan(Clan clan, String message)
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

	public boolean makeWerewolf(Player player, boolean turnNow, Clan clan, WerewolfInfectionEvent.Source source)
	{
		return makeWerewolf(player, turnNow, clan, source, null, 1.0);
	}

	/**
	 * The only way a player becomes a werewolf. Fires {@link WerewolfInfectionEvent} (listeners may change the clan and
	 * the chance, or cancel), then rolls the chance, then silver armor's protection against bites.
	 *
	 * @param biter the wolf or werewolf for bites, otherwise null
	 * @param chance probability that the infection happens (bites: the configured risk; everything else 1)
	 */
	public boolean makeWerewolf(Player player, boolean turnNow, Clan clan, WerewolfInfectionEvent.Source source, Entity biter, double chance)
	{
		WerewolfInfectionEvent event = new WerewolfInfectionEvent(player, clan, source, turnNow, biter, chance);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled())
		{
			return false;
		}
		if (event.getChance() < 1.0 && this.random.nextDouble() >= event.getChance())
		{
			return false;
		}
		if ((source == WerewolfInfectionEvent.Source.WEREWOLF_BITE || source == WerewolfInfectionEvent.Source.WILD_WOLF_BITE) && silverArmorCleanses(player))
		{
			return false;
		}
		clan = event.getClan();
		if (clan == null)
		{
			clan = Clan.values()[this.random.nextInt(Clan.values().length)];
		}

		// The only place a werewolves.yml entry is created; every other setter only changes existing werewolves
		this.werewolves.getOrCreate(player.getUniqueId()).setPlayerName(player.getName());

		if (turnNow)
		{
			setHumanForm(player.getUniqueId(), player.getName());

			setInfectedThisNight(player.getUniqueId(), player.getName(), false);

			setWerewolfName(player.getUniqueId());

			// Last of the data: may make them alpha (NewAlphaEvent), so they must already be a werewolf
			setWerewolfClan(player.getUniqueId(), clan);

			// Only stored as wolf form if the transformation really started (not cancelled, /ww off, ...)
			if (setWerewolfSkin(player, WerewolfTransformEvent.Reason.INFECTION))
			{
				setWolfForm(player.getUniqueId(), player.getName());
			}

			this.plugin.log(player.getName() + " was made an full werewolf");
		}
		else
		{
			setInfectedWerewolf(player.getUniqueId(), player.getName());

			setLastTransformation(player.getUniqueId());

			setInfectedThisNight(player.getUniqueId(), player.getName(), true);

			setWerewolfName(player.getUniqueId());

			// Last of the data: may make them alpha (NewAlphaEvent), so they must already be a werewolf
			setWerewolfClan(player.getUniqueId(), clan);

			this.plugin.log(player.getUniqueId() + " was made a infected werewolf");
		}
		return true;
	}

	// Silver armor gives its wearer a chance to shake off a bite infection
	private boolean silverArmorCleanses(Player player)
	{
		int purity = ctx.getItemManager().getSilverArmorPurity(player);
		if (purity > 0 && this.random.nextInt(100) < purity)
		{
			player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverArmorCleansed, ChatColor.AQUA));
			return true;
		}
		return false;
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
		this.heldForms.remove(playerId);

		this.undisguiseCuredWerewolf(playerId, originalGroup);

		ctx.getClanManager().removeAlpha(playerId);

		this.plugin.log(plugin.getServer().getOfflinePlayer(playerId).getName() + " was unmade from being a werewolf");
		return true;
	}

	/** Starts the change into wolf form; false if the player can't change now or a listener cancelled it */
	public boolean setWerewolfSkin(Player player, WerewolfTransformEvent.Reason reason)
	{
		if (!ctx.isWerewolvesEnabled())
		{
			return false;
		}

		if (!isWerewolf(player))
		{
			return false;
		}

		if (hasWerewolfSkin(player.getUniqueId()))
		{
			return false;
		}

		if (isInfectedThisNight(player.getUniqueId()))
		{
			return false;
		}

		WerewolfTransformEvent event = new WerewolfTransformEvent(player, getWerewolfClan(player.getUniqueId()), true, reason);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled())
		{
			return false;
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
		if (reason == WerewolfTransformEvent.Reason.API)
		{
			this.heldForms.add(playerId);
		}
		else
		{
			this.heldForms.remove(playerId);
		}

		if (reason == WerewolfTransformEvent.Reason.MOON || reason == WerewolfTransformEvent.Reason.INFECTION)
		{
			grantFullMoonLevel(player);
		}

		this.disguiseWerewolf(player);
		return true;
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

	/** Starts the change back into human form; false if the player isn't in wolf form or a listener cancelled it */
	public boolean unsetWerewolfSkin(UUID playerId, WerewolfTransformEvent.Reason reason)
	{
		String playerName = plugin.getServer().getOfflinePlayer(playerId).getName();

		if (!isWerewolf(playerId))
		{
			this.plugin.logDebug("unsetWerewolfSkin(): " + playerName + " does not have werewolf skin");
			return false;
		}

		// Morning (or a command) ends the night's ban on transforming. A forced change back (join, quit, death, world
		// change, shutdown) doesn't, or relogging would lift it.
		if (reason != WerewolfTransformEvent.Reason.FORCED)
		{
			setInfectedThisNight(playerId, playerName, false);
		}

		if (!hasWerewolfSkin(playerId))
		{
			this.plugin.logDebug("unsetWerewolfSkin(): " + playerName + " does not have a werewolf skin");
			return false;
		}

		Player onlinePlayer = this.plugin.getServer().getPlayer(playerId);
		if (onlinePlayer != null)
		{
			WerewolfTransformEvent event = new WerewolfTransformEvent(onlinePlayer, getWerewolfClan(playerId), false, reason);
			plugin.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled())
			{
				return false;
			}
		}

		this.controlledFormStarts.remove(playerId);
		this.heldForms.remove(playerId);
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

		this.undisguiseWerewolf(playerId);
		return true;
	}

	/** Smoke, then {@link #setWerewolfSkin}; false if nothing happened */
	public boolean transform(Player player, WerewolfTransformEvent.Reason reason)
	{
		if (!isWerewolvesAllowedInWorld(player))
		{
			return false;
		}

		if (!hasWerewolfSkin(player.getUniqueId()))
		{
			playSmoke(player);
			return setWerewolfSkin(player, reason);
		}

		this.plugin.log("Could not transform " + player.getName() + ": already in wolf form");
		return false;
	}

	/** Smoke, then {@link #unsetWerewolfSkin}; false if nothing happened */
	public boolean untransform(Player player, WerewolfTransformEvent.Reason reason)
	{
		playSmoke(player);
		return unsetWerewolfSkin(player.getUniqueId(), reason);
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
	public void undisguiseWerewolf(UUID playerId)
	{
		runUndisguise(new UndisguiseTask(this.ctx, playerId, false, null));
	}

	/**
	 * The visual part of a cure: the caller has already removed the player's data. {@code originalGroup} is the
	 * permission group to put them back in, read before the data was removed.
	 */
	public void undisguiseCuredWerewolf(UUID playerId, String originalGroup)
	{
		runUndisguise(new UndisguiseTask(this.ctx, playerId, true, originalGroup));
	}

	/**
	 * A player who quits in wolf form turns human right away (FORCED), so they never stay in wolf form while offline:
	 * their walk speed, effects and permission group are saved as a human's, and a cure while offline has nothing to
	 * undo. Also clears what a change back still waiting in a task (e.g. a cure a moment ago) would have cleared.
	 */
	public void untransformOnQuit(Player player)
	{
		UUID playerId = player.getUniqueId();
		if (hasWerewolfSkin(playerId))
		{
			untransformNow(player);
		}

		if (!hasWerewolfSkin(playerId))
		{
			ctx.getSkinManager().unsetWerewolfSkin(player);
			if (ctx.getWolfFormManager().hasWolfForm(playerId))
			{
				ctx.getWolfFormManager().removeWolfForm(player, false);
			}
		}
	}

	/**
	 * On join: repairs what an older version, a crash, or a cure while offline left behind. Players are never in wolf
	 * form when they join (see {@link #untransformOnQuit}), so a stored WolfForm, a werewolf skin, the werewolf permission
	 * group or wolf form speed and effects are all left overs. CheckTransformationTask then transforms them if it's time.
	 */
	public void repairOnJoin(Player player)
	{
		UUID playerId = player.getUniqueId();

		// A listener cancelled the change back on quit
		if (hasWerewolfSkin(playerId))
		{
			untransformNow(player);
			if (hasWerewolfSkin(playerId))
			{
				return;
			}
		}

		ctx.getSkinManager().clearLeftoverSkin(player);

		if (isWerewolf(playerId))
		{
			// Their clan's effects at the configured strength go, potions they drank themselves stay
			ctx.getWolfFormManager().removeWolfForm(player, true);
			if (isWolfForm(playerId))
			{
				setHumanForm(playerId, player.getName());
			}
		}
		else
		{
			ctx.getWolfFormManager().clearLeftoverWolfForm(player);
		}

		String werewolfGroup = this.plugin.getSettings().general().werewolfGroupName();
		if (this.plugin.getSettings().general().werewolfGroupEnabled() && ctx.getPermissionsManager().isInGroup(player, werewolfGroup))
		{
			this.plugin.log("Taking " + player.getName() + " out of the werewolf group " + werewolfGroup + ", left over from before.");
			ctx.getPermissionsManager().leaveWerewolfGroup(player, werewolfGroup, getOriginalPermissionGroup(playerId));
		}
	}

	// Back to human form in this tick instead of a few ticks later (FORCED)
	private void untransformNow(Player player)
	{
		boolean wasImmediate = this.untransformImmediately;
		this.untransformImmediately = true;
		try
		{
			unsetWerewolfSkin(player.getUniqueId(), WerewolfTransformEvent.Reason.FORCED);
		}
		catch (RuntimeException e)
		{
			this.plugin.getLogger().log(Level.WARNING, "Could not turn " + player.getName() + " back to human form", e);
		}
		finally
		{
			this.untransformImmediately = wasImmediate;
		}
	}

	private void runUndisguise(UndisguiseTask task)
	{
		if (this.shuttingDown || this.untransformImmediately)
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

	/** Morning reverts every wolf form except ones entered with /ww transform (the time limit handles those) or through the API */
	public boolean canUntransform(Player player)
	{
		return (hasWerewolfSkin(player.getUniqueId()) || isInfectedThisNight(player.getUniqueId())) && !ctx.getMoonManager().isNight(player.getWorld()) && !isControlledForm(player.getUniqueId()) && !this.heldForms.contains(player.getUniqueId());
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

		// The night is used up even if a listener vetoes the level, so the event isn't fired again on every update
		record.setLastLevelNight(nightKey);
		if (!setLevel(player.getUniqueId(), record.getTransformations() + 1, WerewolfLevelChangeEvent.Cause.FULL_MOON))
		{
			return;
		}

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
	public Clan getWerewolfClan(UUID playerId)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record == null || record.getClan() == null)
		{
			return Clan.WerewolfBite;
		}

		return record.getClan();
	}

	public UUID getAlphaCandidate(Clan clan)
	{
		return getAlphaCandidate(clan, null);
	}

	/** The clan member best suited to be alpha: full werewolves before infected ones, then the highest level. Skips {@code exclude}. */
	public UUID getAlphaCandidate(Clan clan, UUID exclude)
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
	public List<String> getWerewolfClanMembers(Clan clan)
	{
		List<String> clanMembers = new ArrayList<String>();
		for (WerewolfRecord record : this.werewolves.inClan(clan))
		{
			clanMembers.add(record.getId().toString());
		}
		return clanMembers;
	}

	public List<UUID> getWerewolfClanMembersRanked(Clan clan)
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

	public void setWerewolfClan(UUID playerId, Clan clan)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record == null)
		{
			return;
		}

		Clan oldClan = record.getClan();
		record.setClan(clan);

		// Leaving a clan as its alpha hands the old clan to its next best member; an empty clan gets this player
		if (oldClan != null && oldClan != clan)
		{
			ctx.getClanManager().checkAlpha(oldClan);
		}
		ctx.getClanManager().checkAlpha(clan);
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

	/**
	 * Changes a werewolf's level (online or offline) through {@link WerewolfLevelChangeEvent}. Negative levels become 0.
	 *
	 * @return false if the player is not a werewolf or a listener cancelled the change; true otherwise (also when the
	 *         level already was that, which fires no event)
	 */
	public boolean setLevel(UUID playerId, int level, WerewolfLevelChangeEvent.Cause cause)
	{
		WerewolfRecord record = this.werewolves.get(playerId);
		if (record == null)
		{
			return false;
		}

		int oldLevel = record.getTransformations();
		int newLevel = Math.max(0, level);
		if (newLevel == oldLevel)
		{
			return true;
		}

		WerewolfLevelChangeEvent event = new WerewolfLevelChangeEvent(playerId, oldLevel, newLevel, cause);
		plugin.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled())
		{
			return false;
		}

		// A listener may have cured the werewolf meanwhile
		record = this.werewolves.get(playerId);
		if (record == null)
		{
			return false;
		}
		record.setTransformations(event.getNewLevel());
		return true;
	}

	/**
	 * An infected werewolf becomes a full werewolf in human form, as if it had already transformed once (API). No
	 * event; false if the player is not a werewolf.
	 */
	public boolean makeFullWerewolf(UUID playerId)
	{
		if (!isWerewolf(playerId))
		{
			return false;
		}
		if (isInfectedWerewolf(playerId))
		{
			String name = getPlayerName(playerId);
			setHumanForm(playerId, name);
			setInfectedThisNight(playerId, name, false);
		}
		return true;
	}

	/** The thread-safe snapshot of a werewolf, or null if the player is not a werewolf */
	public WerewolfProfile getProfile(UUID playerId)
	{
		return playerId == null ? null : this.profiles.get(playerId);
	}

	/** Snapshots of every werewolf; thread-safe, read-only view */
	public Collection<WerewolfProfile> getProfiles()
	{
		return Collections.unmodifiableCollection(this.profiles.values());
	}

	/**
	 * Rebuilds one werewolf's snapshot from the current data. Called on the main thread whenever a werewolf's record,
	 * form or alpha status changes (WerewolfRepository's change listener, popPlayerData, ClanManager).
	 */
	public void refreshProfile(UUID playerId)
	{
		if (playerId == null)
		{
			return;
		}

		WerewolfRecord record = this.werewolves.get(playerId);
		if (record == null || !isWerewolf(playerId))
		{
			this.profiles.remove(playerId);
			return;
		}

		String name = record.getWerewolfName() != null ? record.getWerewolfName() : "";
		this.profiles.put(playerId, new WerewolfProfile(playerId, getWerewolfClan(playerId), hasWerewolfSkin(playerId) ? Form.WOLF : Form.HUMAN, isInfectedWerewolf(playerId), record.getTransformations(), name, record.hasIntent(), ctx.getClanManager().isAlpha(playerId)));
	}

	/** Rebuilds every snapshot, after werewolves.yml or clans.yml was (re)loaded */
	public void refreshAllProfiles()
	{
		this.profiles.keySet().retainAll(new HashSet<UUID>(this.werewolves.ids()));
		for (UUID playerId : this.werewolves.ids())
		{
			refreshProfile(playerId);
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
		ctx.getMoonManager().snapshotPhases();

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