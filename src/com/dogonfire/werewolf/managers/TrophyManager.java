package com.dogonfire.werewolf.managers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.storage.TrophyRepository;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import com.dogonfire.werewolf.managers.ClanManager.ClanType;

public class TrophyManager implements Listener
{
	private Werewolf plugin				= null;
	private final TrophyRepository	trophies;

	public TrophyManager(Werewolf plugin, TrophyRepository trophies)
	{
		this.plugin = plugin;
		this.trophies = trophies;
	}

	// UUID's will render the skulls blank, so we use PlayerNames, for now.
	// Otherwise we might have to use the cached textured from MySkin, or just request from MojangAPI,
	// and cache them here...?
	@SuppressWarnings("deprecation") 
	public ItemStack getTrophyFromWerewolfPlayer(UUID killerId, UUID werewolfId)
	{
		ItemStack skullTrophy = new ItemStack(Material.PLAYER_HEAD);

		SkullMeta skullMeta = (SkullMeta) skullTrophy.getItemMeta();

		Date thisDate = new Date();
		String pattern = "dd MMMM yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		ClanType clan = Werewolf.getWerewolfManager().getWerewolfClan(werewolfId);
		OfflinePlayer skullPlayer = null;

		if (Werewolf.getClanManager().isAlpha(werewolfId))
		{
			skullPlayer = plugin.getServer().getOfflinePlayer(Werewolf.getClanManager().getWerewolfAccountForAlpha(clan));
		}
		else
		{
			skullPlayer = plugin.getServer().getOfflinePlayer(Werewolf.getClanManager().getWerewolfAccountForClan(clan));
		}
		
		skullMeta.setOwningPlayer(skullPlayer);
		skullTrophy.setItemMeta(skullMeta);
		
		String werewolfName = "Werewolf";
		if (plugin.getSettings().names().enabled())
		{
			werewolfName = Werewolf.getWerewolfManager().getWerewolfName(werewolfId) + "'s";
		}

		skullMeta.setDisplayName(ChatColor.GOLD + werewolfName + " Head");

		String killerName = plugin.getServer().getPlayer(killerId).getName();
		
		Werewolf.getLanguageManager().setPlayerName(killerName);
		Werewolf.getLanguageManager().setAmount(formatter.format(thisDate));
		String lorePage = Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrophyDescription, ChatColor.GRAY);

		List<String> lorePages = new ArrayList<String>();
		lorePages.add(lorePage);
		skullMeta.setLore(lorePages);
		Werewolf.getItemManager().tagWerewolfTrophy(skullMeta);

		skullTrophy.setItemMeta(skullMeta);

		return skullTrophy;
		/*
		ItemMeta headMeta = skullTrophy.getItemMeta();
		GameProfile profile = new GameProfile(UUID.randomUUID(), null);
		byte[] encodedData = Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", skinURL).getBytes());
		
		profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
		
		Field profileField = null;
		try 
		{
			profileField = headMeta.getClass().getDeclaredField("profile");
		} 
		catch (NoSuchFieldException | SecurityException e) 
		{
			e.printStackTrace();
		}
		
		profileField.setAccessible(true);
		
		try 
		{
			profileField.set(headMeta, profile);
		} 
		catch (IllegalArgumentException | IllegalAccessException e) 
		{
			e.printStackTrace();
		}
		
		skullTrophy.setItemMeta(headMeta);
		*/
	}

	private int hashVector(Location location)
	{
		return location.getBlockX() * 73856093 ^ location.getBlockY() * 19349663 ^ location.getBlockZ() * 83492791;
	}

	/** Werewolf trophies are tagged; older ones (and dropped ones, which lose the tag) are known by their gold "... Head" name */
	private boolean isTrophy(ItemStack skull)
	{
		if (Werewolf.getItemManager().isWerewolfTrophy(skull))
		{
			return true;
		}
		String name = skull.hasItemMeta() && skull.getItemMeta().hasDisplayName() ? skull.getItemMeta().getDisplayName() : "";
		return name.startsWith(ChatColor.GOLD.toString()) && name.endsWith(" Head");
	}

	public void handlePlaceSkull(ItemStack skull, Location location)
	{
		this.plugin.logDebug("Setting skull at " + location);
		List<String> lore = isTrophy(skull) ? skull.getItemMeta().getLore() : null;
		if (lore == null || lore.isEmpty())
		{
			this.trophies.remove(String.valueOf(hashVector(location)));
		}
		else
		{
			this.trophies.put(String.valueOf(hashVector(location)), lore);
		}
	}

	public boolean handleBreakSkull(ItemStack skullItem, Location location)
	{			
		List<String> description = this.trophies.getDescription(String.valueOf(hashVector(location)));
		if (description == null)
		{
			return false;
		}
		
		SkullMeta skullMeta = (SkullMeta) skullItem.getItemMeta();
		
		skullMeta.setDisplayName(ChatColor.GOLD + "Werewolf Head");

		skullMeta.setLore(new ArrayList<String>(description));
		Werewolf.getItemManager().tagWerewolfTrophy(skullMeta);

		this.trophies.remove(String.valueOf(hashVector(location)));

		skullItem.setItemMeta(skullMeta);

		return true;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event)
	{
		if (event.getBlock().getType() != Material.PLAYER_HEAD || event.getBlock().getDrops().isEmpty())
		{
			return;
		}
		ItemStack item = (ItemStack) event.getBlock().getDrops().toArray()[0];
		if (handleBreakSkull(item, event.getBlock().getLocation()))
		{
			event.getBlock().setType(Material.AIR);
			event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event)
	{
		if (event.getItemInHand().getType() != Material.PLAYER_HEAD)
		{
			return;
		}
		handlePlaceSkull(event.getItemInHand(), event.getBlockPlaced().getLocation());
	}
}
