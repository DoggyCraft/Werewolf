package com.dogonfire.werewolf.api;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.werewolf.api.event.NewAlphaEvent;
import com.dogonfire.werewolf.api.event.WerewolfCureEvent;
import com.dogonfire.werewolf.api.event.WerewolfInfectionEvent;
import com.dogonfire.werewolf.api.event.WerewolfLevelChangeEvent;
import com.dogonfire.werewolf.api.event.WerewolfTransformEvent;

/**
 * The Werewolf plugin's public API. Werewolf registers it in Bukkit's {@link org.bukkit.plugin.ServicesManager} when
 * it enables and unregisters it when it disables:
 *
 * <pre>{@code
 * RegisteredServiceProvider<WerewolfService> rsp = Bukkit.getServicesManager().getRegistration(WerewolfService.class);
 * WerewolfService werewolves = rsp != null ? rsp.getProvider() : null;
 * }</pre>
 *
 * Look the service up again after Werewolf is reloaded or re-enabled, or don't keep it beyond your own plugin's
 * lifetime; calling a service whose plugin has been disabled is not supported.
 * <p>
 * <b>Threading.</b> Call everything on the server's main thread, except the methods documented as thread-safe
 * ({@link #getWerewolf}, {@link #isWerewolf}, {@link #isInWolfForm}, {@link #getWerewolves()},
 * {@link #getWerewolves(Clan)}, {@link #getAlpha} and {@link #getClanName}), which read a snapshot that is updated on
 * the main thread whenever a werewolf changes. Actions called from another thread throw
 * {@link IllegalStateException}.
 * <p>
 * <b>Actions</b> go through the same code as the game itself and fire the same events with an {@code API}
 * reason/source/cause ({@link WerewolfInfectionEvent.Source#API}, {@link WerewolfCureEvent.Cause#API},
 * {@link WerewolfTransformEvent.Reason#API}, {@link WerewolfLevelChangeEvent.Cause#API}, {@link NewAlphaEvent.Cause#API}),
 * so other listeners can veto them. A vetoed action changes nothing and returns {@link ActionResult#CANCELLED}.
 */
public interface WerewolfService
{
	// Reads

	/**
	 * A snapshot of the werewolf (infected or full), online or offline. Thread-safe.
	 *
	 * @return the werewolf's profile, or empty if the player is not a werewolf
	 */
	Optional<WerewolfProfile> getWerewolf(UUID playerId);

	/** Whether the player is a werewolf (infected or full), online or offline. Thread-safe. */
	boolean isWerewolf(UUID playerId);

	/** Whether the player is a werewolf in wolf form right now. Thread-safe. */
	boolean isInWolfForm(UUID playerId);

	/**
	 * Whether the player is a vampire (VampireRevamp). Always false when VampireRevamp is not installed, and for
	 * offline players, since VampireRevamp only knows online players. Main thread only.
	 */
	boolean isVampire(UUID playerId);

	/** Whether the player is neither a werewolf nor a vampire. Main thread only. */
	boolean isHuman(UUID playerId);

	/** Every werewolf (infected or full), online or offline. Thread-safe; the set is a copy. */
	Set<UUID> getWerewolves();

	/** Every werewolf in the clan, online or offline. Thread-safe; the set is a copy. */
	Set<UUID> getWerewolves(Clan clan);

	/** The clan's alpha, or empty if it has none. Thread-safe. */
	Optional<UUID> getAlpha(Clan clan);

	/** The clan's display name from config.yml ({@code Clans.<Clan>Name}). Thread-safe. */
	String getClanName(Clan clan);

	/** The world's current moon phase. Main thread only. */
	MoonPhase getMoonPhase(World world);

	/**
	 * Whether the moon forces werewolves under the open sky to transform in this world right now: a full moon night,
	 * or any night when {@code Settings.OnlyTransformDuringFullMoon} is off. Main thread only.
	 */
	boolean isTransformationNight(World world);

	/**
	 * Whether werewolves are switched on (/ww on and /ww off). While they are off, nobody transforms and most
	 * werewolf features do nothing; {@link #transform} returns {@link ActionResult#DISABLED}.
	 */
	boolean isWerewolvesEnabled();

	// Actions (main thread only)

