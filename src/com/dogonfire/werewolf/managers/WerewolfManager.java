package com.dogonfire.werewolf.managers;

import java.io.File;
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
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfAPI;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.werewolf.events.WerewolfCureEvent;
import com.dogonfire.werewolf.events.WerewolfInfectionEvent;
import com.dogonfire.werewolf.events.WerewolfTransformEvent;
import com.dogonfire.werewolf.managers.ClanManager.ClanType;
/*
import org.bukkit.block.Biome;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.EntityType;
import org.bukkit.Location;
*/

public class WerewolfManager
{
	private Werewolf plugin;

	static enum WolfState {
		None, Infected, HumanForm, WolfForm;
	}

	private FileConfiguration		werewolvesConfig				= null;
	private File					werewolvesConfigFile			= null;
	private Random					random							= new Random();
	private long					lastSaveTime					= 0L;
	private String					datePattern						= "HH:mm:ss dd-MM-yyyy";
	private HashMap<UUID, String>	originalGroup					= new HashMap<UUID, String>();
	private HashMap<UUID, String>	playerlistNames					= new HashMap<UUID, String>();
	private HashMap<UUID, Long>		playersPouncing					= new HashMap<UUID, Long>();
	private HashMap<UUID, Integer>	packWolves						= new HashMap<UUID, Integer>();
	// Start time of wolf forms entered with /ww transform; only these are cut short by the time limit
	private HashMap<UUID, Long>		controlledFormStarts			= new HashMap<UUID, Long>();
	private HashMap<UUID, Long>		lastFormChanges					= new HashMap<UUID, Long>();
	private HashMap<UUID, Long>		lastHowls						= new HashMap<UUID, Long>();
	private HashMap<UUID, Long>		lastGrowls						= new HashMap<UUID, Long>();

	public WerewolfManager(Werewolf plugin)
	{
		this.plugin = plugin;
	}

	public void load()
	{
		if (this.werewolvesConfigFile == null)
		{
			this.werewolvesConfigFile = new File(this.plugin.getDataFolder(), "werewolves.yml");
		}
		this.werewolvesConfig = YamlConfiguration.loadConfiguration(this.werewolvesConfigFile);

		this.plugin.log("Loaded " + this.werewolvesConfig.getKeys(false).size() + " werewolves.");
	}

	public void saveTimed()
	{
		if (System.currentTimeMillis() - this.lastSaveTime < 180000L)
		{
			return;
		}

		save();
	}

	public void save()
	{
		this.lastSaveTime = System.currentTimeMillis();
		if ((this.werewolvesConfig == null) || (this.werewolvesConfigFile == null))
		{
			return;
		}
		try
		{
			this.werewolvesConfig.save(this.werewolvesConfigFile);
		}
		catch (Exception ex)
		{
			this.plugin.log("Could not save config to " + this.werewolvesConfigFile + ": " + ex.getMessage());
		}
	}

	public int getNumberOfWerewolves()
	{
		Set<String> werewolves = this.werewolvesConfig.getKeys(false);
		if (werewolves == null)
		{
			return 0;
		}
		return this.werewolvesConfig.getKeys(false).size();
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

		// this.plugin.logDebug("Putting " + player.getName() + ", " +
		// player.getUniqueId() + " into the playerData (" +
		// ChatColor.stripColor(player.getPlayerListName()) + ")");

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
		return this.werewolvesConfig.getString(playerId.toString() + ".OriginalGroup");
	}

