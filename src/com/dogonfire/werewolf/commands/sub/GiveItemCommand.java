package com.dogonfire.werewolf.commands.sub;

import java.util.function.Supplier;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.commands.CommandUtil;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.ItemManager;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/**
 * Commands that create plugin items for the player: potions, silver sword and lore book go into an empty main hand,
 * silver armor and trackers into the inventory (dropping what doesn't fit).
 */
public class GiveItemCommand extends SubCommand
{
	private final LANGUAGESTRING createdMessage;
	private final boolean intoHand;
	private final Supplier<ItemStack[]> items;

	public GiveItemCommand(WerewolfContext ctx, String name, String permission, String usage, LANGUAGESTRING helpKey, LANGUAGESTRING createdMessage, boolean intoHand, Supplier<ItemStack[]> items)
	{
		super(ctx, name, permission, true, usage, helpKey);
		this.createdMessage = createdMessage;
		this.intoHand = intoHand;
		this.items = items;
	}

	public static GiveItemCommand infectionPotion(WerewolfContext ctx)
	{
		return new GiveItemCommand(ctx, "infectionpotion", "werewolf.potion.infection.create", "/ww infectionpotion", LANGUAGESTRING.HelpInfectionPotion,
				LANGUAGESTRING.CreatedWerewolfInfectionPotion, true, () -> new ItemStack[] { ctx.getItemManager().newInfectionPotion() });
	}

	public static GiveItemCommand curePotion(WerewolfContext ctx)
	{
		return new GiveItemCommand(ctx, "curepotion", "werewolf.potion.cure.create", "/ww curepotion", LANGUAGESTRING.HelpCurePotion,
				LANGUAGESTRING.CreatedWerewolfCurePotion, true, () -> new ItemStack[] { ctx.getItemManager().newCurePotion() });
	}

	public static GiveItemCommand wolfbane(WerewolfContext ctx)
	{
		return new GiveItemCommand(ctx, "wolfbane", "werewolf.potion.wolfbane.create", "/ww wolfbane", LANGUAGESTRING.HelpWolfbanePotion,
				LANGUAGESTRING.CreatedWerewolfWolfbanePotion, true, () -> new ItemStack[] { ctx.getItemManager().newWolfbanePotion() });
	}

	public static GiveItemCommand silverSword(WerewolfContext ctx)
	{
		return new GiveItemCommand(ctx, "silversword", "werewolf.silversword.create", "/ww silversword", LANGUAGESTRING.HelpSilverSword,
				LANGUAGESTRING.CreatedWerewolfSilverSword, true, () -> new ItemStack[] { ctx.getItemManager().newSilverSword(1) });
	}

	public static GiveItemCommand loreBook(WerewolfContext ctx)
	{
		return new GiveItemCommand(ctx, "lorebook", "werewolf.lorebook.create", "/ww lorebook", LANGUAGESTRING.HelpLoreBook,
				LANGUAGESTRING.CreatedWerewolfLoreBook, true, () -> new ItemStack[] { ctx.getItemManager().newLoreBook() });
	}

	public static GiveItemCommand silverArmor(WerewolfContext ctx)
	{
		return new GiveItemCommand(ctx, "silverarmor", "werewolf.silverarmor.create", "/ww silverarmor", LANGUAGESTRING.HelpSilverArmor,
				LANGUAGESTRING.CreatedSilverArmor, false, () ->
				{
					ItemManager.ArmorPiece[] pieces = ItemManager.ArmorPiece.values();
					ItemStack[] armor = new ItemStack[pieces.length];
					for (int i = 0; i < pieces.length; i++)
					{
						armor[i] = ctx.getItemManager().newSilverArmor(pieces[i]);
					}
					return armor;
				});
	}

	public static GiveItemCommand werewolfTracker(WerewolfContext ctx)
	{
		return new GiveItemCommand(ctx, "tracker", "werewolf.tracker.create", "/ww tracker", LANGUAGESTRING.HelpTracker,
				LANGUAGESTRING.CreatedWerewolfTracker, false, () -> new ItemStack[] { ctx.getItemManager().newWerewolfTracker() })
		{
			@Override
			protected String usage()
			{
				return ctx.isVampireEnabled() ? "/ww tracker|vampiretracker" : "/ww tracker";
			}
		};
	}

	/** Has no help line of its own; /ww tracker's line mentions it when VampireRevamp is present */
	public static GiveItemCommand vampireTracker(WerewolfContext ctx)
	{
		return new GiveItemCommand(ctx, "vampiretracker", "werewolf.tracker.create", null, null,
				LANGUAGESTRING.CreatedWerewolfTracker, false, () -> new ItemStack[] { ctx.getItemManager().newVampireTracker() })
		{
			@Override
			public boolean isAvailable(Player player)
			{
				return ctx.isVampireEnabled();
			}
		};
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (this.intoHand)
		{
			if (!CommandUtil.hasEmptyHand(player))
			{
				send(player, LANGUAGESTRING.YouMustHaveYourHandsFree, ChatColor.RED);
				return;
			}
			player.getInventory().setItemInMainHand(this.items.get()[0]);
		}
		else
		{
			for (ItemStack leftover : player.getInventory().addItem(this.items.get()).values())
			{
				player.getWorld().dropItemNaturally(player.getLocation(), leftover);
			}
		}

		send(player, this.createdMessage, ChatColor.GREEN);
	}
}
