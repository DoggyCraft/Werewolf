package com.dogonfire.werewolf;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.api.WerewolfItem;
import com.dogonfire.werewolf.api.WerewolfProfile;
import com.dogonfire.werewolf.api.WerewolfService;

/**
 * The old static API, kept so plugins built against older versions keep compiling and linking. Every method delegates
 * to the {@link WerewolfService}. While Werewolf is disabled, reads return false, null or 0 and the item factories
 * return null instead of throwing, so plugins that call it during shutdown don't crash.
 *
 * @deprecated Use {@link WerewolfService} from Bukkit's ServicesManager instead.
 */
@Deprecated
public class WerewolfAPI
{
	private static volatile WerewolfService	service;

	// Set by the plugin on enable, cleared on disable
	static void setService(WerewolfService werewolfService)
	{
		service = werewolfService;
	}

	/**
	 * Makes the player a full werewolf in human form (see {@link WerewolfService#setFullWerewolf(UUID)}). Does nothing
	 * while Werewolf is disabled.
	 *
	 * @deprecated Use {@link WerewolfService#setFullWerewolf(UUID)}.
	 */
	@Deprecated
	public static void setFullWerewolf(UUID playerId)
	{
		WerewolfService s = service;
		if (s != null && playerId != null)
		{
			s.setFullWerewolf(playerId);
		}
	}

	/** Whether the Werewolf plugin is enabled; the other methods only do something while it is */
	@Deprecated
	public static boolean isEnabled()
	{
		return service != null;
	}

	private static ItemStack create(WerewolfItem item)
	{
		WerewolfService s = service;
		return s != null ? s.createItem(item) : null;
	}

	private static boolean is(ItemStack stack, WerewolfItem item)
	{
		WerewolfService s = service;
		return s != null && s.getItemType(stack).orElse(null) == item;
	}

	/** @deprecated Use {@link WerewolfService#createItem(WerewolfItem)}. Null while Werewolf is disabled. */
	@Deprecated
	public static ItemStack newWerewolfSilverSword()
	{
		return create(WerewolfItem.SILVER_SWORD);
	}

	/** @deprecated Use {@link WerewolfService#createItem(WerewolfItem)}. Null while Werewolf is disabled. */
	@Deprecated
	public static ItemStack newWerewolfLoreBook()
	{
		return create(WerewolfItem.LORE_BOOK);
	}

	/** @deprecated Use {@link WerewolfService#createItem(WerewolfItem)}. Null while Werewolf is disabled. */
	@Deprecated
	public static ItemStack newWerewolfCurePotion()
	{
		return create(WerewolfItem.CURE_POTION);
	}

	/** @deprecated Use {@link WerewolfService#createItem(WerewolfItem)}. Null while Werewolf is disabled. */
	@Deprecated
	public static ItemStack newWerewolfInfectionPotion()
	{
		return create(WerewolfItem.INFECTION_POTION);
	}

	/** @deprecated Use {@link WerewolfService#createItem(WerewolfItem)}. Null while Werewolf is disabled. */
	@Deprecated
	public static ItemStack newWerewolfWolfbanePotion()
	{
		return create(WerewolfItem.WOLFBANE_POTION);
	}

	/** @deprecated Use {@link WerewolfService#getItemType(ItemStack)}. */
	@Deprecated
	public static boolean isWerewolfSilverSword(ItemStack item)
	{
		return is(item, WerewolfItem.SILVER_SWORD);
	}

	/** @deprecated Use {@link WerewolfService#createItem(WerewolfItem)}. Null while Werewolf is disabled. */
	@Deprecated
	public static ItemStack newWerewolfTracker()
	{
		return create(WerewolfItem.WEREWOLF_TRACKER);
	}

	/**
	 * Any silver armor piece.
	 *
	 * @deprecated Use {@link WerewolfService#getItemType(ItemStack)}.
	 */
	@Deprecated
	public static boolean isWerewolfSilverArmor(ItemStack item)
	{
		WerewolfService s = service;
		return s != null && s.getItemType(item).map(WerewolfItem::isSilverArmor).orElse(false);
	}

	/** @deprecated Use {@link WerewolfService#getItemType(ItemStack)}. */
	@Deprecated
	public static boolean isWerewolfTracker(ItemStack item)
	{
		return is(item, WerewolfItem.WEREWOLF_TRACKER);
	}