	public void setOriginalPermissionGroup(UUID playerId, String groupName)
	{
		this.werewolvesConfig.set(playerId.toString() + ".OriginalGroup", groupName);
		saveTimed();
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

	public Set<UUID> getAllWerewolves()
	{
		Set<UUID> werewolfList = new HashSet<UUID>();

		for (String playerIdString : this.werewolvesConfig.getKeys(false))
		{
			try
			{
				UUID playerId = UUID.fromString(playerIdString);

				werewolfList.add(playerId);
			}
			catch (Exception ex)
			{

			}
		}

		return werewolfList;
	}

	public Set<UUID> getWerewolvesInClan(ClanManager.ClanType clanType)
	{
		Set<UUID> werewolfList = new HashSet<UUID>();
		UUID playerId;

		for (String playerIdString : this.werewolvesConfig.getKeys(false))
		{
			try
			{
				playerId = UUID.fromString(playerIdString);
			}
			catch (Exception ex)
			{
				continue;
			}

			ClanType playerClanType = this.getWerewolfClan(playerId);

			if (playerClanType.equals(clanType))
			{
				werewolfList.add(playerId);
			}
		}

		return werewolfList;
	}

	public void sendMessageToClan(ClanType clan, String message)
	{
		for (UUID playerId : getWerewolvesInClan(clan))
		{
			plugin.sendInfo(plugin.getServer().getPlayer(playerId), message);
		}
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
			Werewolf.pu.addPotionEffectNoGraphic(player, new PotionEffect(PotionEffectType.NAUSEA, 100, 1));
		}

		this.plugin.undisguiseWerewolf(playerId, true, true);

		if (this.plugin.useClans)
		{
			ClanManager.ClanType clan = Werewolf.getWerewolfManager().getWerewolfClan(playerId);
			Werewolf.getClanManager().assignAlphaInClan(clan, null);
		}

		this.plugin.log(plugin.getServer().getOfflinePlayer(playerId).getName() + " was unmade from being a werewolf");
		return true;
	}

