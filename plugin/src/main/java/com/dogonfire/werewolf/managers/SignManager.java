package com.dogonfire.werewolf.managers;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.config.Settings;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

import net.milkbowl.vault.economy.Economy;

/**
 * Shop signs: a sign with "Werewolf" on the first line and an item type on the second sells that item when
 * right-clicked. Prices are paid through Vault; without Vault the items are free, and with Vault but no economy plugin
 * nothing is sold. Only registered while Signs.Enabled is on.
 */
public class SignManager implements Listener
{
	private enum SignType
	{
		INFECTION("Infection", "infection", "werewolf infection potion", Settings.Signs::infectionPrice, ItemManager::newInfectionPotion,
				LANGUAGESTRING.YouPlacedAInfectionPotionSign, LANGUAGESTRING.YouBoughtAInfectionPotion),
		CURE("Cure", "cure", "werewolf cure potion", Settings.Signs::curePrice, ItemManager::newCurePotion,
				LANGUAGESTRING.YouPlacedACurePotionSign, LANGUAGESTRING.YouBoughtACurePotion),
		WOLFBANE("Wolfbane", "wolfbane", "werewolf wolfbane potion", Settings.Signs::wolfbanePrice, ItemManager::newWolfbanePotion,
				LANGUAGESTRING.YouPlacedAWolfbanePotionSign, LANGUAGESTRING.YouBoughtAWolfbanePotion),
		SILVER_SWORD("SilverSword", "silversword", "silver sword", Settings.Signs::silverSwordPrice, items -> items.newSilverSword(1),
				LANGUAGESTRING.YouPlacedASilverSwordSign, LANGUAGESTRING.YouBoughtASilverSword),
		LORE_BOOK("LoreBook", "lorebook", "werewolf lorebook", Settings.Signs::bookPrice, ItemManager::newLoreBook,
				LANGUAGESTRING.YouPlacedABookSign, LANGUAGESTRING.YouBoughtALoreBook);

		final String							line;
		final String							permission;
		final String							itemName;
		final ToDoubleFunction<Settings.Signs>	price;
		final Function<ItemManager, ItemStack>	item;
		final LANGUAGESTRING					placed;
		final LANGUAGESTRING					bought;

		SignType(String line, String permission, String itemName, ToDoubleFunction<Settings.Signs> price, Function<ItemManager, ItemStack> item, LANGUAGESTRING placed, LANGUAGESTRING bought)
		{
			this.line = line;
			this.permission = permission;
			this.itemName = itemName;
			this.price = price;
			this.item = item;
			this.placed = placed;
			this.bought = bought;
		}

		static SignType fromLine(String line, boolean ignoreCase)
		{
			if (line == null)
			{
				return null;
			}
			for (SignType type : values())
			{
				if (ignoreCase ? type.line.equalsIgnoreCase(line.trim()) : type.line.equals(line))
				{
					return type;
				}
			}
			return null;
		}
	}

	private final Werewolf			plugin;
	private final WerewolfContext	ctx;

	public SignManager(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	private double getPrice(SignType type)
	{
		return type.price.applyAsDouble(this.plugin.getSettings().signs());
	}

	private boolean hasPermission(Player player, SignType type, String action)
	{
		return player.isOp() || this.ctx.getPermissionsManager().hasPermission(player, "werewolf.sign." + type.permission + "." + action);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onSignChange(SignChangeEvent event)
	{
		String title = event.getLine(0);
		if (title == null || !title.trim().equalsIgnoreCase("Werewolf"))
		{
			return;
		}

		SignType type = SignType.fromLine(event.getLine(1), true);
		if (type == null)
		{
			return;
		}

		Player player = event.getPlayer();
		if (!hasPermission(player, type, "place"))
		{
			event.setCancelled(true);
			event.getBlock().setType(Material.AIR);
			event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.OAK_SIGN, 1));
			return;
		}

		// Without Vault the price line stays empty: the item is free
		String priceText = "";
		if (this.ctx.isVaultEnabled())
		{
			Economy economy = this.ctx.getEconomy();
			priceText = economy != null ? economy.format(getPrice(type)) : String.valueOf(getPrice(type));
		}
		event.setLine(0, "Werewolf");
		event.setLine(1, type.line);
		event.setLine(2, priceText);
		event.setLine(3, "");

		this.plugin.sendInfo(player.getUniqueId(), type.placed, ChatColor.GREEN, "", (int) getPrice(type), 1);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onSignClick(PlayerInteractEvent event)
	{
		if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getClickedBlock().getType() != Material.OAK_WALL_SIGN)
		{
			return;
		}

		BlockState state = event.getClickedBlock().getState();
		Sign sign = (Sign) state;

		String titleLine = sign.getLine(0);
		if (titleLine == null || !titleLine.equals("Werewolf"))
		{
			return;
		}

		Player player = event.getPlayer();
		SignType type = SignType.fromLine(sign.getLine(1), false);
		if (type == null || !hasPermission(player, type, "use"))
		{
			return;
		}

		double price = 0.0D;
		if (this.ctx.isVaultEnabled())
		{
			Economy economy = this.ctx.getEconomy();
			if (economy == null)
			{
				player.sendMessage(this.ctx.getLanguageManager().getLanguageString(LANGUAGESTRING.EconomyUnavailable, ChatColor.RED));
				this.plugin.logDebug("No economy provider: " + player.getName() + " could not buy a " + type.itemName);
				return;
			}

			price = getPrice(type);
			if (!economy.has(player, price))
			{
				this.plugin.sendInfo(player.getUniqueId(), LANGUAGESTRING.YouDoNotHaveEnoughMoney, ChatColor.RED, "", (int) price, 1);
				return;
			}
			economy.withdrawPlayer(player, price);
		}

		player.getWorld().dropItemNaturally(player.getLocation(), type.item.apply(this.ctx.getItemManager()));

		this.plugin.sendInfo(player.getUniqueId(), type.bought, ChatColor.GREEN, "", (int) price, 1);
		this.plugin.log(player.getName() + " bought a " + type.itemName + " for " + price);
	}
}
