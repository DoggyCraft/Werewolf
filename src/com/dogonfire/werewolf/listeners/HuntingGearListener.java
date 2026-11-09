package com.dogonfire.werewolf.listeners;

import java.util.Random;

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
import com.dogonfire.werewolf.WerewolfAPI;
import com.dogonfire.werewolf.events.TrackerTargetEvent.TargetType;
import com.dogonfire.werewolf.events.WerewolfInfectionEvent;
import com.dogonfire.werewolf.events.WerewolfTransformEvent;
import com.dogonfire.werewolf.managers.LanguageManager;

/**
 * Hunting gear: silver armor (infection protection, only humans may wear it), tracker compasses and scent sniffing.
 * The armor's damage reduction is in DamageListener; the periodic checks are in ItemManager, TrackerManager and
 * ScentManager.
 */
public class HuntingGearListener implements Listener
{
	private final Werewolf	plugin;
	private final Random	random	= new Random();

	public HuntingGearListener(Werewolf plugin)
	{
		this.plugin = plugin;
	}

	private void sendMessage(Player player, LanguageManager.LANGUAGESTRING message, ChatColor color)
	{
		player.sendMessage(Werewolf.getLanguageManager().getLanguageString(message, color));
	}

	// Silver armor gives its wearer a chance to shake off a werewolf bite (not potions or commands)
	@EventHandler(ignoreCancelled = true)
	public void onInfection(WerewolfInfectionEvent event)
	{
		if (event.getSource() != WerewolfInfectionEvent.Source.WEREWOLF_BITE && event.getSource() != WerewolfInfectionEvent.Source.WILD_WOLF_BITE)
		{
			return;
		}

		int purity = Werewolf.getItemManager().getSilverArmorPurity(event.getPlayer());
		if (purity > 0 && this.random.nextInt(100) < purity)
		{
			event.setCancelled(true);
			sendMessage(event.getPlayer(), LanguageManager.LANGUAGESTRING.SilverArmorCleansed, ChatColor.AQUA);
		}
	}

	private boolean mayNotWearSilver(Player player)
	{
		return Werewolf.pluginEnabled && !WerewolfAPI.isHuman(player);
	}

	// Right-clicking armor equips it
	@EventHandler(priority = EventPriority.HIGH)
	public void onEquipByHand(PlayerInteractEvent event)
	{
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
		{
			return;
		}

		if (!Werewolf.getItemManager().isSilverArmor(event.getItem()) || !mayNotWearSilver(event.getPlayer()))
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
			equipping = Werewolf.getItemManager().isSilverArmor(placed);
		}
		else if (event.isShiftClick() && event.getView().getTopInventory().getType() == InventoryType.CRAFTING)
		{
			equipping = Werewolf.getItemManager().isSilverArmor(event.getCurrentItem());
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
		if (target instanceof Player && Werewolf.getItemManager().isSilverArmor(event.getItem()) && mayNotWearSilver((Player) target))
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
		TargetType type = Werewolf.getTrackerManager().getTrackerType(item);
		if (type == null)
		{
			return;
		}

		Player hunter = event.getPlayer();
		// Right-clicking fires once per hand; with a tracker in each hand only the main hand counts
		if (event.getHand() == EquipmentSlot.OFF_HAND && Werewolf.getTrackerManager().getTrackerType(hunter.getInventory().getItemInMainHand()) != null)
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
		if (!WerewolfAPI.isHuman(hunter))
		{
			Werewolf.getLanguageManager().setType(itemName);
			sendMessage(hunter, LanguageManager.LANGUAGESTRING.TrackerOnlyHumans, ChatColor.RED);
			return;
		}

		if (rightClick)
		{
			Werewolf.getTrackerManager().lock(hunter, type, itemName);
		}
		else
		{
			Werewolf.getTrackerManager().clear(hunter, itemName);
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
		if (!(event.getWhoClicked() instanceof Player) || Werewolf.getTrackerManager().getTrackerType(event.getRecipe().getResult()) == null)
		{
			return;
		}

		Player player = (Player) event.getWhoClicked();
		if (!WerewolfAPI.isHuman(player))
		{
			event.setCancelled(true);
			Werewolf.getLanguageManager().setType(getItemName(event.getRecipe().getResult()));
			sendMessage(player, LanguageManager.LANGUAGESTRING.TrackerOnlyHumansCraft, ChatColor.RED);
		}
	}

	// A werewolf in wolf form right-clicks a player to sniff out their scent
	@EventHandler(ignoreCancelled = true)
	public void onSniff(PlayerInteractEntityEvent event)
	{
		if (!Werewolf.pluginEnabled || event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Player))
		{
			return;
		}

		Werewolf.getScentManager().sniff(event.getPlayer(), (Player) event.getRightClicked());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onUntransform(WerewolfTransformEvent event)
	{
		if (!event.isToWolfForm())
		{
			Werewolf.getScentManager().stopTracking(event.getPlayer());
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event)
	{
		Werewolf.getScentManager().clear(event.getPlayer());
		Werewolf.getTrackerManager().remove(event.getPlayer().getUniqueId());
	}
}
