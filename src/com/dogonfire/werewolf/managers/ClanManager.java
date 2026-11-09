package com.dogonfire.werewolf.managers;

import com.dogonfire.werewolf.api.Clan;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

	private HashMap<Clan, String>	werewolfAccount		= new HashMap<Clan, String>();
	private HashMap<Clan, UUID>		werewolfAccountId	= new HashMap<Clan, UUID>();

	private HashMap<Clan, String>	werewolfTextures	= new HashMap<Clan, String>();
	private HashMap<Clan, String>	werewolfTextureSignatures	= new HashMap<Clan, String>();

	private final ClanRepository		clans;
	private HashMap<Clan, Double>	totalClanPoints		= new HashMap<Clan, Double>();
	private HashMap<String, Double>		playerClanPoints	= new HashMap<String, Double>();
	private HashMap<Clan, String>	clanNames			= new HashMap<Clan, String>();
	private String						datePattern			= "HH:mm:ss dd-MM-yyyy";
	
	private UUID 						alphaAccountId		= null;
	private String 						alphaAccount		= "";

	public ClanManager(WerewolfContext ctx, ClanRepository clans)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.clans = clans;
	}

	//Bloodmoon da508eccdbd946c5809547b91aa4ff5f
	//Silvermane b68a8f007d244c52b6ad1423bfbe26ee
	//Witherfang 68c5d9f216384a0f8f56d43fb368ad17
	//https://sessionserver.mojang.com/session/minecraft/profile/039c53aa-5873-4420-a095-9af971321408?unsigned=false
	//https://sessionserver.mojang.com/session/minecraft/profile/b68a8f00-7d24-4c52-b6ad-1423bfbe26ee?unsigned=false
	//https://sessionserver.mojang.com/session/minecraft/profile/da508ecc-dbd9-46c5-8095-47b91aa4ff5f?unsigned=false
	//UUID of player DogOnFire is 54f089fc-19e7-4a3e-9902-b90c5eb1cbed
	public void load()
	{
		Settings.Clans clans = plugin.getSettings().clans();
		this.alphaAccount = clans.alphaSkin().accountName();
		this.alphaAccountId = clans.alphaSkin().accountId();

		for (Clan clan : Clan.values())
		{
			Settings.Skin skin = clans.skins().get(clan);
			this.clanNames.put(clan, clans.names().get(clan));
			this.werewolfAccount.put(clan, skin.accountName());
			this.werewolfAccountId.put(clan, skin.accountId());
			this.werewolfTextures.put(clan, skin.value());
			this.werewolfTextureSignatures.put(clan, skin.signature());
		}
		
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

	public String getWerewolfAccountForClan(Clan type)
	{
		return this.werewolfAccount.get(type);
	}

	public UUID getWerewolfAccountIdForClan(Clan type)
	{
		return this.werewolfAccountId.get(type);
	}

	
	public String getWerewolfTextureForClan(Clan type)
	{
		return this.werewolfTextures.get(type);
	}
	
	public String getWerewolfTextureSignatureForClan(Clan type)
	{
		return this.werewolfTextureSignatures.get(type);
	}

	public String getWerewolfAccountForAlpha(Clan type)
	{
		return this.alphaAccount;
	}

	public UUID getWerewolfAccountIdForAlpha(Clan type)
	{
		return this.alphaAccountId;
	}

	public String getWerewolfTextureForAlpha(Clan type)
	{
		return plugin.getSettings().clans().alphaSkin().value();
	}

	public String getWerewolfTextureSignatureForAlpha(Clan type)
	{
		return plugin.getSettings().clans().alphaSkin().signature();
	}
	
	public void handleMobKill(Player player, Clan clanType, EntityType mobType)
	{
		double points = 0.0D;
		if (this.playerClanPoints.containsKey(player.getName()))
		{
			points = this.playerClanPoints.get(player.getName()).doubleValue();
		}
		
		switch (mobType)
		{
			case WARDEN:
			case ENDER_DRAGON:
			case WITHER:
				points = 50.0D;
				break;
			case PLAYER:
			case VILLAGER:
			case WANDERING_TRADER:
			case PILLAGER:
			case VINDICATOR:
			case EVOKER:
			case ILLUSIONER:
			case RAVAGER:
			case ZOGLIN:
			case PIGLIN_BRUTE:
			case ELDER_GUARDIAN:
			case ENDERMAN:
				points = 5.0D;
				break;
			case POLAR_BEAR:
			case HORSE:
			case DONKEY:
			case MULE:
			case COW:
			case MOOSHROOM:
			case ZOMBIE:
			case ZOMBIE_VILLAGER:
			case LLAMA:
			case TRADER_LLAMA:
			case PANDA:
			case DROWNED:
			case HUSK:
			case DOLPHIN:
			case PIGLIN:
			case STRIDER:
			case ZOMBIFIED_PIGLIN:
			case WITCH:
			case GUARDIAN:
			case SKELETON:
			case WITHER_SKELETON:
			case CAMEL:
			case SNIFFER:
				points = 3.0D;
				break;
			case SHEEP:
			case GOAT:
			case WOLF:
			case CAT:
			case OCELOT:
			case RABBIT:
			case PIG:
			case TURTLE:
			case FOX:
			case SPIDER:
			case GLOW_SQUID:
			case PHANTOM:
				points = 2.0D;
				break;
			case BLAZE:
			case STRAY:
			case CHICKEN:
			case PARROT:
			case AXOLOTL:
			case SQUID:
			case BEE:
			case COD:
			case SALMON:
			case PUFFERFISH:
			case TROPICAL_FISH:
			case FROG:
			case TADPOLE:
			case BAT:
			case VEX:
				points = 1.0D;
				break;
			case ALLAY:
			case ENDERMITE:
			case SNOW_GOLEM:
			case MAGMA_CUBE:
			case SILVERFISH:
			default:
				points = 0.5D;
				break;
		}
				
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
		if (candidate != null)
		{
			assignAlphaInClan(clan, candidate);
		}
		else if (alphaId != null)
		{
			assignAlphaInClan(clan, null);
		}
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
			if (candidate == null || !assignAlphaInClan(clan, candidate))
			{
				assignAlphaInClan(clan, null);
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

	public boolean assignAlphaInClan(Clan clan, UUID playerId)
	{
		if (playerId == null)
		{
			// Clearing fires no NewAlphaEvent, so the old alpha's skin is refreshed here
			UUID oldAlphaId = getAlpha(clan);
			this.clans.get(clan).setAlpha(null);
			ctx.getWerewolfManager().refreshWerewolfSkinLater(oldAlphaId);
		}
		else
		{
			UUID oldAlphaId = getAlpha(clan);

			if (playerId.equals(oldAlphaId))
			{
				return true;
			}

			NewAlphaEvent event = new NewAlphaEvent(clan, oldAlphaId, playerId);
			plugin.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled())
			{
				return false;
			}

			this.clans.get(clan).setAlpha(playerId);
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