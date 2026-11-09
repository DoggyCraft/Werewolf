package com.dogonfire.werewolf;

import java.util.UUID;

import com.dogonfire.werewolf.managers.ClanManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WerewolfAPI
{
	private static volatile WerewolfContext	ctx;

	// Set by the plugin on enable, cleared on disable
	static void setContext(WerewolfContext context)
	{
		ctx = context;
	}

	private static WerewolfContext ctx()
	{
		WerewolfContext context = ctx;
		if (context == null)
		{
			throw new IllegalStateException("Werewolf is not enabled");
		}
		return context;
	}

	/** Does nothing; kept so plugins built against older versions still link */
	@Deprecated
	public static void setFullWerewolf(UUID playerId)
	{
	}

	/** Whether the Werewolf plugin is enabled, so the other methods can be used */
	public static boolean isEnabled()
	{
		return ctx != null;
	}

	public static ItemStack newWerewolfSilverSword()
	{
		return ctx().getItemManager().newSilverSword(1);
	}

	public static ItemStack newWerewolfLoreBook()
	{
		return ctx().getItemManager().newLoreBook();
	}

	public static ItemStack newWerewolfCurePotion()
	{
		return ctx().getItemManager().newCurePotion();
	}

	public static ItemStack newWerewolfInfectionPotion()
	{
		return ctx().getItemManager().newInfectionPotion();
	}

	public static ItemStack newWerewolfWolfbanePotion()
	{
		return ctx().getItemManager().newWolfbanePotion();
	}

	public static boolean isWerewolfSilverSword(ItemStack item)
	{
		return ctx().getItemManager().isSilverSword(item);
	}

	public static ItemStack newWerewolfTracker()
	{
		return ctx().getItemManager().newWerewolfTracker();
	}

	public static boolean isWerewolfSilverArmor(ItemStack item)
	{
		return ctx().getItemManager().isSilverArmor(item);
	}

	public static boolean isWerewolfTracker(ItemStack item)
	{
		return ctx().getItemManager().isWerewolfTracker(item);
	}

	public static boolean isWerewolfLoreBook(ItemStack item)
	{
		return ctx().getItemManager().isLoreBook(item);
	}

	public static boolean isWerewolfCurePotion(ItemStack item)
	{
		return ctx().getItemManager().isCurePotion(item);
	}

	public static boolean isWerewolfInfectionPotion(ItemStack item)
	{
		return ctx().getItemManager().isInfectionPotion(item);
	}

	public static boolean isWerewolfWolfbanePotion(ItemStack item)
	{
		return ctx().getItemManager().isWolfbanePotion(item);
	}

	public static boolean isWerewolf(UUID playerId)
	{
		return ctx().getWerewolfManager().isWerewolf(playerId);
	}

	public static boolean isWerewolf(Player player)
	{
		return isWerewolf(player.getUniqueId());
	}

	public static boolean isVampire(Player player)
	{
		return ctx().getWerewolfManager().isVampire(player);
	}

	public static boolean isVampire(UUID playerId)
	{
		return ctx().getWerewolfManager().isVampire(playerId);
	}

	public static boolean isHuman(UUID playerId)
	{
		return ctx().getWerewolfManager().isHuman(playerId);
	}

	public static boolean isHuman(Player player)
	{
		return ctx().getWerewolfManager().isHuman(player.getUniqueId());
	}

	public static boolean isAlpha(UUID playerId)
	{
		return ctx().getClanManager().isAlpha(playerId);
	}

	public static boolean isAlpha(Player player)
	{
		return ctx().getClanManager().isAlpha(player.getUniqueId());
	}

	public static int getNumberOfWerewolves()
	{
		return ctx().getWerewolfManager().getNumberOfWerewolves();
	}

	public static ClanManager.ClanType getWerewolfClan(UUID playerId) {
		if (!isWerewolf(playerId)) {
			return null;
		}
		return ctx().getWerewolfManager().getWerewolfClan(playerId);
	}
}
