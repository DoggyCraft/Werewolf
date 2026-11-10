package com.dogonfire.werewolf.listeners;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InventoryListener implements Listener
{
	// Not configurable; werewolves in wolf form never hold weapons
	private static final boolean	KEEP_WEREWOLF_HANDS_FREE	= true;

	private final Werewolf	plugin;
	private final WerewolfContext ctx;

	public InventoryListener(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInventoryClose(InventoryCloseEvent event)
	{
		if ((!ctx.isWerewolvesEnabled()) || (!this.plugin.getSettings().general().dropArmorOnTransform()))
		{
			return;
		}

		if (!(event.getPlayer() instanceof Player))
		{
			return;
		}

		Player player = (Player) event.getPlayer();
		if (!ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			return;
		}

		removeOffHand(player);
		removeMainHand(player);
		removeArmor(player);
	}

	public void removeMainHand(Player player)
	{
		if (isForbiddenItem(player.getInventory().getItemInMainHand().getType()))
		{
			if (ctx.getWerewolfManager().hasToDropItems(player.getUniqueId()))
			{
				player.getWorld().dropItemNaturally(player.getLocation(), player.getInventory().getItemInMainHand());
			}
			else
			{
				ItemStack stack = player.getInventory().getItemInMainHand();
				int slot = player.getInventory().firstEmpty();
				if (slot > -1)
				{
					player.getInventory().setItem(slot, stack);
				}
				else
				{
					player.getWorld().dropItemNaturally(player.getLocation(), player.getInventory().getItemInMainHand());
				}
			}
			player.getInventory().setItemInMainHand(null);
		}
	}

	public void removeOffHand(Player player)
	{
		if (isForbiddenItem(player.getInventory().getItemInOffHand().getType()))
		{
			if (ctx.getWerewolfManager().hasToDropItems(player.getUniqueId()))
			{
				player.getWorld().dropItemNaturally(player.getLocation(), player.getInventory().getItemInOffHand());
			}
			else
			{
				ItemStack stack = player.getInventory().getItemInOffHand();
				int slot = player.getInventory().firstEmpty();
				if (slot > -1)
				{
					player.getInventory().setItem(slot, stack);
				}
				else
				{
					player.getWorld().dropItemNaturally(player.getLocation(), player.getInventory().getItemInOffHand());
				}
			}
			player.getInventory().setItemInOffHand(null);
		}
	}

	public void removeArmor(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		for (ItemStack stack : inventory.getArmorContents())
		{
			if (stack != null && !stack.getType().equals(Material.AIR) && stack.getAmount() != 0)
			{
				if (ctx.getWerewolfManager().hasToDropItems(player.getUniqueId()))
				{
					player.getWorld().dropItemNaturally(player.getLocation(), stack);
				}
				else
				{
					int slot = player.getInventory().firstEmpty();
					if (slot > -1)
					{
						player.getInventory().setItem(slot, stack);
					}
					else
					{
						player.getWorld().dropItemNaturally(player.getLocation(), stack);
					}
				}
			}
		}

		player.getInventory().setArmorContents(new ItemStack[] { new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR) });
	}

	@EventHandler
	public void onPlayerNewHeldItem(PlayerItemHeldEvent event)
	{
		Player player = event.getPlayer();
		Entity entity = player;
		if (entity.getType() == EntityType.PLAYER && player != null) {
			if (!ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
			{
				return;
			}

			if (KEEP_WEREWOLF_HANDS_FREE)
			{
				int heldItemSlot = event.getNewSlot();
				if (player.getInventory().getItem(heldItemSlot) != null && player.getInventory().getItem(heldItemSlot).getType() != Material.AIR)
				{
					Material material = player.getInventory().getItem(heldItemSlot).getType();
					if (!isForbiddenItem(material)) { return; }

					ItemStack stack = player.getInventory().getItem(heldItemSlot);
					int inventorySlots = 36; //player.getInventory().getSize();
					Boolean foundSlot = false;

					for (int slot = 0; slot < inventorySlots; slot++) {
						if ((player.getInventory().getItem(slot) == null || player.getInventory().getItem(slot).getType() == Material.AIR) && slot != heldItemSlot) {
							player.getInventory().setItem(slot, stack);
							player.getInventory().setItem(heldItemSlot, new ItemStack(Material.AIR));
							foundSlot = true;
							break;
						}
					}
					
					if (!foundSlot) {
						event.setCancelled(true);
					}
				}
			}	
		}
	}

	@EventHandler
	public void onPlayerPickupItem(EntityPickupItemEvent event)
	{
		Entity entity = event.getEntity();
		if (entity.getType() == EntityType.PLAYER)
		{
			Player player = (Player) entity;

			if (!ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
			{
				return;
			}

			Material material = event.getItem().getItemStack().getType();
			// The difference is that, if it is not a forbidden item, we still
			// don't want
			// the item to be in the main hand, but if there is no other choice,
			// then it's fine.
			if (!isForbiddenItem(material))
			{
				if (player.getInventory().getItemInMainHand().getType() == Material.AIR)
				{
					ItemStack stack = event.getItem().getItemStack();

					int heldItemSlot = player.getInventory().getHeldItemSlot();
					int inventorySlots = 36;

					for (int slot = 0; slot < inventorySlots; slot++)
					{
						if ((player.getInventory().getItem(slot) == null || player.getInventory().getItem(slot).getType() == Material.AIR) && slot != heldItemSlot)
						{
							player.getInventory().setItem(slot, stack);
							event.getItem().remove();
							event.setCancelled(true);
							break;
						}
					}
				}
				return;
			}

			if (KEEP_WEREWOLF_HANDS_FREE)
			{
				if (player.getInventory().getItemInMainHand().getType() == Material.AIR)
				{
					ItemStack stack = event.getItem().getItemStack();

					int heldItemSlot = player.getInventory().getHeldItemSlot();
					int inventorySlots = 36; // player.getInventory().getSize();
					Boolean foundSlot = false;

					for (int slot = 0; slot < inventorySlots; slot++)
					{
						if ((player.getInventory().getItem(slot) == null || player.getInventory().getItem(slot).getType() == Material.AIR) && slot != heldItemSlot)
						{
							player.getInventory().setItem(slot, stack);
							event.getItem().remove();
							event.setCancelled(true);
							foundSlot = true;
							break;
						}
					}

					if (!foundSlot)
					{
						event.setCancelled(true);
					}
				}
			}
		}
	}

	public boolean isForbiddenItem(Material material)
	{
		switch (material)
		{
			case WOODEN_SWORD:
			case GOLDEN_SWORD:
			case STONE_SWORD:
			case DIAMOND_SWORD:
			case NETHERITE_SWORD:
			case IRON_SWORD:
			case SHIELD:
			case BOW:
			case POTION:
			case SPLASH_POTION:
			case WOODEN_AXE:
			case GOLDEN_AXE:
			case STONE_AXE:
			case DIAMOND_AXE:
			case NETHERITE_AXE:
			case IRON_AXE:
			case WOODEN_PICKAXE:
			case GOLDEN_PICKAXE:
			case STONE_PICKAXE:
			case DIAMOND_PICKAXE:
			case NETHERITE_PICKAXE:
			case IRON_PICKAXE:
			case WOODEN_HOE:
			case GOLDEN_HOE:
			case STONE_HOE:
			case DIAMOND_HOE:
			case NETHERITE_HOE:
			case IRON_HOE:
			case WOODEN_SHOVEL:
			case GOLDEN_SHOVEL:
			case STONE_SHOVEL:
			case DIAMOND_SHOVEL:
			case NETHERITE_SHOVEL:
			case IRON_SHOVEL:
			case FLINT_AND_STEEL:
//			case FISHING_ROD:
//			case LEAD:
//			case COMPASS:
//			case CLOCK:
			case ENDER_PEARL:
//			case ENDER_EYE:
//			case WRITABLE_BOOK:
//			case BOOK:
			case FIRE_CHARGE:
//			case NAME_TAG:
//			case CARROT_ON_A_STICK:
//			case WARPED_FUNGUS_ON_A_STICK:
//			case MAP:
//			case FILLED_MAP:
//			case BUCKET:
//			case WATER_BUCKET:
//			case PUFFERFISH_BUCKET:
//			case SALMON_BUCKET:
//			case MILK_BUCKET:
//			case SHEARS:
			case TRIDENT:
			case TOTEM_OF_UNDYING:
			case CROSSBOW:
			case SPYGLASS:
//			case RECOVERY_COMPASS:
//			case BUNDLE:
//			case BRUSH:
				return true;
			default:
				return false;
		}
	}
}
