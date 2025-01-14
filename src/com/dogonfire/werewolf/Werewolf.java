package com.dogonfire.werewolf;

import com.clanjhoo.vampire.VampireAPI;
import com.dogonfire.werewolf.commands.Commands;
import com.dogonfire.werewolf.tasks.CompassTrackerTask;
import com.dogonfire.werewolf.utils.Metrics;
import com.dogonfire.werewolf.utils.Metrics.Graph;
import com.dogonfire.werewolf.disguises.WerewolfDisguiseAPI;
import com.dogonfire.werewolf.listeners.ChatListener;
import com.dogonfire.werewolf.listeners.DamageListener;
import com.dogonfire.werewolf.listeners.InteractListener;
import com.dogonfire.werewolf.listeners.InventoryListener;
import com.dogonfire.werewolf.listeners.PlayerListener;
import com.dogonfire.werewolf.managers.*;
import com.dogonfire.werewolf.tasks.CentralMessageTask;
import com.dogonfire.werewolf.tasks.DisguiseTask;
import com.dogonfire.werewolf.tasks.UndisguiseTask;
import com.dogonfire.werewolf.utils.PacketUtils;
import com.dogonfire.werewolf.utils.WerewolfPlaceholderExpansion;
import com.dogonfire.werewolf.versioning.Version;
import com.dogonfire.werewolf.versioning.VersionFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Werewolf extends JavaPlugin
{
	public static Werewolf 						instance;
	public static boolean						pluginEnabled							= false;

	public boolean 								vampireEnabled 							= false;
	public boolean								vaultEnabled							= false;
	public boolean								disguisesEnabled						= false;
	public boolean								healthBarEnabled						= false;
	public static int							nightStart								= 13300;
	public static int							nightEnd								= 23500;
	public int									wolfdistance							= 10;
	public double								cureChance								= 1.0D;
	public double								wildWolfInfectionRisk					= 1.0D;
	public double								werewolfInfectionRisk					= 1.0D;
	public double								potionInfectChance						= 1.0D;
	public int									compassUpdateRate						= 100;
	public ArrayList<String>					wolfMessage								= new ArrayList<String>();
	public boolean								movementUpdateThreading					= true;
	public int									movementUpdateFrequency					= 4;
	public ConcurrentHashMap<Player, Integer>	positionUpdaters						= new ConcurrentHashMap<Player, Integer>();
	public static Server						server									= null;
	public boolean								debug									= false;
	public String								language								= "english";
	private final Set<String>					supportedLanguages						= new HashSet<String>(Arrays.asList(new String[] { "english", "german", "french", "chinese", "polish", "danish", "portuguese", "spanish" }));
	public boolean								renameWerewolves						= true;
	public boolean								autoBounty								= true;
	public int									autoBountyMaximum						= 1000;
	public boolean								werewolfUrges							= true;
	public boolean								announceFullmoons						= true;
	public boolean								wolfChat								= true;
	public boolean								useWerewolfGroupName					= false;
	public boolean								useTrophies								= true;
	public boolean								dropArmorOnTransform					= true;
	public boolean								onlyTransformDuringFullMoon				= true;
	public boolean								keepWerewolfHandsFree					= true;
	public boolean								cureWerewolfWhenSlain					= true;
	public String								werewolfGroupName						= "Werewolf";
	public int									autoCureDays							= 14;
	public boolean								useSigns								= false;
	public double								infectionPrice							= 1000.0D;
	public double								curePrice								= 500.0D;
	public double								silverSwordPrice						= 1000.0D;
	public double								wolfbanePrice							= 200.0D;
	public double								bookPrice								= 100.0D;
	public boolean								useClans								= true;
	public boolean								useUpdateNotifications					= true;
	public boolean								useScoreboards							= true;
	public boolean								metricsOptOut							= false;
	public int									transformsForNoDropItems				= 2;
	public int									transformsForControlledTransformation	= 6;
	public int									transformsForGoldImmunity				= 8;
	public int									transformationTimeoutSeconds			= 1800;
	public boolean								usePounce								= false;
	public float								pouncePlaneSpeed						= 1.0F;
	public float								pounceHeightSpeed						= 1.0F;
	public List<String>							allowedWorlds							= new ArrayList<String>();
	private static Werewolf						plugin;
	private static FileConfiguration			config									= null;
	public static PacketUtils pu										= null;
	private static LanguageManager languageManager							= null;
	// private static PotionManager potionManager = null;
	private static ClanManager clanManager								= null;
	private static SignManager					signManager								= null;
	private static WerewolfManager				werewolfManager							= null;
	private static HuntManager huntManager								= null;
	private static TrophyManager				trophyManager							= null;
	private static SkinManager					skinManager								= null;
	private static PermissionsManager			permissionsManager						= null;
	private static WerewolfScoreboardManager	werewolfScoreboardManager				= null;
	private static StatisticsManager			statisticsManager						= null;
	private static ItemManager itemManager								= null;

	public String								potionName								= "Witherfang";
	public String								werewolfBiteName						= "Bloodmoon";
	public String								wildBiteName							= "Silvermane";
	public String								alphaAccountName						= "WerewolfAlpha";
	public String								potionAccountName						= "WF_Werewolf";
	public String								werewolfBiteAccountName					= "SM_Werewolf";
	public String								wildBiteAccountName						= "BM_Werewolf";
	public String								alphaAccountUUID						= "e0d074bd-6722-47fc-95d3-f28e2899e155";
	public String								potionAccountUUID						= "c61647e5-fc2f-4536-abe9-c851911ad22f";
	public String								werewolfBiteAccountUUID					= "b68a8f00-7d24-4c52-b6ad-1423bfbe26ee";
	public String								wildBiteAccountUUID						= "da508ecc-dbd9-46c5-8095-47b91aa4ff5f";
	public String								alphaSkinValue							= "ewogICJ0aW1lc3RhbXAiIDogMTY4NTAzNzE3NjgzNiwKICAicHJvZmlsZUlkIiA6ICJlMGQwNzRiZDY3MjI0N2ZjOTVkM2YyOGUyODk5ZTE1NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJXZXJld29sZkFscGhhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2NhZTg4YjY5MjA0YWUyYzNlM2NiZTcwOWUzZTYyNTIxZTUzZmEwNWU0ZGFkODkxYzZhZmJkZmI5MDg2OGE3ZTAiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
	public String								alphaSkinSignature						= "GIDMFU5fTmB4T4RFFs/pQDAg7ZsCsu5KAxMXiQw4G32G0Db5SRmJ6OtiSc3SRznJO9EAW7lmjo38+BdY4b3cgTM7IbXmZGx5ML22ZW5KYspUoot2wo9kIj5ksh+H0Tfy9JCPLpg17nd7O1aY5+RhI5Aq+mhtyhJFXn1a906Qg9/XtHhiw0bUrthpa1RrtHnbH6+hFKgzNKAEeT93MvrPajOT7dmzQ72UK3KXgpe6KzbrWFf+NL0ZbpfkjwabSIALUdcL6+92EI//dB5yMgHEln9wfzKjH+82cDGEoGQjAnJIf+/KVWOtkfNRJauwpUkIUkD4tmkXxCCTyXXwUuIb7e7CwxyFWK4K7e7ir/P5zf1NuQZV+X1MwbF1c++dlGa4yBIvrb97F9tTu+7tw6KFwxqCvj4yRjhkGXsN0soI39v95q7ucxWaX0D4oKwa9QYi9zLW6DIh9bPxGa78TAPC/WyAdM/8J+/uinX2mvEsECsKQVmrCGRrqrZgfctsnXHuqstKMNOecwDFDzi8rWlcM3/wrdHn14Lvn2bsZGR4Xhr532CY2t+JgkQvYBM5Z+tkW5C7MCVH4BbLwf2zDcDkGIOzq5VoKlu/2WML88etjyzSIhCN0WJXWFkeRr7ddTar7Nx8/AzHRUBcTGXC+UGiHMYWWq8bAILFggLKY8eJzq0=";
	public String								potionSkinValue							= "ewogICJ0aW1lc3RhbXAiIDogMTY4NTAzNzM4MjA1NCwKICAicHJvZmlsZUlkIiA6ICJjNjE2NDdlNWZjMmY0NTM2YWJlOWM4NTE5MTFhZDIyZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJXRl9XZXJld29sZiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84NjA2YmNlMjkzZGRiY2RlNDE1NzE5NGJkMTZkZDhmZDQzNTAyMjU3MTUyYThiYTJiMWFmMTRlOTM3ZWY5NzAzIgogICAgfQogIH0KfQ==";
	public String								potionSkinSignature						= "M4bpafjzWq9GBbbHGL2xL8dwHxghLdily2bUnBsPPfsjrYidl8uws2pagluNPzxcCSqiZusJvbyoELgFBHrhKxDMGsUvG2nG/XV6GrZj8+bOYKlsU5IszwDaXJB9p6Uxh68pGoKximacJa1ELayKxbxLhJndGVTtO55pOvIB425f6VMRdJa6EvimsiibPMgGBKDVR4BP9ctP5Ij1cFyVpUZuiocp069LKPBMRR4mjRIeD4fmhz6o7yP5g4j6v3/n75T/NPR/NnMcRO7zYDnH0V0/a+dr0UTsGbH+4teuxdExgBrh2sjCCt7Zb4Yno6ElDYIABRqPUHSzNQ7IY3BHzGBaLZ29K/nUgN4mABlqj30ZL6RXmnlyzJUF/MxTcU0R/ROe8nmrt7eX700c29yrtufEkHhhUNX/hB/LjLIAoRIQgpHOGaocCgFsVRR1GGTot1KHuj8k0rvUKmxITpxbjOhdXwkwIMJ/WCAFygeBP6CTfnIKvZbJO2IAkv0/SQ8T38zLl2Xo0wbcUhQS4T44PWGzAVIX9I21lce9VHh55qTgQn0k+Wf9wog/KJw4HjLpWm+eTKuJiZH5zVGxQOzS0AU0OT+g/wHTcBqqtEDD1MeqhYfnQX7uU0wVN+HbahYV6Q2pE6qgyHCqjFTaS+N1dhmtTnhPsJdA4EhHCE9AAWE=";
	public String								werewolfBiteSkinValue					= "ewogICJ0aW1lc3RhbXAiIDogMTY4NTAzNzQwOTQzMiwKICAicHJvZmlsZUlkIiA6ICJiNjhhOGYwMDdkMjQ0YzUyYjZhZDE0MjNiZmJlMjZlZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTTV9XZXJld29sZiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82MzZhNWVkN2I0ZmZmM2I3MzU0YzRjOGM5NjBmM2QxOTVkZWZkMjgyMjA4NjYyNjY1ZGViYjQ2NGJiNWJmZTY1IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
	public String								werewolfBiteSkinSignature				= "RlCvrZ4SYkPDg2xl6f97ya5Z+578Uov0R6eHqJdFAPEB/rXqlwgPXS3bEhubQ3pLV9QS8BPE2OqycGrg7QrrQ7w267aurZ6Ax7RmGunGnuiJ9jcjlxjwp6Ltk4o/N8OUBYfF3TUfFXnj6lzZUeITZDSb21o/2UFglVxssk5U0PFN7/VpV4g6hv8iDBT9TH6v8wG8I4AZqgU2eoYGvddkLlCX/63FVEEbUb/eTc8R/iOyQVvnhaP8vhs1DXu7kp/b50hhDM31pDkMT3TJq2tEEHGmLZVok9aabtIuhRlIfpjyvJOV5VO9RDWiZeZqKQcYO1F7p+bv5gl8Cqodl7OrG8tzwjq2hH+tRMLidHCjgvsbHOLElUjWoBnMu10r+94Adl9+B/CPUF+NFqUwQ2jCY+WS75M/GX9C7gftO8f1c5n9eO8HVaUeB4fmPqAA0yoCUvIjVgr46a1imcppl2O1fKS+kIVI+Lb08Ujymilqe5lEnwOz/ykndAoleMM7+6kViHGF5/HkUS8l50AnJYVgIUPwLTlwY2GlvtSOcSEHTXCx9YsHBY60r4Ow3bi5pCkNvBTvaHqCWIHLuIA86r2GQPJGuZGn7iy1KJTlvDjZ2LhdfDktZpzmhfcAR8Iw2bnM9jeWKgg3gzEQgkROzQBvzONDVMqcL2ILXPKEdniypy8=";
	public String								wildBiteSkinValue						= "ewogICJ0aW1lc3RhbXAiIDogMTY4NTAzNzQzNDY4NywKICAicHJvZmlsZUlkIiA6ICJkYTUwOGVjY2RiZDk0NmM1ODA5NTQ3YjkxYWE0ZmY1ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJCTV9XZXJld29sZiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jMzc5MWEwOGM1Zjk1ZTQ1MGZkMmZiOGEwZTc1ZmViYTMwN2ZhZDJkNjYzMTkzNTAzYzU3NmMxOWRjM2MwYzdlIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
	public String								wildBiteSkinSignature					= "Z8NgxOC7mVyeW9hGKMxGKGG16ZDkADR5RXmyktriE6wUodq+TRokU0LIGaCHYrc6ReH3rzQJtNj5Whs83tLPAWGMj79silx6jzPhZ5Db2iTZQjVAG98rNGFBxgsjqSG72LyeyxwRIN6L/OSnf2qna2dP1T12d2uyfdipzuNdXnreG/5WqRj0Rrn3ok+GieuoR1DzYugkPu/3vhJOAFfvtSdoAv0C1JtuKaIeDj5BOm+RHvzwcFiYU9dhnGA0n3mxjGUpCCY/RFGF5l80Cw2RqN6j4rougUhklPe4swpS9KX2fF2TsK/JS8WJ1jHuKyykqnml2VHqsoNDX8yTCD/tTdXWJ+lTDsdkdCkPLbX2hkfIJ/0vv55GnQltERie757K+HHncmYw4tBxjgtTCcdIcLNNoAf9jlQC4f8sUCdPiIr7PKoxrik6GkZKx52DsaUGvv/+32E/Aswo1TnD0gMFOiWTtYfJ4OmMW5Ez+AtpoWnypAFKDtTIqMadhD/3HatlpCXsAuA49vE15dfN6WF+wJ66hCtxUKZJSqFhQ81Fan3R9+62E1f+7Vm8yyzoo/ttRmg7zFRAMTaf200z4Ei3xZvq0NiN8USdd3au4DAwK45Tn3FYc+Qo+9TVYn1iDEG9uo5BGYhxiYAz/TnVJccKOqaCzlu1KV/JxabEpAFd534=";

	public boolean								werewolfNamesEnabled					= true;
	public List<String>							givenNames								= Arrays.asList("Dark", "Black", "White", "Red", "Blood", "Blue", "Gray", "Neon", "Wild", "Lurking", "Feral", "Bestial", "Fierce", "Vicious", "Bloodthirsty", "Fearsome", "Ghoulish", "Wrathful", "Teenage", "Savage");
	public List<String>							surnames								= Arrays.asList("Devourer", "Fang", "Wolf", "Howl", "Turned", "Ghoul", "Paws", "Claw");

	private static Economy						economy									= null;
	private Commands commands								= null;
	private String								chatPrefix								= "Werewolf";
	public String								serverName								= "Your Server";

	public int									wolfbaneUntransformChance				= 25;
	public boolean								craftableInfectionPotionEnabled			= true;
	public boolean								craftableCurePotionEnabled				= true;
	public boolean								craftableWolfbanePotionEnabled			= true;
	public boolean								craftableSilverSwordEnabled				= true;
	public boolean								craftableSilverArmorEnabled				= true;
	public boolean								craftableLoreBookEnabled				= true;

	private Version								version									= null;
	public static final String					MAX										= "1.19.4-R0.1-SNAPSHOT";
	public static final String					MIN										= "1.13";
	public static final String					NMS										= VersionFactory.getNmsVersion().toString();
	private static boolean						isCompatible							= true;

	public static Werewolf instance()
	{
		return instance;
	}
	
	public static Class<?> isCombatibleServer() throws Exception
	{
		return Class.forName("net.minecraft.server." + NMS + ".ItemStack");
	}

	public static StatisticsManager getStatisticsManager()
	{
		return statisticsManager;
	}

	public static WerewolfScoreboardManager getWerewolfScoreboardManager()
	{
		return werewolfScoreboardManager;
	}

	public static ItemManager getItemManager()
	{
		return itemManager;
	}

	public static SignManager getSignManager()
	{
		return signManager;
	}

	public static LanguageManager getLanguageManager()
	{
		return languageManager;
	}

	/*
	 * public static PotionManager getPotionManager() { return potionManager; }
	 */

	public static PermissionsManager getPermissionsManager()
	{
		return permissionsManager;
	}

	public static ClanManager getClanManager()
	{
		return clanManager;
	}

	public static WerewolfManager getWerewolfManager()
	{
		return werewolfManager;
	}

	public static HuntManager getHuntManager()
	{
		return huntManager;
	}

	public static TrophyManager getTrophyManager()
	{
		return trophyManager;
	}

	public static SkinManager getSkinManager()
	{
		return skinManager;
	}

	public static Economy getEconomy()
	{
		return economy;
	}

	public String getChatPrefix()
	{
		return chatPrefix;
	}

	public void announcementMessage(World world, String messageText, Sound sound, long delay)
	{
		server.getScheduler().runTaskLater(plugin, new CentralMessageTask(world, messageText, sound), delay);
	}

	public void disguiseWerewolf(Player p)
	{
		server.getScheduler().runTaskLater(plugin, new DisguiseTask(plugin, p), 1L);
	}

	public void undisguiseWerewolf(UUID playerId, boolean makeVisible, boolean forever)
	{
		OfflinePlayer player = getServer().getOfflinePlayer(playerId);

		if (player != null)
		{
			server.getScheduler().runTaskLater(plugin, new UndisguiseTask(plugin, playerId, makeVisible, forever), 8L);
		}
		else
		{
			server.getScheduler().runTaskLater(plugin, new UndisguiseTask(plugin, playerId, makeVisible, forever), 8L);
		}
	}

	public void sendInfo(Player player, String message)
	{
		if (player == null)
		{
			log(message);
		}
		else
		{
			player.sendMessage(message);
		}
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, String name, int amount, int delay)
	{
		if (playerId == null)
		{
			return;
		}

		Player player = getServer().getPlayer(playerId);
		if (player == null)
		{
			logDebug("sendInfo can not find online player with id " + playerId);
			return;
		}

		getServer().getScheduler().runTaskLater(this, new com.dogonfire.werewolf.tasks.InfoTask(this, color, playerId, message, amount, name), delay);
	}

	public static boolean isCompatible()
	{
		return isCompatible;
	}

	public void onDisable()
	{
		if (vaultEnabled)
		{
			CompassTrackerTask.stop();
		}

		for (Player player : getServer().getOnlinePlayers())
		{
			if (getWerewolfManager().isWerewolf(player) && getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
			{
				untransform(player);
			}
		}

		saveSettings();
		reloadSettings();

		pluginEnabled = false;
	}

	@Override
	public void onEnable()
	{
		DamageListener damageListener = null;
		PlayerListener playerListener = null;
		InteractListener interactListener = null;
		InventoryListener inventoryListener = null;
		ChatListener chatListener = null;
		
		plugin = this;
		instance = this;
		server = getServer();
		config = getConfig();
		version = VersionFactory.getServerVersion();

		this.commands = new Commands(this);

		if (!version.isCompatible(MIN))
		{
			log(ChatColor.RED + "* Werewolf is not compatible with your server");
			log(ChatColor.RED + "* Your server version is " + ChatColor.GOLD + version.toString());
			log(ChatColor.RED + "* The minimum supported version for this plugin is " + ChatColor.GOLD + MIN);
			log(ChatColor.RED + "* Werewolves are now disabled.");

			pluginEnabled = false;
			isCompatible = false;
			// this.getServer().getPluginManager().disablePlugin(this);

			return;
		}

		if (!version.isSupported(MAX))
		{
			log(ChatColor.RED + "* Werewolf is not supported and tested with your server version!");
			log(ChatColor.RED + "* Your server version is " + ChatColor.GOLD + version.toString());
			log(ChatColor.RED + "* The maximum supported version for this plugin is " + ChatColor.GOLD + MAX);
			log(ChatColor.RED + "* You can still report bugs to https://github.com/DogOnFire/Werewolf/issues.");

			isCompatible = false;
		}

		pluginEnabled = true;

		werewolfManager = new WerewolfManager(this);
		clanManager = new ClanManager(this);
		// potionManager = new PotionManager(this);
		languageManager = new LanguageManager(this);
		statisticsManager = new StatisticsManager();
		itemManager = new ItemManager(this);		
		damageListener = new DamageListener(this);
		playerListener = new PlayerListener(this);
		interactListener = new InteractListener(this);
		chatListener = new ChatListener(this);
		this.disguisesEnabled = true;
		skinManager = new SkinManager(this);
		
		if(!WerewolfDisguiseAPI.init())
		{
			this.disguisesEnabled = false;
		}
		
		// Pre-cache some known disguises
		WerewolfDisguiseAPI.getDisguise(UUID.fromString(potionAccountUUID), potionAccountName);
		WerewolfDisguiseAPI.getDisguise(UUID.fromString(wildBiteAccountUUID), wildBiteAccountName);
		WerewolfDisguiseAPI.getDisguise(UUID.fromString(werewolfBiteAccountUUID), werewolfBiteAccountName);
		WerewolfDisguiseAPI.getDisguise(UUID.fromString(alphaAccountUUID), alphaAccountName);
				
		// If ! prevent armor
		if (this.dropArmorOnTransform)
		{
			inventoryListener = new InventoryListener(this);
		}

		pu = new PacketUtils();

		PluginManager pm = getServer().getPluginManager();

		// Check for Vault
		if (pm.getPlugin("Vault") != null && pm.getPlugin("Vault").isEnabled())
		{
			this.vaultEnabled = true;
			huntManager = new HuntManager(this);

			log("Vault detected. Bounties and sign economy are enabled!");

			CompassTrackerTask.setPlugin(this);
			CompassTrackerTask.setUpdateRate(this.compassUpdateRate);

			RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
			if (economyProvider != null)
			{
				economy = economyProvider.getProvider();
			}
			else
			{
				plugin.log("Vault not found.");
			}
		}
		else
		{
			log("Vault not found. Werewolf bounties and signs are disabled.");
		}

		permissionsManager = new PermissionsManager(this);

		if (pm.getPlugin("Vampire") != null)
		{
			log("Vampire plugin detected. Enabling support for vampirism :-)");

			this.vampireEnabled = true;
		}

		// Check for HealthBar
		if (pm.getPlugin("HealthBar") != null && pm.getPlugin("HealthBar").isEnabled())
		{
			log("HealthBar plugin detected. Enabling support for healthbars.");

			this.healthBarEnabled = true;
		}

		// Check for PlaceholderAPI
		if (pm.getPlugin("PlaceholderAPI") != null && pm.getPlugin("PlaceholderAPI").isEnabled())
		{
			new WerewolfPlaceholderExpansion(this).register();
		}

		getServer().getPluginManager().registerEvents(playerListener, this);
		getServer().getPluginManager().registerEvents(interactListener, this);
		getServer().getPluginManager().registerEvents(damageListener, this);
		getServer().getPluginManager().registerEvents(chatListener, this);
		if (this.dropArmorOnTransform)
		{
			getServer().getPluginManager().registerEvents(inventoryListener, this);
		}

		loadSettings();
		saveSettings();

		permissionsManager.load();
		werewolfManager.load();
		clanManager.load();
		languageManager.load();

		itemManager.setupRecipes();

		if (this.vaultEnabled)
		{
			huntManager.load();
		}

		if (this.useScoreboards)
		{
			Werewolf.werewolfScoreboardManager = new WerewolfScoreboardManager();
		}

		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		{
			public void run()
			{
				Werewolf.getWerewolfManager().update();
			}
		}, 20L, 100L);

		if (this.useClans)
		{
			getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
			{
				public void run()
				{
					Werewolf.getClanManager().updateClans();
				}
			}, 20L, 72000L);
		}

		if (!this.metricsOptOut)
		{
			startMetrics();
		}
	}

	public boolean isWerewolvesAllowedInWorld(Player player)
	{
		return this.allowedWorlds.contains(player.getWorld().getName());
	}

	public boolean isUnderOpenSky(Player player)
	{
		return player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getLocation().getBlockY();
	}

	public boolean isFullMoonDuskInWorld(World world)
	{
		long time = world.getFullTime() % 24000L;

		return (time > 10000L) && (time < 14000L) && (moonCheck(world) == MoonPhase.FullMoon);
	}

	public boolean isNightInWorld(World world)
	{
		long time = world.getFullTime() % 24000L;

		return (time > getTimeStart()) && (time < getTimeEnd());
	}

	public boolean isFullMoonInWorld(World world)
	{
		if (plugin.onlyTransformDuringFullMoon)
		{
			return (isNightInWorld(world)) && (moonCheck(world) == MoonPhase.FullMoon);
		}
		return isNightInWorld(world);
	}

	public static MoonPhase getMoonPhaseByInt(int I)
	{
		return MoonPhase.values()[I];
	}

	public MoonPhase moonCheck(World world)
	{
		long T = world.getFullTime();
		long D = T / 24000L;
		int days = (int) D;
		int phaseInt = days % 8;
		return getMoonPhaseByInt(phaseInt);
	}

	public String getNextFullMoonText(World world)
	{
		long T = world.getFullTime();
		long D = T / 24000L;
		int days = (int) D;
		int phaseInt = days % 8;
		switch (phaseInt)
		{
		case 0:
			return getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.Today, ChatColor.GOLD);
		case 1:
			return getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In7Days, ChatColor.GOLD);
		case 2:
			return getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In6Days, ChatColor.GOLD);
		case 3:
			return getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In5Days, ChatColor.GOLD);
		case 4:
			return getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In4Days, ChatColor.GOLD);
		case 5:
			return getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In3Days, ChatColor.GOLD);
		case 6:
			return getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.In2Days, ChatColor.GOLD);
		case 7:
			return getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.Tomorrow, ChatColor.GOLD);
		default:
			return "WTF?";
		}
	}

	public static enum MoonPhase {
		FullMoon, WaningGibbous, LastQuarter, WaningCrescent, NewMoon, WaxingCrescent, FirstQuarter, WaxingGibbous;
	}

	public boolean hasTransformation()
	{
		return true;
	}

	public int getTimeStart()
	{
		return nightStart;
	}

	public int getTimeEnd()
	{
		return nightEnd;
	}

	public void transform(Player player)
	{
		if (!isWerewolvesAllowedInWorld(player))
		{
			return;
		}

		if (!getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			player.getLocation().getWorld().playEffect(player.getLocation(), Effect.SMOKE, 100);
			player.getLocation().getWorld().playEffect(player.getLocation().add(new Vector(0, 1, 0)), Effect.SMOKE, 100);
			player.getLocation().getWorld().playEffect(player.getLocation().add(new Vector(0, 2, 0)), Effect.SMOKE, 100);
			werewolfManager.setWerewolfSkin(player);
		}
		else
		{
			plugin.log("Could not transform " + player.getName() + ": Not a werewolf!");
		}
	}

	public void untransform(Player player)
	{
		player.getLocation().getWorld().playEffect(player.getLocation(), Effect.SMOKE, 100);
		player.getLocation().getWorld().playEffect(player.getLocation().add(new Vector(0, 1, 0)), Effect.SMOKE, 100);
		player.getLocation().getWorld().playEffect(player.getLocation().add(new Vector(0, 2, 0)), Effect.SMOKE, 100);

		werewolfManager.unsetWerewolfSkin(player.getUniqueId(), true);
	}

	/*public void setPositionUpdater(Player player, WerewolfDisguise skin)
	{
		if (this.movementUpdateThreading)
		{
			this.positionUpdaters.put(player, Integer.valueOf(getServer().getScheduler().scheduleSyncRepeatingTask(this, new PlayerPositionUpdater(this, player, skin), 1L, this.movementUpdateFrequency)));
		}
	}
	*/

	public void log(String message)
	{
		plugin.getLogger().info(message);
	}

	public void logDebug(String message)
	{
		if (this.debug)
		{
			plugin.getLogger().info("[Debug] " + message);
		}
	}

	public void reloadSettings()
	{
		reloadConfig();
		loadSettings();

		getWerewolfManager().load();
		getClanManager().load();
		getLanguageManager().load();
	}

	public void loadSettings()
	{
		config = getConfig();

		this.debug = config.getBoolean("Settings.Debug", false);

		DamageManager.werewolfItemDamage = config.getInt("WerewolfWolf.ItemDamage", 3);
		DamageManager.werewolfHandDamage = config.getInt("WerewolfWolf.HandDamage", 8);
		DamageManager.SilverArmorMultiplier = config.getDouble("WerewolfWolf.ArmorMultiplier", 0.8D);
		this.wolfdistance = config.getInt("WerewolfWolf.WolfDistance", 10);

		this.cureChance = config.getDouble("Infection.CurePotionChance", 1.0D);

		this.werewolfInfectionRisk = config.getDouble("Infection.WerewolfBiteRisk", 0.05D);
		this.wildWolfInfectionRisk = config.getDouble("Infection.WildWolfBiteRisk", 0.75D);

		this.autoCureDays = config.getInt("Infection.AutoCureDays", 14);

		this.allowedWorlds = config.getStringList("AllowedWorlds");
		if (this.allowedWorlds.size() == 0)
		{
			log("Allowed worlds is empty. Adding world " + getServer().getWorlds().get(0).getName() + " as werewolf world.");
			this.allowedWorlds.add(getServer().getWorlds().get(0).getName());
		}
		else
		{
			for (String worldName : this.allowedWorlds)
			{
				log("Werewolves are allowed in worlds '" + worldName + "'");
			}
		}
		//
		// DamageListener.WEREWOLF_GROWL = config.getString("Files.Growl", "");

		nightStart = config.getInt("Night.Start", 13000);
		nightEnd = config.getInt("Night.End", 23000);

		this.wolfMessage.add("*Grunt*");
		this.wolfMessage.add("*Grunt* *Grrrr*");
		this.wolfMessage.add("*Grunt* *Grunt*");
		this.wolfMessage.add("*Growl*");
		this.wolfMessage.add("*Grrroowl Grunt*");
		this.wolfMessage.add("*Grrrrr*");
		this.wolfMessage.add("*Rrrrrr*");
		this.wolfMessage.add("*Groooowl*");
		this.wolfMessage.add("*Grrrrr* *Grrr*");
		this.wolfMessage.add("*Hoooowl*");
		this.wolfMessage.add("*Rrraagh*");
		this.wolfMessage.add("*Grrawl*");
		this.wolfMessage.add("*Grrrrawl* *Growls*");
		this.wolfMessage.add("*HOOOOOWLLLL!*");
		this.wolfMessage.add("*Wimper*");
		this.wolfMessage.add("*Awooooo*");

		this.werewolfGroupName = config.getString("WerewolfGroup.Name", "Werewolf");
		this.useWerewolfGroupName = config.getBoolean("WerewolfGroup.Enabled", false);
		this.useTrophies = config.getBoolean("Trophies.Enabled", false);
		if (this.useTrophies)
		{
			trophyManager = new TrophyManager(this);
		}

		this.autoBounty = config.getBoolean("Settings.AutoBounty", false);
		this.autoBountyMaximum = config.getInt("Settings.AutoBountyMaximum", 1000);
		this.renameWerewolves = config.getBoolean("Settings.RenameWerewolves", true);
		this.useUpdateNotifications = config.getBoolean("Settings.DisplayUpdateNotifications", true);
		this.metricsOptOut = config.getBoolean("Settings.MetricsOptOut", false);
		this.werewolfUrges = config.getBoolean("Settings.WerewolfUrges", true);
		this.wolfChat = config.getBoolean("Settings.WolfChat", true);
		this.dropArmorOnTransform = config.getBoolean("Settings.DropArmorOnTransform", true);
		this.onlyTransformDuringFullMoon = config.getBoolean("Settings.OnlyTransformDuringFullMoon", true);
		this.serverName = config.getString("Settings.ServerName", "Your Server");
		this.cureWerewolfWhenSlain = config.getBoolean("Settings.CureWerewolfWhenSlain", false);

		this.useSigns = config.getBoolean("Signs.Enabled", false);
		if (this.useSigns)
		{
			signManager = new SignManager(this);
			getServer().getPluginManager().registerEvents(signManager, this);
		}

		this.curePrice = config.getDouble("Signs.CurePrice", 500.0D);
		this.infectionPrice = config.getDouble("Signs.InfectionPrice", 1000.0D);
		this.bookPrice = config.getDouble("Signs.BookPrice", 100.0D);
		this.wolfbanePrice = config.getDouble("Signs.WolfbanePrice", 250.0D);
		this.silverSwordPrice = config.getDouble("Signs.SilverSwordPrice", 1000.0D);

		this.language = config.getString("Settings.Language", "english");
		if (!this.supportedLanguages.contains(this.language))
		{
			log("Language '" + this.language + "' is not supported. Reverting to english.");
			this.language = "english";
		}
		this.transformsForNoDropItems = config.getInt("Maturity.NoDropItems", 3);

		this.transformsForControlledTransformation = config.getInt("Maturity.ControlledTransformation", 7);
		this.transformsForGoldImmunity = config.getInt("Maturity.GoldImmunity", 10);

		/*
		 * this.usePounce = config.getBoolean("Pounce.Enabled", false);
		 * this.pouncePlaneSpeed = ((float)
		 * config.getDouble("Pounce.PlaneSpeed", 2.25D)); this.pounceHeightSpeed
		 * = ((float) config.getDouble("Pounce.HeightSpeed", 1.1D)); if
		 * (this.useTrophies && (trophyManager == null)) { trophyManager = new
		 * TrophyManager(this); trophyManager.load(); }
		 */

		this.chatPrefix = config.getString("Chat.Prefix", "Werewolf");

		this.useScoreboards = config.getBoolean("Scoreboards.Enabled", true);

		this.useClans = config.getBoolean("Clans.Enabled", true);
		this.potionName = config.getString("Clans.PotionName", "Witherfang");
		this.werewolfBiteName = config.getString("Clans.WerewolfBiteName", "Bloodmoon");
		this.wildBiteName = config.getString("Clans.WildBiteName", "Silvermane");
		this.alphaAccountName = config.getString("Clans.AlphaAccountName", "WerewolfAlpha");
		this.potionAccountName = config.getString("Clans.PotionAccountName", "WF_Werewolf");
		this.werewolfBiteAccountName = config.getString("Clans.WerewolfBiteAccountName", "SM_Werewolf");
		this.wildBiteAccountName = config.getString("Clans.WildBiteAccountName", "BM_Werewolf");
		this.alphaAccountUUID = config.getString("Clans.AlphaAccountUUID", "e0d074bd-6722-47fc-95d3-f28e2899e155");
		this.potionAccountUUID = config.getString("Clans.PotionAccountUUID", "c61647e5-fc2f-4536-abe9-c851911ad22f");
		this.werewolfBiteAccountUUID = config.getString("Clans.WerewolfBiteAccountUUID", "b68a8f00-7d24-4c52-b6ad-1423bfbe26ee");
		this.wildBiteAccountUUID = config.getString("Clans.WildBiteAccountUUID", "da508ecc-dbd9-46c5-8095-47b91aa4ff5f");
		this.alphaSkinValue = config.getString("Clans.AlphaSkinValue", "ewogICJ0aW1lc3RhbXAiIDogMTY4NTAzNzE3NjgzNiwKICAicHJvZmlsZUlkIiA6ICJlMGQwNzRiZDY3MjI0N2ZjOTVkM2YyOGUyODk5ZTE1NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJXZXJld29sZkFscGhhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2NhZTg4YjY5MjA0YWUyYzNlM2NiZTcwOWUzZTYyNTIxZTUzZmEwNWU0ZGFkODkxYzZhZmJkZmI5MDg2OGE3ZTAiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==");
		this.alphaSkinSignature = config.getString("Clans.AlphaSkinSignature", "GIDMFU5fTmB4T4RFFs/pQDAg7ZsCsu5KAxMXiQw4G32G0Db5SRmJ6OtiSc3SRznJO9EAW7lmjo38+BdY4b3cgTM7IbXmZGx5ML22ZW5KYspUoot2wo9kIj5ksh+H0Tfy9JCPLpg17nd7O1aY5+RhI5Aq+mhtyhJFXn1a906Qg9/XtHhiw0bUrthpa1RrtHnbH6+hFKgzNKAEeT93MvrPajOT7dmzQ72UK3KXgpe6KzbrWFf+NL0ZbpfkjwabSIALUdcL6+92EI//dB5yMgHEln9wfzKjH+82cDGEoGQjAnJIf+/KVWOtkfNRJauwpUkIUkD4tmkXxCCTyXXwUuIb7e7CwxyFWK4K7e7ir/P5zf1NuQZV+X1MwbF1c++dlGa4yBIvrb97F9tTu+7tw6KFwxqCvj4yRjhkGXsN0soI39v95q7ucxWaX0D4oKwa9QYi9zLW6DIh9bPxGa78TAPC/WyAdM/8J+/uinX2mvEsECsKQVmrCGRrqrZgfctsnXHuqstKMNOecwDFDzi8rWlcM3/wrdHn14Lvn2bsZGR4Xhr532CY2t+JgkQvYBM5Z+tkW5C7MCVH4BbLwf2zDcDkGIOzq5VoKlu/2WML88etjyzSIhCN0WJXWFkeRr7ddTar7Nx8/AzHRUBcTGXC+UGiHMYWWq8bAILFggLKY8eJzq0=");
		this.potionSkinValue = config.getString("Clans.PotionSkinValue", "ewogICJ0aW1lc3RhbXAiIDogMTY4NTAzNzM4MjA1NCwKICAicHJvZmlsZUlkIiA6ICJjNjE2NDdlNWZjMmY0NTM2YWJlOWM4NTE5MTFhZDIyZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJXRl9XZXJld29sZiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84NjA2YmNlMjkzZGRiY2RlNDE1NzE5NGJkMTZkZDhmZDQzNTAyMjU3MTUyYThiYTJiMWFmMTRlOTM3ZWY5NzAzIgogICAgfQogIH0KfQ==");
		this.potionSkinSignature = config.getString("Clans.PotionSkinSignature", "M4bpafjzWq9GBbbHGL2xL8dwHxghLdily2bUnBsPPfsjrYidl8uws2pagluNPzxcCSqiZusJvbyoELgFBHrhKxDMGsUvG2nG/XV6GrZj8+bOYKlsU5IszwDaXJB9p6Uxh68pGoKximacJa1ELayKxbxLhJndGVTtO55pOvIB425f6VMRdJa6EvimsiibPMgGBKDVR4BP9ctP5Ij1cFyVpUZuiocp069LKPBMRR4mjRIeD4fmhz6o7yP5g4j6v3/n75T/NPR/NnMcRO7zYDnH0V0/a+dr0UTsGbH+4teuxdExgBrh2sjCCt7Zb4Yno6ElDYIABRqPUHSzNQ7IY3BHzGBaLZ29K/nUgN4mABlqj30ZL6RXmnlyzJUF/MxTcU0R/ROe8nmrt7eX700c29yrtufEkHhhUNX/hB/LjLIAoRIQgpHOGaocCgFsVRR1GGTot1KHuj8k0rvUKmxITpxbjOhdXwkwIMJ/WCAFygeBP6CTfnIKvZbJO2IAkv0/SQ8T38zLl2Xo0wbcUhQS4T44PWGzAVIX9I21lce9VHh55qTgQn0k+Wf9wog/KJw4HjLpWm+eTKuJiZH5zVGxQOzS0AU0OT+g/wHTcBqqtEDD1MeqhYfnQX7uU0wVN+HbahYV6Q2pE6qgyHCqjFTaS+N1dhmtTnhPsJdA4EhHCE9AAWE=");
		this.werewolfBiteSkinValue = config.getString("Clans.WerewolfBiteSkinValue", "ewogICJ0aW1lc3RhbXAiIDogMTY4NTAzNzQwOTQzMiwKICAicHJvZmlsZUlkIiA6ICJiNjhhOGYwMDdkMjQ0YzUyYjZhZDE0MjNiZmJlMjZlZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTTV9XZXJld29sZiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82MzZhNWVkN2I0ZmZmM2I3MzU0YzRjOGM5NjBmM2QxOTVkZWZkMjgyMjA4NjYyNjY1ZGViYjQ2NGJiNWJmZTY1IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=");
		this.werewolfBiteSkinSignature = config.getString("Clans.WerewolfBiteSkinSignature", "RlCvrZ4SYkPDg2xl6f97ya5Z+578Uov0R6eHqJdFAPEB/rXqlwgPXS3bEhubQ3pLV9QS8BPE2OqycGrg7QrrQ7w267aurZ6Ax7RmGunGnuiJ9jcjlxjwp6Ltk4o/N8OUBYfF3TUfFXnj6lzZUeITZDSb21o/2UFglVxssk5U0PFN7/VpV4g6hv8iDBT9TH6v8wG8I4AZqgU2eoYGvddkLlCX/63FVEEbUb/eTc8R/iOyQVvnhaP8vhs1DXu7kp/b50hhDM31pDkMT3TJq2tEEHGmLZVok9aabtIuhRlIfpjyvJOV5VO9RDWiZeZqKQcYO1F7p+bv5gl8Cqodl7OrG8tzwjq2hH+tRMLidHCjgvsbHOLElUjWoBnMu10r+94Adl9+B/CPUF+NFqUwQ2jCY+WS75M/GX9C7gftO8f1c5n9eO8HVaUeB4fmPqAA0yoCUvIjVgr46a1imcppl2O1fKS+kIVI+Lb08Ujymilqe5lEnwOz/ykndAoleMM7+6kViHGF5/HkUS8l50AnJYVgIUPwLTlwY2GlvtSOcSEHTXCx9YsHBY60r4Ow3bi5pCkNvBTvaHqCWIHLuIA86r2GQPJGuZGn7iy1KJTlvDjZ2LhdfDktZpzmhfcAR8Iw2bnM9jeWKgg3gzEQgkROzQBvzONDVMqcL2ILXPKEdniypy8=");
		this.wildBiteSkinValue = config.getString("Clans.WildBiteSkinValue", "ewogICJ0aW1lc3RhbXAiIDogMTY4NTAzNzQzNDY4NywKICAicHJvZmlsZUlkIiA6ICJkYTUwOGVjY2RiZDk0NmM1ODA5NTQ3YjkxYWE0ZmY1ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJCTV9XZXJld29sZiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jMzc5MWEwOGM1Zjk1ZTQ1MGZkMmZiOGEwZTc1ZmViYTMwN2ZhZDJkNjYzMTkzNTAzYzU3NmMxOWRjM2MwYzdlIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=");
		this.wildBiteSkinSignature = config.getString("Clans.WildBiteSkinSignature", "Z8NgxOC7mVyeW9hGKMxGKGG16ZDkADR5RXmyktriE6wUodq+TRokU0LIGaCHYrc6ReH3rzQJtNj5Whs83tLPAWGMj79silx6jzPhZ5Db2iTZQjVAG98rNGFBxgsjqSG72LyeyxwRIN6L/OSnf2qna2dP1T12d2uyfdipzuNdXnreG/5WqRj0Rrn3ok+GieuoR1DzYugkPu/3vhJOAFfvtSdoAv0C1JtuKaIeDj5BOm+RHvzwcFiYU9dhnGA0n3mxjGUpCCY/RFGF5l80Cw2RqN6j4rougUhklPe4swpS9KX2fF2TsK/JS8WJ1jHuKyykqnml2VHqsoNDX8yTCD/tTdXWJ+lTDsdkdCkPLbX2hkfIJ/0vv55GnQltERie757K+HHncmYw4tBxjgtTCcdIcLNNoAf9jlQC4f8sUCdPiIr7PKoxrik6GkZKx52DsaUGvv/+32E/Aswo1TnD0gMFOiWTtYfJ4OmMW5Ez+AtpoWnypAFKDtTIqMadhD/3HatlpCXsAuA49vE15dfN6WF+wJ66hCtxUKZJSqFhQ81Fan3R9+62E1f+7Vm8yyzoo/ttRmg7zFRAMTaf200z4Ei3xZvq0NiN8USdd3au4DAwK45Tn3FYc+Qo+9TVYn1iDEG9uo5BGYhxiYAz/TnVJccKOqaCzlu1KV/JxabEpAFd534=");

		this.werewolfNamesEnabled = config.getBoolean("WerewolfNames.Enabled", true);
		if (config.contains("WerewolfNames.Given_names"))
		{
			this.givenNames = config.getStringList("WerewolfNames.Given_names");
		}

		if (config.contains("WerewolfNames.Surnames"))
		{
			this.surnames = config.getStringList("WerewolfNames.Surnames");
		}

		DamageManager.SilverSwordMultiplier = config.getInt("Items.SilverSwordMultiplier", 2);
		this.wolfbaneUntransformChance = config.getInt("Items.WolfbaneUntransformChance", 25);
		this.craftableSilverSwordEnabled = config.getBoolean("Items.CraftableSilverSword", true);
		this.craftableSilverArmorEnabled = config.getBoolean("Items.CraftableSilverArmor", true);
		this.craftableLoreBookEnabled = config.getBoolean("Items.CraftableLoreBookEnabled", true);
		this.craftableInfectionPotionEnabled = config.getBoolean("Items.CraftableInfectionPotionEnabled", true);
		this.craftableCurePotionEnabled = config.getBoolean("Items.CraftableCurePotionEnabled", true);
		this.craftableWolfbanePotionEnabled = config.getBoolean("Items.CraftableWolfbanePotionEnabled", true);
	}

	public void saveSettings()
	{
		config.set("Settings.ServerName", this.serverName);
		config.set("Settings.DisplayUpdateNotifications", Boolean.valueOf(this.useUpdateNotifications));
		config.set("Settings.MetricsOptOut", Boolean.valueOf(this.metricsOptOut));
		config.set("Settings.AutoBounty", Boolean.valueOf(this.autoBounty));
		config.set("Settings.AutoBountyMaximum", Integer.valueOf(this.autoBountyMaximum));
		config.set("Settings.RenameWerewolves", Boolean.valueOf(this.renameWerewolves));
		config.set("Settings.WerewolfUrges", Boolean.valueOf(this.werewolfUrges));
		config.set("Settings.Language", this.language);
		config.set("Settings.WolfChat", Boolean.valueOf(this.wolfChat));
		config.set("Settings.DropArmorOnTransform", Boolean.valueOf(this.dropArmorOnTransform));
		config.set("Settings.OnlyTransformDuringFullMoon", Boolean.valueOf(this.onlyTransformDuringFullMoon));
		config.set("Settings.CureWerewolfWhenSlain", Boolean.valueOf(this.cureWerewolfWhenSlain));

		config.set("Maturity.NoDropItems", Integer.valueOf(this.transformsForNoDropItems));
		config.set("Maturity.ControlledTransformation", Integer.valueOf(this.transformsForControlledTransformation));
		config.set("Maturity.GoldImmunity", Integer.valueOf(this.transformsForGoldImmunity));

		config.set("Settings.Debug", Boolean.valueOf(this.debug));

		config.set("Infection.CureChance", Double.valueOf(this.cureChance));
		config.set("Infection.WerewolfBiteRisk", Double.valueOf(this.werewolfInfectionRisk));
		config.set("Infection.WildWolfBiteRisk", Double.valueOf(this.wildWolfInfectionRisk));
		config.set("Infection.AutoCureDays", Integer.valueOf(this.autoCureDays));

		config.set("Werewolf.HandDamage", Double.valueOf(DamageManager.werewolfHandDamage));
		config.set("Werewolf.ItemDamage", Double.valueOf(DamageManager.werewolfItemDamage));
		config.set("Werewolf.ArmorMultiplier", Double.valueOf(DamageManager.SilverArmorMultiplier));
		config.set("Werewolf.WolfDistance", Integer.valueOf(this.wolfdistance));

		config.set("WerewolfGroup.Enabled", Boolean.valueOf(this.useWerewolfGroupName));
		config.set("WerewolfGroup.Name", this.werewolfGroupName);

		config.set("Pounce.Enabled", Boolean.valueOf(this.usePounce));
		config.set("Pounce.PlaneSpeed", Float.valueOf(this.pouncePlaneSpeed));
		config.set("Pounce.HeightSpeed", Float.valueOf(this.pounceHeightSpeed));

		config.set("AllowedWorlds", this.allowedWorlds);

		config.set("Night.Start", Integer.valueOf(nightStart));
		config.set("Night.End", Integer.valueOf(nightEnd));

		config.set("Clans.Enabled", Boolean.valueOf(this.useClans));
		config.set("Clans.PotionName", this.potionName);
		config.set("Clans.WerewolfBiteName", this.werewolfBiteName);
		config.set("Clans.WildBiteName", this.wildBiteName);
		config.set("Clans.AlphaAccountName", this.alphaAccountName);
		config.set("Clans.PotionAccountName", this.potionAccountName);
		config.set("Clans.WerewolfBiteAccountName", this.werewolfBiteAccountName);
		config.set("Clans.WildBiteAccountName", this.wildBiteAccountName);
		config.set("Clans.AlphaAccountUUID", this.alphaAccountUUID);
		config.set("Clans.PotionAccountUUID", this.potionAccountUUID);
		config.set("Clans.WerewolfBiteAccountUUID", this.werewolfBiteAccountUUID);
		config.set("Clans.WildBiteAccountUUID", this.wildBiteAccountUUID);
		config.set("Clans.AlphaSkinValue", this.alphaSkinValue);
		config.set("Clans.AlphaSkinSignature", this.alphaSkinSignature);
		config.set("Clans.PotionSkinValue", this.potionSkinValue);
		config.set("Clans.PotionSkinSignature", this.potionSkinSignature);
		config.set("Clans.WerewolfBiteSkinValue", this.werewolfBiteSkinValue);
		config.set("Clans.WerewolfBiteSkinSignature", this.werewolfBiteSkinSignature);
		config.set("Clans.WildBiteSkinValue", this.wildBiteSkinValue);
		config.set("Clans.WildBiteSkinSignature", this.wildBiteSkinSignature);

		config.set("WerewolfNames.Enabled", this.werewolfNamesEnabled);
		config.set("WerewolfNames.Given_names", this.givenNames);
		config.set("WerewolfNames.Surnames", this.surnames);

		config.set("Signs.Enabled", Boolean.valueOf(this.useSigns));
		config.set("Signs.CurePrice", Double.valueOf(this.curePrice));
		config.set("Signs.InfectionPrice", Double.valueOf(this.infectionPrice));
		config.set("Signs.WolfbanePrice", Double.valueOf(this.wolfbanePrice));
		config.set("Signs.SilverSwordPrice", Double.valueOf(this.silverSwordPrice));
		config.set("Signs.BookPrice", Double.valueOf(this.bookPrice));

		config.set("Trophies.Enabled", Boolean.valueOf(this.useTrophies));

		config.set("Scoreboards.Enabled", Boolean.valueOf(this.useScoreboards));

		config.set("Chat.Prefix", this.chatPrefix);

		config.set("Items.Enabled", true);
		config.set("Items.SilverSwordMultiplier", Double.valueOf(DamageManager.SilverSwordMultiplier));
		config.set("Items.WolfbaneUntransformChance", this.wolfbaneUntransformChance);
		config.set("Items.CraftableSilverSword", Boolean.valueOf(this.craftableSilverSwordEnabled));
		config.set("Items.CraftableSilverArmor", Boolean.valueOf(this.craftableSilverArmorEnabled));
		config.set("Items.CraftableLoreBookEnabled", Boolean.valueOf(this.craftableLoreBookEnabled));
		config.set("Items.CraftableInfectionPotionEnabled", Boolean.valueOf(this.craftableInfectionPotionEnabled));
		config.set("Items.CraftableCurePotionEnabled", Boolean.valueOf(this.craftableCurePotionEnabled));
		config.set("Items.CraftableWolfbanePotionEnabled", Boolean.valueOf(this.craftableWolfbanePotionEnabled));
		/*
		 * DamageManager.SilverSwordMultiplier =
		 * config.getInt("Items.SilverSwordMultiplier", 2);
		 * this.craftableSilverSwordEnabled =
		 * config.getBoolean("Items.CraftableSilverSword", true);
		 * this.craftableLoreBookEnabled =
		 * config.getBoolean("Items.CraftableLoreBookEnabled", true);
		 * this.craftableInfectionPotionEnabled =
		 * config.getBoolean("Items.CraftableInfectionPotionEnabled", true);
		 * this.craftableCurePotionEnabled =
		 * config.getBoolean("Items.CraftableCurePotionEnabled", true);
		 * this.craftableWolfbanePotionEnabled =
		 * config.getBoolean("Items.CraftableWolfbanePotionEnabled", true);
		 */

		saveConfig();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		return this.commands.onCommand(sender, cmd, label, args);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{
		return this.commands.onTabComplete(sender, cmd, alias, args);
	}

	public void startMetrics()
	{
		try
		{
			Metrics metrics = new Metrics(this);

			Graph pluginsUsedGraph = metrics.createGraph("External plugins used");

			pluginsUsedGraph.addPlotter(new Metrics.Plotter("Using Vault")
			{
				@Override
				public int getValue()
				{
					if (Werewolf.this.vaultEnabled)
					{
						return 1;
					}
					return 0;
				}
			});

			pluginsUsedGraph.addPlotter(new Metrics.Plotter("Using Lib's Disguises")
			{
				@Override public int getValue()
				{
					Plugin pl = getServer().getPluginManager().getPlugin("LibsDisguises");
					if (pl != null && pl.isEnabled())
					{
						return 1;
					}
					return 0;
				}
			});

			pluginsUsedGraph.addPlotter(new Metrics.Plotter("Using SkinsRestorer")
			{
				@Override public int getValue()
				{
					Plugin pl = getServer().getPluginManager().getPlugin("SkinsRestorer");
					if (pl != null && pl.isEnabled())
					{
						return 1;
					}
					return 0;
				}
			});

			 pluginsUsedGraph.addPlotter(new Metrics.Plotter("Using Vampire")
			 {
				 @Override public int getValue()
				 {
					 if (Werewolf.this.vampireEnabled)
					 {
						 return 1;
					 }
					 return 0;
				 }
			 });

			Graph featuresEnabledGraph = metrics.createGraph("Features enabled");

			featuresEnabledGraph.addPlotter(new Metrics.Plotter("WolfChat")
			{
				@Override
				public int getValue()
				{
					if (Werewolf.this.wolfChat)
					{
						return 1;
					}
					return 0;
				}
			});

			featuresEnabledGraph.addPlotter(new Metrics.Plotter("Rename Werewolves")
			{
				@Override
				public int getValue()
				{
					if (Werewolf.this.renameWerewolves)
					{
						return 1;
					}
					return 0;
				}
			});

			featuresEnabledGraph.addPlotter(new Metrics.Plotter("Using clans")
			{
				@Override
				public int getValue()
				{
					if (Werewolf.this.useClans)
					{
						return 1;
					}
					return 0;
				}
			});

			featuresEnabledGraph.addPlotter(new Metrics.Plotter("Using Signs")
			{
				@Override
				public int getValue()
				{
					if (Werewolf.this.useSigns)
					{
						return 1;
					}
					return 0;
				}
			});

			featuresEnabledGraph.addPlotter(new Metrics.Plotter("Using Trophies")
			{
				@Override
				public int getValue()
				{
					if (Werewolf.this.useTrophies)
					{
						return 1;
					}
					return 0;
				}
			});

			featuresEnabledGraph.addPlotter(new Metrics.Plotter("Using Update Notifications")
			{
				@Override
				public int getValue()
				{
					if (Werewolf.this.useUpdateNotifications)
					{
						return 1;
					}
					return 0;
				}
			});

			Graph languageGraph = metrics.createGraph("Languages");

			languageGraph.addPlotter(new Metrics.Plotter(this.language)
			{
				@Override
				public int getValue()
				{
					return 1;
				}
			});

			metrics.start();
		}
		catch (Exception e)
		{
			log("Failed to submit metrics :-(");
		}
	}
}