	public void setWerewolfSkin(Player player, WerewolfTransformEvent.Reason reason)
	{
		if (!Werewolf.pluginEnabled)
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

		this.plugin.disguiseWerewolf(player);
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
				String message = Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.Untransform, ChatColor.LIGHT_PURPLE);
				player.sendMessage(message);
			}
		}

		this.plugin.log(playerName + " turned into human form!");

		this.plugin.undisguiseWerewolf(playerId, makeVisible, false);
	}

	public boolean canTransform(Player player)
	{
		return !hasWerewolfSkin(player.getUniqueId()) && !isInfectedThisNight(player.getUniqueId()) && Werewolf.getMoonManager().isTransformationNight(player.getWorld()) && this.plugin.isWerewolvesAllowedInWorld(player) && this.plugin.isUnderOpenSky(player);
	}

	/** Morning reverts every wolf form except ones entered with /ww transform, which the time limit handles */
	public boolean canUntransform(Player player)
	{
		return (hasWerewolfSkin(player.getUniqueId()) || isInfectedThisNight(player.getUniqueId())) && !Werewolf.getMoonManager().isNight(player.getWorld()) && !isControlledForm(player.getUniqueId());
	}

	public boolean isControlledForm(UUID playerId)
	{
		return this.controlledFormStarts.containsKey(playerId);
	}

	/** Seconds a /ww transform wolf form may last at this werewolf's level, or -1 if there is no limit */
	public long getTransformationTimeLimitSeconds(UUID playerId)
	{
		if (!this.plugin.transformationLimitEnabled)
		{
			return -1;
		}

		int level = getNumberOfTransformations(playerId);
		double base = this.plugin.transformationLimitBase;
		double modifier = this.plugin.transformationLimitModifier;
		double seconds;

		switch (this.plugin.transformationLimitEquation.toUpperCase())
		{
			case "LINEAR":
				seconds = base + modifier * level;
				break;
			case "EXPONENTIAL":
				seconds = base + Math.pow(level, modifier);
				break;
			default:
				seconds = base;
				break;
		}

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

		if (Werewolf.getMoonManager().isTransformationNight(player.getWorld()))
		{
			return false;
		}

		this.plugin.untransform(player, WerewolfTransformEvent.Reason.TIME_LIMIT);
		if (isControlledForm(player.getUniqueId()))
		{
			// A listener cancelled the untransformation
			return false;
		}

		player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TransformationTimeExpired, ChatColor.LIGHT_PURPLE));
		return true;
	}

	/** Gives a level for this transformation night, at most once per night even across relogs and restarts */
	public void grantFullMoonLevel(Player player)
	{
		World world = player.getWorld();
		if (!Werewolf.getMoonManager().isTransformationNight(world))
		{
			return;
		}

		String nightKey = Werewolf.getMoonManager().getNightKey(world);
		String lastNightKey = this.werewolvesConfig.getString(player.getUniqueId().toString() + ".LastLevelNight");
		if (nightKey.equals(lastNightKey))
		{
			return;
		}

		this.werewolvesConfig.set(player.getUniqueId().toString() + ".LastLevelNight", nightKey);
		incrementNumberOfFullMoonTransformations(player.getUniqueId());
		saveTimed();

		Werewolf.getLanguageManager().setAmount("" + getNumberOfTransformations(player.getUniqueId()));
		player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.FullMoonLevelGained, ChatColor.LIGHT_PURPLE));
	}

	/** Seconds left before the werewolf can /ww transform into wolf form again, 0 if ready */
	public long getTransformCooldownLeft(UUID playerId)
	{
		return getCooldownLeft(this.lastFormChanges, playerId, this.plugin.transformCooldownSeconds);
	}

	public long getHowlCooldownLeft(UUID playerId)
	{
		return getCooldownLeft(this.lastHowls, playerId, this.plugin.howlCooldownSeconds);
	}

	public long getGrowlCooldownLeft(UUID playerId)
	{
		return getCooldownLeft(this.lastGrowls, playerId, this.plugin.growlCooldownSeconds);
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
		return getNumberOfTransformations(playerId) >= this.plugin.transformsForGoldImmunity;
	}

	public boolean canUseIntent(UUID playerId)
	{
		return getNumberOfTransformations(playerId) >= this.plugin.levelForIntent;
	}

	public boolean hasIntent(UUID playerId)
	{
		return this.werewolvesConfig.getBoolean(playerId.toString() + ".Intent", false);
	}

	public void setIntent(UUID playerId, boolean intent)
	{
		this.werewolvesConfig.set(playerId.toString() + ".Intent", intent ? Boolean.TRUE : null);
		saveTimed();
	}

	/** Chance that a bite from this werewolf infects; intent raises it once the werewolf is mature enough */
	public double getBiteInfectionRisk(UUID werewolfId)
	{
		if (this.plugin.werewolfInfectionRisk <= 0)
		{
			return 0;
		}

		if (hasIntent(werewolfId) && canUseIntent(werewolfId))
		{
			return this.plugin.werewolfIntentInfectionRisk;
		}

		return this.plugin.werewolfInfectionRisk;
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

		String wolfState = this.werewolvesConfig.getString(playerId.toString() + ".WolfState");
		
		return wolfState != null;
	}

	public boolean isFullWerewolf(UUID playerId)
	{
		if (playerId == null)
		{
			return false;
		}

		String wolfType = this.werewolvesConfig.getString(playerId.toString() + ".WolfState");

		if (wolfType == null)
		{
			return false;
		}

		WolfState wolfState = WolfState.None;

		try
		{
			wolfState = WolfState.valueOf(wolfType);
		}
		catch (Exception ex)
		{
			setNotWerewolf(playerId);
			return false;
		}

		return (wolfState == WolfState.WolfForm) || (wolfState == WolfState.HumanForm);
	}

	public boolean isInfectedWerewolf(UUID playerId)
	{
		String wolfType = this.werewolvesConfig.getString(playerId.toString() + ".WolfState");
		if (wolfType == null)
		{
			return false;
		}
		WolfState wolfState = WolfState.None;
		try
		{
			wolfState = WolfState.valueOf(wolfType);
		}
		catch (Exception ex)
		{
			setNotWerewolf(playerId);
			return false;
		}
		return wolfState == WolfState.Infected;
	}

	public boolean isWolfForm(UUID playerId)
	{
		if (playerId == null)
		{
			return false;
		}

		String wolfType = this.werewolvesConfig.getString(playerId.toString() + ".WolfState");

		WolfState wolfState = WolfState.None;

		try
		{
			wolfState = WolfState.valueOf(wolfType);
		}
		catch (Exception ex)
		{
			setNotWerewolf(playerId);
			return false;
		}

		return wolfState == WolfState.WolfForm;
	}

	public boolean isHumanForm(UUID playerId)
	{
		if (playerId == null)
		{
			return false;
		}

		String wolfType = this.werewolvesConfig.getString(playerId.toString() + ".WolfState");

		if (wolfType == null)
		{
			return false;
		}

		WolfState wolfState = WolfState.None;

		try
		{
			wolfState = WolfState.valueOf(wolfType);
		}
		catch (Exception ex)
		{
			setNotWerewolf(playerId);
			return false;
		}

		return wolfState == WolfState.HumanForm;
	}

	public String getWerewolfName(UUID playerId)
	{
		if (playerId == null)
		{
			return "";
		}

		String werewolfName = this.werewolvesConfig.getString(playerId.toString() + ".WerewolfName");

		if (werewolfName == null || werewolfName.isEmpty())
		{
			setWerewolfName(playerId);
			werewolfName = this.werewolvesConfig.getString(playerId.toString() + ".WerewolfName");
		}

		return werewolfName;
	}

	public ClanManager.ClanType getWerewolfClan(UUID playerId)
	{
		if (playerId == null)
		{
			return ClanManager.ClanType.Potion;
		}

		String clanTypeString = this.werewolvesConfig.getString(playerId.toString() + ".Clan");

		ClanManager.ClanType clanType = ClanManager.ClanType.WerewolfBite;

		try
		{
			clanType = ClanManager.ClanType.valueOf(clanTypeString);
		}
		catch (Exception ex)
		{
			clanType = ClanManager.ClanType.values()[this.random.nextInt(ClanManager.ClanType.values().length)];
			this.werewolvesConfig.set(playerId.toString() + ".Clan", clanType.name());

			saveTimed();
		}

		return clanType;
	}

	public UUID getAlphaCandidate(ClanManager.ClanType clan)
	{
		List<UUID> clanMembers = Werewolf.getWerewolfManager().getWerewolfClanMembersRanked(clan);// new
																									// ArrayList<UUID>();

		/*
		 * for (String playerIdString : playerList) { try { UUID playerId =
		 * UUID.fromString(playerIdString);
		 * 
		 * String clanName = this.werewolvesConfig.getString(playerId.toString()
		 * + ".Clan"); if (clanName != null && clanName.equals(clan.name())) {
		 * clanMembers.add(playerId); } } catch(Exception ex) {
		 * 
		 * } }
		 * 
		 * Collections.sort(clanMembers, new TransformationsComparator());
		 */

		if (clanMembers.size() == 0)
		{
			return null;
		}

		// if(clanMembers.size()<2)
		// {
		return clanMembers.get(0);
		// }

		// int n = random.nextInt(clanMembers.size() - 1);

		// return clanMembers.get(n);
	}

	public class TransformationsComparator implements Comparator<UUID>
	{
		public int compare(UUID member1, UUID member2)
		{
			return Werewolf.getWerewolfManager().getNumberOfTransformations(member2) - Werewolf.getWerewolfManager().getNumberOfTransformations(member1);
		}
	}

	public List<String> getWerewolfClanMembers(ClanManager.ClanType clan)
	{
		Set<String> playerList = this.werewolvesConfig.getKeys(false);
		List<String> clanMembers = new ArrayList<String>();

		for (String playerName : playerList)
		{
			String clanString = this.werewolvesConfig.getString(playerName + ".Clan");
			if (clanString != null)
			{
				ClanManager.ClanType clanType = null;

				try
				{
					clanType = ClanManager.ClanType.valueOf(clanString);
				}
				catch (Exception localException)
				{
				}

				if (clanType.equals(clan))
				{
					clanMembers.add(playerName);
				}
			}
		}
		return clanMembers;
	}

	public List<UUID> getWerewolfClanMembersRanked(ClanManager.ClanType clan)
	{
		Set<String> playerList = this.werewolvesConfig.getKeys(false);
		List<UUID> clanMembers = new ArrayList<UUID>();

		for (String playerString : playerList)
		{
			try
			{
				UUID playerId = UUID.fromString(playerString);
				String clanName = this.werewolvesConfig.getString(playerId.toString() + ".Clan");

				if ((clanName != null) && clanName.equals(clan.name()))
				{
					clanMembers.add(playerId);
				}
			}
			catch (Exception ex)
			{

			}
		}

		Collections.sort(clanMembers, new TransformationsComparator());

		return clanMembers;
	}

	public boolean isInfectedThisNight(UUID playerId)
	{
		return this.werewolvesConfig.getBoolean(playerId.toString() + ".InfectedThisNight");
	}

	public void setInfectedThisNight(UUID playerId, String playerName, boolean thisNight)
	{
		this.werewolvesConfig.set(playerId.toString() + ".PlayerName", playerName);

		if (thisNight)
		{
			this.werewolvesConfig.set(playerId.toString() + ".InfectedThisNight", thisNight);
		}
		else
		{
			this.werewolvesConfig.set(playerId.toString() + ".InfectedThisNight", thisNight);
		}

		saveTimed();
	}

	public void setWerewolfClan(UUID playerId, ClanManager.ClanType clan)
	{
		if (Werewolf.getClanManager().getAlpha(clan) == null)
		{
			UUID alphaPlayerId = getAlphaCandidate(clan);

			if (alphaPlayerId == null)
			{
				Werewolf.getClanManager().assignAlphaInClan(clan, playerId);
			}
			else
			{
				Werewolf.getClanManager().assignAlphaInClan(clan, alphaPlayerId);
			}
		}

		this.werewolvesConfig.set(playerId.toString() + ".Clan", clan.name());

		saveTimed();
	}

	public void incrementNumberOfFullMoonTransformations(UUID playerId)
	{
		int transforms = this.werewolvesConfig.getInt(playerId.toString() + ".Transformations");

		transforms++;

		this.werewolvesConfig.set(playerId.toString() + ".Transformations", transforms);
	}

	public void setLastTransformation(UUID playerId)
	{
		DateFormat formatter = new SimpleDateFormat(this.datePattern);
		Date thisDate = new Date();

		this.werewolvesConfig.set(playerId.toString() + ".LastTransform", formatter.format(thisDate));

		saveTimed();
	}

	public int getNumberOfTransformations(UUID playerId)
	{
		return this.werewolvesConfig.getInt(playerId.toString() + ".Transformations");
	}

	public boolean canControlledTransformation(UUID playerId)
	{
		return getNumberOfTransformations(playerId) >= this.plugin.transformsForControlledTransformation;
	}

	public boolean hasToDropItems(UUID playerId)
	{
		return getNumberOfTransformations(playerId) < this.plugin.transformsForNoDropItems;
	}

	public boolean hasRecentTransformAutoCure(UUID playerId)
	{
		DateFormat formatter = new SimpleDateFormat(this.datePattern);
		Date thisDate = new Date();
		Date transformDate = null;

		String transformDateString = this.werewolvesConfig.getString(playerId.toString() + ".LastTransform");
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

		return diffDays < this.plugin.autoCureDays;
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
		List<String> strings = plugin.givenNames;
		if (strings.size() == 0)
		{
			this.plugin.log("No given name strings found in the config!");
		}
		else
		{
			givenName = (String) strings.toArray()[this.random.nextInt(strings.size())];
		}

		// Generate a random surname
		List<String> strings2 = plugin.surnames;
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
		this.werewolvesConfig.set(playerId.toString() + ".WerewolfName", newWerewolfName());

		saveTimed();
	}

	public void setHumanForm(UUID playerId, String playerName)
	{
		this.werewolvesConfig.set(playerId.toString() + ".PlayerName", playerName);
		this.werewolvesConfig.set(playerId.toString() + ".WolfState", WolfState.HumanForm.name());

		saveTimed();
	}

	public void setWolfForm(UUID playerId, String playerName)
	{
		this.werewolvesConfig.set(playerId.toString() + ".PlayerName", playerName);
		this.werewolvesConfig.set(playerId.toString() + ".WolfState", WolfState.WolfForm.name());

		saveTimed();
	}

	public void setInfectedWerewolf(UUID playerId, String playerName)
	{
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		this.werewolvesConfig.set(playerId.toString() + ".PlayerName", playerName);
		this.werewolvesConfig.set(playerId.toString() + ".WolfState", WolfState.Infected.name());
		this.werewolvesConfig.set(playerId.toString() + ".InfectedDate", formatter.format(thisDate));

		saveTimed();
	}

	public void setNotWerewolf(UUID playerId)
	{
		this.werewolvesConfig.set(playerId.toString(), null);

		saveTimed();
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

		if (this.random.nextInt(20 + 10 * Werewolf.getWerewolfManager().getNumberOfTransformations(player.getUniqueId())) > 0)
		{
			return;
		}

		if (!isWerewolf(player))
		{
			return;
		}

		if (Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			message = Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UrgesInfectedThisNight, ChatColor.LIGHT_PURPLE);
		}
		else if (isInfectedWerewolf(player.getUniqueId()))
		{
			if (isInfectedThisNight(player.getUniqueId()))
			{
				message = Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UrgesInfectedThisNight, ChatColor.LIGHT_PURPLE);
			}
			else
			{
				message = Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UrgesInfectedHumanForm, ChatColor.LIGHT_PURPLE);
			}
		}
		else if (Werewolf.getMoonManager().isNight(player.getWorld()) && player.getWorld().getHighestBlockYAt(player.getLocation()) > player.getLocation().getBlockY())
		{
			message = Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UrgesInside, ChatColor.LIGHT_PURPLE);
		}

		if (!message.isEmpty())
		{
			player.sendMessage(message);
		}
	}

	public void setPouncing(UUID playerId)
	{
		this.playersPouncing.put(playerId, Long.valueOf(System.currentTimeMillis()));
	}

	public boolean isPouncing(UUID playerId)
	{
		return this.playersPouncing.containsKey(playerId);
	}

	public void removePouncing(UUID playerId)
	{
		if (!this.playersPouncing.containsKey(playerId))
		{
			return;
		}

		if (this.playersPouncing.get(playerId) < System.currentTimeMillis() - 1000L)
		{
			this.playersPouncing.remove(playerId);
		}
	}

	public void increaseNumberOfPackWolvesForPlayer(UUID playerId)
	{
		int numberOfWolves = 0;
		if (this.packWolves.containsKey(playerId))
		{
			numberOfWolves = ((Integer) this.packWolves.get(playerId)).intValue();
		}
		numberOfWolves++;

		this.packWolves.put(playerId, numberOfWolves);
	}

	public int getNumberOfPackWolvesForPlayer(UUID playerId)
	{
		if (this.packWolves.containsKey(playerId))
		{
			return this.packWolves.get(playerId);
		}

		return 0;
	}

	public void clearNumberOfPackWolvesForPlayer(UUID playerId)
	{
		if (this.packWolves.containsKey(playerId))
		{
			this.packWolves.remove(playerId);
		}
	}

	public void update()
	{
		if (!Werewolf.pluginEnabled)
		{
			return;
		}

		Werewolf.getMoonManager().update();

		if (this.plugin.autoCureDays > 0 && this.random.nextInt(100) == 0 && Werewolf.getWerewolfManager().getAllWerewolves().size() > 0)
		{
			int r = this.random.nextInt(Werewolf.getWerewolfManager().getAllWerewolves().size());
			UUID werewolfPlayerId = (UUID) Werewolf.getWerewolfManager().getAllWerewolves().toArray()[r];

			if (!hasRecentTransformAutoCure(werewolfPlayerId))
			{
				String werewolfPlayerName = plugin.getServer().getOfflinePlayer(werewolfPlayerId).getName();
				if (unmakeWerewolf(werewolfPlayerId, WerewolfCureEvent.Cause.AUTO_CURE))
				{
					this.plugin.log(werewolfPlayerName + " has not transformed for " + this.plugin.autoCureDays + " days. Removing their werewolf infection.");
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

			if (WerewolfAPI.isVampire(player))
			{
				this.plugin.log(player.getName() + " is a Vampire! Removing their Werewolf infection...");
				unmakeWerewolf(player.getUniqueId(), WerewolfCureEvent.Cause.VAMPIRISM);
				changedForm.add(player);
			}
			else if (canTransform(player))
			{
				this.plugin.transform(player, WerewolfTransformEvent.Reason.MOON);
				changedForm.add(player);
			}
			else if (canUntransform(player))
			{
				this.plugin.untransform(player, WerewolfTransformEvent.Reason.MOON);
				changedForm.add(player);
			}
			else if (checkTransformationTimeLimit(player))
			{
				changedForm.add(player);
			}
			else if (hasWerewolfSkin(player.getUniqueId()) && this.plugin.isWerewolvesAllowedInWorld(player) && this.plugin.isUnderOpenSky(player))
			{
				// Already in wolf form when the moon rose (e.g. after /ww transform): still counts for this night's level
				grantFullMoonLevel(player);
			}

			if (!changedForm.contains(player) && hasWerewolfSkin(player.getUniqueId()))
			{
				Werewolf.getWolfFormManager().refreshWolfForm(player);
			}
		}

		// Urges are rare flavour messages, so only one random player gets a chance per update
		Player player = (Player) this.plugin.getServer().getOnlinePlayers().toArray()[this.random.nextInt(this.plugin.getServer().getOnlinePlayers().size())];

		if (this.plugin.werewolfUrges && !changedForm.contains(player) && isWerewolf(player))
		{
			sendWerewolfUrges(player);
		}

		/*
		 * if (this.random.nextInt(10) == 0) { if
		 * (isValidBiomeForWildWolf(player.getWorld().getBiome(player.
		 * getLocation().getBlockX(), player.getLocation().getBlockZ()))) { int
		 * wolves = getNumberOfPackWolvesForPlayer(player.getUniqueId()); if
		 * (this.random.nextInt(50 * wolves + 50) == 0) { spawnWildWolf(player);
		 * } } }
		 */

		if (this.plugin.autoBounty && this.plugin.vaultEnabled)
		{
			Werewolf.getHuntManager().autoAddBounty();
		}
	}

	/*
	 * private boolean isValidBiomeForWildWolf(Biome biome) { Boolean bool;
	 * switch (biome) { case SNOWY_TAIGA: case SNOWY_TAIGA_HILLS: case TAIGA:
	 * case GIANT_SPRUCE_TAIGA: case FOREST: bool = true; default: bool = false;
	 * } return bool; }
	 * 
	 * private void spawnWildWolf(Player player) { World world =
	 * player.getWorld(); Location center = player.getLocation(); int minDist =
	 * 15; int maxDist = 20; int x; int z;
	 * 
	 * do { x = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockX();
	 * z = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockZ(); }
	 * while ((Math.abs(x - center.getBlockX()) < minDist) || (Math.abs(z -
	 * center.getBlockZ()) < minDist));
	 * 
	 * int y = world.getHighestBlockYAt(x, z);
	 * 
	 * Location spawnLocation = new Location(world, x, y, z);
	 * 
	 * Wolf wolf = (Wolf) world.spawnEntity(spawnLocation, EntityType.WOLF);
	 * 
	 * wolf.setTarget(player); }
	 */
}