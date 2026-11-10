package com.dogonfire.werewolf.managers;

import com.dogonfire.werewolf.api.Clan;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.UUID;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.config.Settings;
import com.dogonfire.werewolf.api.event.NewAlphaEvent;
import com.dogonfire.werewolf.storage.ClanRecord;
import com.dogonfire.werewolf.storage.ClanRepository;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;


public class ClanManager
{
	private final Werewolf plugin;
	private final WerewolfContext ctx;

	private final ClanRepository		clans;
	private HashMap<Clan, Double>	totalClanPoints		= new HashMap<Clan, Double>();
	private HashMap<String, Double>		playerClanPoints	= new HashMap<String, Double>();
	// Replaced as a whole on load, so other threads (PlaceholderAPI, the API) can read it
	private volatile Map<Clan, String>	clanNames		= Collections.emptyMap();
	private String						datePattern			= "HH:mm:ss dd-MM-yyyy";
	
	// Per clan, the last successor a NewAlphaEvent listener vetoed, with the members at that time
	private final Map<Clan, VetoedSuccession>	vetoedSuccessions	= new EnumMap<Clan, VetoedSuccession>(Clan.class);

	public ClanManager(WerewolfContext ctx, ClanRepository clans)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.clans = clans;
	}

	public void load()
	{
		Settings.Clans clans = plugin.getSettings().clans();
		Map<Clan, String> names = new EnumMap<Clan, String>(Clan.class);

		for (Clan clan : Clan.values())
		{
			names.put(clan, clans.names().get(clan));
		}
		
		this.clanNames = Collections.unmodifiableMap(names);

		for (Clan clan : Clan.values())
		{
			this.totalClanPoints.put(clan, Double.valueOf(0.0D));
		}
		
		resetClanScores();
	}

	public String getClanName(UUID playerId)
	{
		return this.clanNames.get(ctx.getWerewolfManager().getWerewolfClan(playerId));
	}

	/** The werewolf skin of this clan: its alpha skin (the clan's own, or Clans.AlphaSkin) or its normal skin */
	public Settings.Skin getWerewolfSkin(Clan type, boolean alpha)
	{
		return plugin.getSettings().clans().skin(type, alpha);
	}
	
	public void handleMobKill(Player player, Clan clanType, EntityType mobType)
	{
		double points = 0.0D;
		if (this.playerClanPoints.containsKey(player.getName()))
		{
			points = this.playerClanPoints.get(player.getName()).doubleValue();
		}
		
		points = this.plugin.getSettings().clans().mobPoints().get(mobType).doubleValue();
				
		if (points > 0)
		{
			this.playerClanPoints.put(player.getName(), Double.valueOf(points));

			double totalPoints = ((Double) this.totalClanPoints.get(clanType)).doubleValue();

			totalPoints += points / ctx.getWerewolfManager().getWerewolfClanMembers(clanType).size();

			this.totalClanPoints.put(clanType, Double.valueOf(totalPoints));
		}
	}

	public List<Clan> getClansRanked()
	{
		List<Clan> clanList = new ArrayList<Clan>();
		for (Clan clan : Clan.values())
		{
			clanList.add(clan);
		}
		Collections.sort(clanList, new ClanComparator());

		return clanList;
	}

	public double getClanPoint(Clan clan)
	{
		return ((Double) this.totalClanPoints.get(clan)).doubleValue();
	}

	/** The clan's display name (config); thread-safe */
	public String getClanName(Clan clan)
	{
		return (String) this.clanNames.get(clan);
	}

	public void updateClans()
	{
		if (!this.plugin.getSettings().clans().enabled())
		{
			return;
		}
		List<Clan> clanList = getClansRanked();
		if ((((Double) this.totalClanPoints.get(Clan.Potion)).doubleValue() > 0.0D) || (((Double) this.totalClanPoints.get(Clan.WildBite)).doubleValue() > 0.0D) || (((Double) this.totalClanPoints.get(Clan.WerewolfBite)).doubleValue() > 0.0D))
		{
			setBloodrageClan((Clan) clanList.get(0));
		}
	}

	private void setBloodrageClan(Clan clan)
	{
	}

	private void resetClanScores()
	{
		this.playerClanPoints.clear();
		for (Clan clan : Clan.values())
		{
			double points = ((Double) this.totalClanPoints.get(clan)).doubleValue();

			this.totalClanPoints.put(clan, Double.valueOf(points / 2.0D));
		}
	}

	public void setHomeForClan(Clan clan, Location location)
	{
		this.clans.get(clan).setHome(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
	}

	public Location getHomeForClan(Clan clan)
	{
		ClanRecord record = this.clans.get(clan);
		World world = record.getHomeWorld() != null ? this.plugin.getServer().getWorld(record.getHomeWorld()) : null;
		if (world == null)
		{
			return null;
		}

		return new Location(world, record.getHomeX(), record.getHomeY(), record.getHomeZ());
	}
	
	
	/** The clan's alpha as stored in clans.yml, or null. Can be stale for a moment until {@link #checkAlphas()} runs. */
	public UUID getAlpha(Clan clan)
	{
		return this.clans.get(clan).getAlpha();
	}

	private boolean isValidAlpha(Clan clan, UUID alphaId)
	{
		WerewolfManager werewolfManager = ctx.getWerewolfManager();
		return alphaId != null && werewolfManager.isWerewolf(alphaId) && werewolfManager.getWerewolfClan(alphaId) == clan;
	}

	/**
	 * Gives the clan an alpha if it has none, or if its alpha is no longer a werewolf of that clan: the best ranked
	 * member becomes alpha (through {@link #assignAlphaInClan}, so it can be vetoed).
	 */
	public void checkAlpha(Clan clan)
	{
		UUID alphaId = getAlpha(clan);
		if (isValidAlpha(clan, alphaId))
		{
			return;
		}

		UUID candidate = ctx.getWerewolfManager().getAlphaCandidate(clan, alphaId);
		if ((candidate == null || !trySuccession(clan, candidate)) && alphaId != null)
		{
			assignAlphaInClan(clan, null, NewAlphaEvent.Cause.SUCCESSION);
		}
	}

	/**
	 * Makes the candidate alpha by succession. A candidate a listener vetoed is not asked about again (this runs every
	 * update) until the clan's members change.
	 */
	private boolean trySuccession(Clan clan, UUID candidate)
	{
		Set<UUID> members = new HashSet<UUID>(ctx.getWerewolfManager().getWerewolvesInClan(clan));
		VetoedSuccession vetoed = this.vetoedSuccessions.get(clan);
		if (vetoed != null && vetoed.candidate().equals(candidate) && vetoed.members().equals(members))
		{
			return false;
		}

		if (assignAlphaInClan(clan, candidate, NewAlphaEvent.Cause.SUCCESSION))
		{
			this.vetoedSuccessions.remove(clan);
			return true;
		}

		this.vetoedSuccessions.put(clan, new VetoedSuccession(candidate, members));
		return false;
	}

	private record VetoedSuccession(UUID candidate, Set<UUID> members)
	{
	}

	/** Runs from the main loop: fills every empty (or stale) alpha slot */
	public void checkAlphas()
	{
		if (!this.plugin.getSettings().clans().enabled())
		{
			return;
		}

		for (Clan clan : Clan.values())
		{
			checkAlpha(clan);
		}
	}

	/** The player stops being alpha of any clan (cured, or moved to another clan); the next best member takes over */
	public void removeAlpha(UUID playerId)
	{
		for (Clan clan : Clan.values())
		{
			if (!isAlpha(clan, playerId))
			{
				continue;
			}

			UUID candidate = ctx.getWerewolfManager().getAlphaCandidate(clan, playerId);
			if (candidate == null || !trySuccession(clan, candidate))
			{
				assignAlphaInClan(clan, null, NewAlphaEvent.Cause.SUCCESSION);
			}
		}
	}

	/** The best ranked member of the clan other than the alpha, or null if there is none */
	public UUID getRunnerUp(Clan clan)
	{
		return ctx.getWerewolfManager().getAlphaCandidate(clan, getAlpha(clan));
	}

	public boolean isAlpha(UUID playerId)
	{
		for (Clan clan : Clan.values())
		{
			if (isAlpha(clan, playerId))
			{
				return true;
			}
		}

		return false;
	}

	public boolean isAlpha(Clan clan, UUID playerId)
	{
		UUID alphaId = getAlpha(clan);
		return alphaId != null && alphaId.equals(playerId);
	}

	/**
	 * Makes the player the clan's alpha (fires {@link NewAlphaEvent}; false if a listener cancelled it), or clears the
	 * slot when {@code playerId} is null (no event). The caller checks that the player is a werewolf of that clan.
	 */
	public boolean assignAlphaInClan(Clan clan, UUID playerId, NewAlphaEvent.Cause cause)
	{
		if (playerId == null)
		{
			// Clearing fires no NewAlphaEvent, so the old alpha's skin is refreshed here
			UUID oldAlphaId = getAlpha(clan);
			this.clans.get(clan).setAlpha(null);
			ctx.getWerewolfManager().refreshProfile(oldAlphaId);
			ctx.getWerewolfManager().refreshWerewolfSkinLater(oldAlphaId);
		}
		else
		{
			UUID oldAlphaId = getAlpha(clan);

			if (playerId.equals(oldAlphaId))
			{
				return true;
			}

			NewAlphaEvent event = new NewAlphaEvent(clan, oldAlphaId, playerId, cause);
			plugin.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled())
			{
				return false;
			}

			this.clans.get(clan).setAlpha(playerId);
			ctx.getWerewolfManager().refreshProfile(oldAlphaId);
			ctx.getWerewolfManager().refreshProfile(playerId);
		}

		return true;
	}

	public void setLastCall(Clan clan)
	{
		Date thisDate = new Date();
		DateFormat formatter = new SimpleDateFormat(this.datePattern);

		this.clans.get(clan).setLastCallTime(formatter.format(thisDate));
	}

	public class ClanComparator implements Comparator<Clan>
	{
		public int compare(Clan clan1, Clan clan2)
		{
			return (int) (((Double) ClanManager.this.totalClanPoints.get(clan2)).doubleValue() - ((Double) ClanManager.this.totalClanPoints.get(clan1)).doubleValue());
		}
	}
}