	/**
	 * Makes the player a werewolf. Fires a {@link WerewolfInfectionEvent} with {@link WerewolfInfectionEvent.Source#API}
	 * and a chance of 1; listeners may change the clan, lower the chance or cancel.
	 *
	 * @param player the player, who must not be a werewolf yet
	 * @param clan the clan to join, or null for a random one
	 * @param turnNow true to make them a full werewolf in wolf form right away (when werewolves are switched on), false
	 *            to infect them so they turn on the next transformation night
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#ALREADY_WEREWOLF}, or {@link ActionResult#CANCELLED}
	 *         when a listener cancelled the event or the lowered chance failed
	 */
	ActionResult infect(Player player, Clan clan, boolean turnNow);

	/**
	 * Makes the player a full werewolf in human form. An infected werewolf becomes full without waiting for a
	 * transformation night (no event is fired for that step). A player who is not a werewolf yet must be online: they
	 * are infected first through {@link #infect(Player, Clan, boolean)} (random clan), which can be vetoed.
	 *
	 * @return {@link ActionResult#SUCCESS} (also when already full), {@link ActionResult#PLAYER_OFFLINE} or
	 *         {@link ActionResult#CANCELLED}
	 */
	ActionResult setFullWerewolf(UUID playerId);

	/**
	 * Cures the werewolf, online or offline: its werewolf data is removed and, if it is in wolf form, it turns back
	 * into a human a moment later. Fires a {@link WerewolfCureEvent} with {@link WerewolfCureEvent.Cause#API}.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NOT_WEREWOLF} or {@link ActionResult#CANCELLED}
	 */
	ActionResult cure(UUID playerId);

	/**
	 * Turns the werewolf into wolf form. Fires a {@link WerewolfTransformEvent} with
	 * {@link WerewolfTransformEvent.Reason#API}; the skin and wolf form effects are applied a tick later. The wolf form
	 * lasts until {@link #untransform(Player)} is called or it is ended otherwise (wolfbane, death, quitting, /ww off):
	 * it is not reverted at morning and has no time limit.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NOT_WEREWOLF}, {@link ActionResult#ALREADY_IN_FORM},
	 *         {@link ActionResult#NOT_ALLOWED} (world not in {@code AllowedWorlds}, or infected this night),
	 *         {@link ActionResult#DISABLED} or {@link ActionResult#CANCELLED}
	 */
	ActionResult transform(Player player);

	/**
	 * Turns the werewolf back into human form. Fires a {@link WerewolfTransformEvent} with
	 * {@link WerewolfTransformEvent.Reason#API}; the skin changes back a moment later. A werewolf still outside on a
	 * transformation night will be turned again by the moon.
	 *
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NOT_WEREWOLF}, {@link ActionResult#ALREADY_IN_FORM} or
	 *         {@link ActionResult#CANCELLED}
	 */
	ActionResult untransform(Player player);

	/**
	 * Sets the werewolf's level, online or offline. Fires a {@link WerewolfLevelChangeEvent} with
	 * {@link WerewolfLevelChangeEvent.Cause#API} (not when the level is unchanged); listeners may change the new level.
	 *
	 * @param level the new level; negative values become 0
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NOT_WEREWOLF} or {@link ActionResult#CANCELLED}
	 */
	ActionResult setLevel(UUID playerId, int level);

	/**
	 * Makes the werewolf its clan's alpha, online or offline. Fires a {@link NewAlphaEvent} with
	 * {@link NewAlphaEvent.Cause#API} (not when it already is the alpha).
	 *
	 * @param clan the clan, which must be the werewolf's own clan
	 * @return {@link ActionResult#SUCCESS}, {@link ActionResult#NOT_WEREWOLF}, {@link ActionResult#WRONG_CLAN} or
	 *         {@link ActionResult#CANCELLED}
	 */
	ActionResult setAlpha(Clan clan, UUID playerId);

	// Items (main thread only)

	/** A new stack of one of the custom items, with its name and lore in the server's language */
	ItemStack createItem(WerewolfItem item);

	/**
	 * Which custom item the stack is, recognised by its tag (not its name).
	 *
	 * @return the item type, or empty for null, air and any other item
	 */
	Optional<WerewolfItem> getItemType(ItemStack item);
}
