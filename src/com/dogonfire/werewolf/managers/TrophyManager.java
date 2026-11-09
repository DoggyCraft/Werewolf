package com.dogonfire.werewolf.managers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
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
import com.dogonfire.werewolf.api.Clan;

public class TrophyManager implements Listener
{
	private final Werewolf plugin;
	private final WerewolfContext ctx;
	private final TrophyRepository	trophies;

	public TrophyManager(WerewolfContext ctx, TrophyRepository trophies)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.trophies = trophies;
	}

	// UUIDs render the skulls blank, so the head is made from the skin account's name
	@SuppressWarnings("deprecation") 
	public ItemStack getTrophyFromWerewolfPlayer(UUID killerId, UUID werewolfId)
	{
		ItemStack skullTrophy = new ItemStack(Material.PLAYER_HEAD);

		SkullMeta skullMeta = (SkullMeta) skullTrophy.getItemMeta();

		Date thisDate = new Date();
		String pattern = "dd MMMM yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		Clan clan = ctx.getWerewolfManager().getWerewolfClan(werewolfId);
		OfflinePlayer skullPlayer = null;

		if (ctx.getClanManager().isAlpha(werewolfId))
		{
			skullPlayer = plugin.getServer().getOfflinePlayer(ctx.getClanManager().getWerewolfAccountForAlpha(clan));
		}
		else
		{
			skullPlayer = plugin.getServer().getOfflinePlayer(ctx.getClanManager().getWerewolfAccountForClan(clan));
		}
		
		skullMeta.setOwningPlayer(skullPlayer);
		skullTrophy.setItemMeta(skullMeta);
		
		String werewolfName = "Werewolf";
		if (plugin.getSettings().names().enabled())
		{
			werewolfName = ctx.getWerewolfManager().getWerewolfName(werewolfId) + "'s";
		}

		skullMeta.setDisplayName(ChatColor.GOLD + werewolfName + " Head");

		String killerName = plugin.getServer().getPlayer(killerId).getName();
		
		ctx.getLanguageManager().setPlayerName(killerName);
		ctx.getLanguageManager().setAmount(formatter.format(thisDate));
		String lorePage = ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrophyDescription, ChatColor.GRAY);

		List<String> lorePages = new ArrayList<String>();
		lorePages.add(lorePage);
		skullMeta.setLore(lorePages);
		ctx.getItemManager().tagWerewolfTrophy(skullMeta);

		skullTrophy.setItemMeta(skullMeta);

		return skullTrophy;
	}

	private int hashVector(Location location)
	{
		return location.getBlockX() * 73856093 ^ location.getBlockY() * 19349663 ^ location.getBlockZ() * 83492791;
	}

	/** Werewolf trophies are tagged; older ones (and dropped ones, which lose the tag) are known by their gold "... Head" name */
	private boolean isTrophy(ItemStack skull)
	{
		if (ctx.getItemManager().isWerewolfTrophy(skull))
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
		ctx.getItemManager().tagWerewolfTrophy(skullMeta);

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