	/** @deprecated Use {@link WerewolfService#getItemType(ItemStack)}. */
	@Deprecated
	public static boolean isWerewolfLoreBook(ItemStack item)
	{
		return is(item, WerewolfItem.LORE_BOOK);
	}

	/** @deprecated Use {@link WerewolfService#getItemType(ItemStack)}. */
	@Deprecated
	public static boolean isWerewolfCurePotion(ItemStack item)
	{
		return is(item, WerewolfItem.CURE_POTION);
	}

	/** @deprecated Use {@link WerewolfService#getItemType(ItemStack)}. */
	@Deprecated
	public static boolean isWerewolfInfectionPotion(ItemStack item)
	{
		return is(item, WerewolfItem.INFECTION_POTION);
	}

	/** @deprecated Use {@link WerewolfService#getItemType(ItemStack)}. */
	@Deprecated
	public static boolean isWerewolfWolfbanePotion(ItemStack item)
	{
		return is(item, WerewolfItem.WOLFBANE_POTION);
	}

	/** @deprecated Use {@link WerewolfService#isWerewolf(UUID)}. */
	@Deprecated
	public static boolean isWerewolf(UUID playerId)
	{
		WerewolfService s = service;
		return s != null && playerId != null && s.isWerewolf(playerId);
	}

	/** @deprecated Use {@link WerewolfService#isWerewolf(UUID)}. */
	@Deprecated
	public static boolean isWerewolf(Player player)
	{
		return player != null && isWerewolf(player.getUniqueId());
	}

	/** @deprecated Use {@link WerewolfService#isVampire(UUID)}. */
	@Deprecated
	public static boolean isVampire(Player player)
	{
		return player != null && isVampire(player.getUniqueId());
	}

	/** @deprecated Use {@link WerewolfService#isVampire(UUID)}. */
	@Deprecated
	public static boolean isVampire(UUID playerId)
	{
		WerewolfService s = service;
		return s != null && playerId != null && s.isVampire(playerId);
	}

	/**
	 * Neither werewolf nor vampire; false while Werewolf is disabled.
	 *
	 * @deprecated Use {@link WerewolfService#isHuman(UUID)}.
	 */
	@Deprecated
	public static boolean isHuman(UUID playerId)
	{
		WerewolfService s = service;
		return s != null && playerId != null && s.isHuman(playerId);
	}

	/** @deprecated Use {@link WerewolfService#isHuman(UUID)}. */
	@Deprecated
	public static boolean isHuman(Player player)
	{
		return player != null && isHuman(player.getUniqueId());
	}

	/** @deprecated Use {@link WerewolfService#getWerewolf(UUID)} and {@link WerewolfProfile#alpha()}. */
	@Deprecated
	public static boolean isAlpha(UUID playerId)
	{
		WerewolfService s = service;
		return s != null && playerId != null && s.getWerewolf(playerId).map(WerewolfProfile::alpha).orElse(false);
	}

	/** @deprecated Use {@link WerewolfService#getWerewolf(UUID)} and {@link WerewolfProfile#alpha()}. */
	@Deprecated
	public static boolean isAlpha(Player player)
	{
		return player != null && isAlpha(player.getUniqueId());
	}

	/** @deprecated Use {@link WerewolfService#getWerewolves()}. */
	@Deprecated
	public static int getNumberOfWerewolves()
	{
		WerewolfService s = service;
		return s != null ? s.getWerewolves().size() : 0;
	}

	/**
	 * The werewolf's clan, or null if the player is not a werewolf (or Werewolf is disabled). Returns
	 * {@link com.dogonfire.werewolf.api.Clan} now; it used to return the internal {@code ClanManager.ClanType}, which
	 * had the same constants.
	 *
	 * @deprecated Use {@link WerewolfService#getWerewolf(UUID)} and {@link WerewolfProfile#clan()}.
	 */
	@Deprecated
	public static Clan getWerewolfClan(UUID playerId)
	{
		WerewolfService s = service;
		return s != null && playerId != null ? s.getWerewolf(playerId).map(WerewolfProfile::clan).orElse(null) : null;
	}
}
