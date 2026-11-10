package com.dogonfire.werewolf.listeners;

import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.api.event.WerewolfCureEvent;
import com.dogonfire.werewolf.api.event.WerewolfInfectionEvent;


import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener
{
	private final Werewolf	plugin;
	private final WerewolfContext ctx;

	public InteractListener(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	private boolean checkForDefense(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		
		Material type = player.getInventory().getItemInMainHand().getType();
		if (isForbiddenDefense(type))
		{
			player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfTryDefense, ChatColor.RED));
			event.setCancelled(true);
			return true;
		}
		
		return false;
	}

	private boolean checkForDrinkingWerewolfCurePotion(PlayerItemConsumeEvent event)
	{
		Player player = event.getPlayer();
		// The item being drunk, from whichever hand; the game uses up exactly that one (a glass bottle is left)
		ItemStack handItem = event.getItem();
		
		if(!ctx.getItemManager().isCurePotion(handItem))
		{
			return false;
		}

		if (!ctx.getPermissionsManager().hasPermission(player, "werewolf.potion.cure.drink") && !player.isOp())
		{
			return false;
		}
		
		if (!ctx.getWerewolfManager().isWerewolf(player.getUniqueId()))
		{
			player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.DrinkCureFailure, ChatColor.LIGHT_PURPLE));
		}
		else if (!ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			if (Math.random() < this.plugin.getSettings().infection().cureChance())
			{
				if (ctx.getWerewolfManager().unmakeWerewolf(player.getUniqueId(), WerewolfCureEvent.Cause.CURE_POTION))
				{
					player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.DrinkCureSuccess, ChatColor.LIGHT_PURPLE));
				}
			}
			else
			{
				player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.DrinkCureFailure, ChatColor.LIGHT_PURPLE));
			}
		}
		else
		{
			return false;
		}

		return true;
	}

	private boolean checkForDrinkingWerewolfInfectionPotion(PlayerItemConsumeEvent event)
	{
		Player player = event.getPlayer();
		// The item being drunk, from whichever hand; the game uses up exactly that one (a glass bottle is left)
		ItemStack handItem = event.getItem();

		if(!ctx.getItemManager().isInfectionPotion(handItem))
		{
			return false;
		}

		if ((!ctx.getPermissionsManager().hasPermission(player, "werewolf.potion.infection.drink")) && (!player.isOp()))
		{
			return false;
		}
		
		if (ctx.getWerewolfManager().isWerewolf(player))
		{
			player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.DrinkInfectionFailure, ChatColor.LIGHT_PURPLE));
		}
		else if (ctx.getMoonManager().isTransformationNight(player.getWorld()))
		{
			player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.DrinkInfectionSuccess, ChatColor.LIGHT_PURPLE));
			plugin.log(player.getName() + " contracted the werewolf infection by drinking a potion!");
			
			final UUID targetId = player.getUniqueId();

			this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable()
			{
				public void run()
				{
					// Offline now, or became a werewolf some other way in the 7 seconds since drinking
					Player targetPlayer = plugin.getServer().getPlayer(targetId);
					if (targetPlayer == null || ctx.getWerewolfManager().isWerewolf(targetPlayer))
					{
						return;
					}
					ctx.getWerewolfManager().makeWerewolf(targetPlayer, true, Clan.Potion, WerewolfInfectionEvent.Source.POTION);
				}
			}, 140L);
		}
		else
		{
			player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.DrinkInfectionFailure, ChatColor.LIGHT_PURPLE));
		}

		return true;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (!ctx.isWerewolvesEnabled())
		{
			return;
		}
		Player player = event.getPlayer();
		if (((event.getAction().equals(Action.RIGHT_CLICK_AIR)) || (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) && (ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId())))
		{
			checkForDefense(event);
		}
	}

	@EventHandler
	public void onPlayerConsumeEvent(PlayerItemConsumeEvent event)
	{
		if (!ctx.isWerewolvesEnabled())
		{
			return;
		}
		if (ctx.getWerewolfManager().isWerewolf(event.getPlayer()))
		{
			if (!checkForDrinkingWerewolfCurePotion(event))
			{
				checkForDrinkingWerewolfInfectionPotion(event);
			}
		}
		else if (!checkForDrinkingWerewolfInfectionPotion(event))
		{
			checkForDrinkingWerewolfCurePotion(event);
		}
	}

	@EventHandler
	public void dispenseArmorEvent(BlockDispenseArmorEvent event)
	{
		if (event.getTargetEntity() instanceof Player)
		{
			Player player = (Player) event.getTargetEntity();

			if (ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
			{
				event.setCancelled(true);
			}
		}
	}
	
	public boolean isForbiddenDefense(Material material)
	{
		switch (material)
		{
			case GOLDEN_CHESTPLATE:
			case LEATHER_CHESTPLATE:
			case DIAMOND_CHESTPLATE:
			case CHAINMAIL_CHESTPLATE:
			case IRON_CHESTPLATE:
			case NETHERITE_CHESTPLATE:
			case CHAINMAIL_BOOTS:
			case GOLDEN_BOOTS:
			case LEATHER_BOOTS:
			case IRON_BOOTS:
			case DIAMOND_BOOTS:
			case NETHERITE_BOOTS:
			case GOLDEN_LEGGINGS:
			case CHAINMAIL_LEGGINGS:
			case LEATHER_LEGGINGS:
			case IRON_LEGGINGS:
			case DIAMOND_LEGGINGS:
			case NETHERITE_LEGGINGS:
			case CHAINMAIL_HELMET:
			case GOLDEN_HELMET:
			case LEATHER_HELMET:
			case IRON_HELMET:
			case DIAMOND_HELMET:
			case NETHERITE_HELMET:
			case TURTLE_HELMET:
			case ELYTRA:
			case SHIELD:
				return true;
			default:
				return false;
		}
	}
}
