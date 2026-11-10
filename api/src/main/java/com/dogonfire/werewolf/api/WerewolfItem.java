package com.dogonfire.werewolf.api;

/**
 * The plugin's custom items. They are recognised by a tag in their persistent data ({@code werewolf:ww-item}), not by
 * name, so renamed items still work. See {@link WerewolfService#createItem(WerewolfItem)} and
 * {@link WerewolfService#getItemType(org.bukkit.inventory.ItemStack)}.
 */
public enum WerewolfItem
{
	/** Silver sword: extra damage against werewolves in wolf form */
	SILVER_SWORD,
	/** Silver armor helmet: protection against bite infections and supernatural attackers; humans only */
	SILVER_HELMET,
	/** Silver armor chestplate */
	SILVER_CHESTPLATE,
	/** Silver armor leggings */
	SILVER_LEGGINGS,
	/** Silver armor boots */
	SILVER_BOOTS,
	/** Wolfbane potion: turns a werewolf back into human form */
	WOLFBANE_POTION,
	/** Infection potion: turns the drinker into a werewolf (Potion clan) */
	INFECTION_POTION,
	/** Cure potion: cures a werewolf */
	CURE_POTION,
	/** The werewolf lore book */
	LORE_BOOK,
	/** Tracker compass that locks onto a nearby werewolf; humans only */
	WEREWOLF_TRACKER,
	/** Tracker compass that locks onto a nearby vampire (VampireRevamp); humans only */
	VAMPIRE_TRACKER;

	/** True for the four silver armor pieces */
	public boolean isSilverArmor()
	{
		return this == SILVER_HELMET || this == SILVER_CHESTPLATE || this == SILVER_LEGGINGS || this == SILVER_BOOTS;
	}
}
