package com.dogonfire.werewolf.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class DamageManager
{
	public static double getDamageFromItemInHand(Player player)
	{
		ItemStack is = player.getInventory().getItemInMainHand();
		if (is == null)
		{
			return 1;
		}
		
		double damage = 1;//is.getDurability();
		Material mat = is.getType();		
					
		switch (mat)
		{
			case WOODEN_PICKAXE: damage = 3; break;
			case GOLDEN_PICKAXE: damage = 3; break;
			case STONE_PICKAXE: damage = 4; break;
			case IRON_PICKAXE: damage = 5; break;
			case DIAMOND_PICKAXE: damage = 6; break;
			case NETHERITE_PICKAXE: damage = 7; break;

			case WOODEN_SHOVEL: damage = 2; break;
			case GOLDEN_SHOVEL: damage = 2; break;
			case STONE_SHOVEL: damage = 3; break;
			case IRON_SHOVEL: damage = 4; break;
			case DIAMOND_SHOVEL: damage = 5; break;
			case NETHERITE_SHOVEL: damage = 6; break;

			case WOODEN_AXE: damage = 4; break;
			case GOLDEN_AXE: damage = 4; break;
			case STONE_AXE: damage = 5; break;
			case IRON_AXE: damage = 6; break;
			case DIAMOND_AXE: damage = 7; break;
			case NETHERITE_AXE: damage = 8; break;

			case WOODEN_SWORD: damage = 5; break;
			case GOLDEN_SWORD: damage = 5; break;
			case STONE_SWORD: damage = 6; break;
			case IRON_SWORD: damage = 7; break;
			case DIAMOND_SWORD: damage = 8; break;
			case NETHERITE_SWORD: damage = 9; break;
			
			default: damage = 1; break;
		}
				
		return damage;
	}

	public static boolean canPlayerHit(Player player, Location targetPoint)
	{
		Vector diff = targetPoint.toVector().subtract(player.getLocation().toVector());
		double length = diff.length();
		
		diff = diff.normalize();
		if (length > 4.0D)
		{			
			return false;
		}

		double dotProduct = player.getLocation().getDirection().dot(diff);

		return dotProduct > 0.95D;
	}
}
