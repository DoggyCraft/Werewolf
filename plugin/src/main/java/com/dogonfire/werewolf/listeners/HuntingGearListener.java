package com.dogonfire.werewolf.listeners;


import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.api.event.TrackerTargetEvent.TargetType;
import com.dogonfire.werewolf.api.event.WerewolfTransformEvent;
import com.dogonfire.werewolf.managers.LanguageManager;

/**
 * Hunting gear: silver armor (only humans may wear it), tracker compasses and scent sniffing. Silver armor's bite
 * protection is rolled in WerewolfManager.makeWerewolf, after the infection event.
 * The armor's damage reduction is in DamageListener; the periodic checks are in ItemManager, TrackerManager and
 * ScentManager.
 */
public class HuntingGearListener implements Listener
{
	private final Werewolf	plugin;
	private final WerewolfContext ctx;

	public HuntingGearListener(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	private void sendMessage(Player player, LanguageManager.LANGUAGESTRING message, ChatColor color)
	{
		player.sendMessage(ctx.getLanguageManager().getLanguageString(message, color));
	}

	private boolean mayNotWearSilver(Player player)
	{
		return ctx.isWerewolvesEnabled() && !ctx.getWerewolfManager().isHuman(player);
	}

	// Right-clicking armor equips it
	@EventHandler(priority = EventPriority.HIGH)
	public void onEquipByHand(PlayerInteractEvent event)
	{
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
		{
			return;
		}

		if (!ctx.getItemManager().isSilverArmor(event.getItem()) || !mayNotWearSilver(event.getPlayer()))
		{
			return;
		}

		event.setUseItemInHand(Event.Result.DENY);
		sendMessage(event.getPlayer(), LanguageManager.LANGUAGESTRING.SilverArmorCannotWear, ChatColor.RED);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onEquipByInventory(InventoryClickEvent event)
	{
		if (!(event.getWhoClicked() instanceof Player))
		{
			return;
		}

		Player player = (Player) event.getWhoClicked();
		boolean equipping = false;

		if (event.getSlotType() == InventoryType.SlotType.ARMOR)
		{
			ItemStack placed = event.getCursor();
			if (event.getClick() == ClickType.NUMBER_KEY)
			{
				placed = player.getInventory().getItem(event.getHotbarButton());
			}
			else if (event.getClick() == ClickType.SWAP_OFFHAND)
			{
				placed = player.getInventory().getItemInOffHand();
			}
			equipping = ctx.getItemManager().isSilverArmor(placed);
		}
		else if (event.isShiftClick() && event.getView().getTopInventory().getType() == InventoryType.CRAFTING)
		{
			equipping = ctx.getItemManager().isSilverArmor(event.getCurrentItem());
		}

		if (equipping && mayNotWearSilver(player))
		{
			event.setCancelled(true);
			sendMessage(player, LanguageManager.LANGUAGESTRING.SilverArmorCannotWear, ChatColor.RED);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onDispenseArmor(BlockDispenseArmorEvent event)
	{
		LivingEntity target = event.getTargetEntity();
		if (target instanceof Player && ctx.getItemManager().isSilverArmor(event.getItem()) && mayNotWearSilver((Player) target))
		{
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onTrackerUse(PlayerInteractEvent event)
	{
		if (event.useItemInHand() == Event.Result.DENY || event.getAction() == Action.PHYSICAL)
		{
			return;
		}

		ItemStack item = event.getItem();
		TargetType type = ctx.getTrackerManager().getTrackerType(item);
		if (type == null)
		{
			return;
		}

		Player hunter = event.getPlayer();
		// Right-clicking fires once per hand; with a tracker in each hand only the main hand counts
		if (event.getHand() == EquipmentSlot.OFF_HAND && ctx.getTrackerManager().getTrackerType(hunter.getInventory().getItemInMainHand()) != null)
		{
			return;
		}

		boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
		if (rightClick)
		{
			// Don't let the tracker bind to a lodestone
			event.setCancelled(true);
		}

		if (!this.plugin.getSettings().hunting().enabled())
		{
			sendMessage(hunter, LanguageManager.LANGUAGESTRING.HuntingDisabled, ChatColor.RED);
			return;
		}

		String itemName = getItemName(item);
		if (!ctx.getWerewolfManager().isHuman(hunter))
		{
			ctx.getLanguageManager().setType(itemName);
			sendMessage(hunter, LanguageManager.LANGUAGESTRING.TrackerOnlyHumans, ChatColor.RED);
			return;
		}

		if (rightClick)
		{
			ctx.getTrackerManager().lock(hunter, type, itemName);
		}
		else
		{
			ctx.getTrackerManager().clear(hunter, itemName);
		}
	}

	private static String getItemName(ItemStack item)
	{
		ItemMeta meta = item.getItemMeta();
		if (meta != null && meta.hasDisplayName())
		{
			return meta.getDisplayName();
		}
		return "tracker";
	}

	@EventHandler(ignoreCancelled = true)
	public void onCraftTracker(CraftItemEvent event)
	{
		if (!(event.getWhoClicked() instanceof Player) || ctx.getTrackerManager().getTrackerType(event.getRecipe().getResult()) == null)
		{
			return;
		}

		Player player = (Player) event.getWhoClicked();
		if (!ctx.getWerewolfManager().isHuman(player))
		{
			event.setCancelled(true);
			ctx.getLanguageManager().setType(getItemName(event.getRecipe().getResult()));
			sendMessage(player, LanguageManager.LANGUAGESTRING.TrackerOnlyHumansCraft, ChatColor.RED);
		}
	}

	// A werewolf in wolf form right-clicks a player to sniff out their scent
	@EventHandler(ignoreCancelled = true)
	public void onSniff(PlayerInteractEntityEvent event)
	{
		if (!ctx.isWerewolvesEnabled() || event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Player))
		{
			return;
		}

		ctx.getScentManager().sniff(event.getPlayer(), (Player) event.getRightClicked());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onUntransform(WerewolfTransformEvent event)
	{
		if (!event.isToWolfForm())
		{
			ctx.getScentManager().stopTracking(event.getPlayer());
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event)
	{
		ctx.getScentManager().clear(event.getPlayer());
		ctx.getTrackerManager().remove(event.getPlayer().getUniqueId());
	}
}
