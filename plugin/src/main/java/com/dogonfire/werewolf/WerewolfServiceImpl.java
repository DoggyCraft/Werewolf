package com.dogonfire.werewolf;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.werewolf.api.ActionResult;
import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.api.MoonPhase;
import com.dogonfire.werewolf.api.WerewolfItem;
import com.dogonfire.werewolf.api.WerewolfProfile;
import com.dogonfire.werewolf.api.WerewolfService;
import com.dogonfire.werewolf.api.event.NewAlphaEvent;
import com.dogonfire.werewolf.api.event.WerewolfCureEvent;
import com.dogonfire.werewolf.api.event.WerewolfInfectionEvent;
import com.dogonfire.werewolf.api.event.WerewolfLevelChangeEvent;
import com.dogonfire.werewolf.api.event.WerewolfTransformEvent;
import com.dogonfire.werewolf.managers.WerewolfManager;

/**
 * The {@link WerewolfService} registered in Bukkit's ServicesManager while the plugin is enabled. Reads that may come
 * from other threads use WerewolfManager's profile snapshot; actions check the preconditions here (to tell the caller
 * why nothing happened) and then go through the same choke points as the game, with the API reason/cause.
 */
final class WerewolfServiceImpl implements WerewolfService
{
	private final WerewolfContext ctx;

	WerewolfServiceImpl(WerewolfContext ctx)
	{
		this.ctx = ctx;
	}

	private WerewolfManager werewolves()
	{
		return this.ctx.getWerewolfManager();
	}

	private void checkMainThread()
	{
		if (!this.ctx.getPlugin().getServer().isPrimaryThread())
		{
			throw new IllegalStateException("WerewolfService actions must be called on the main thread");
		}
	}

	// Reads

	@Override
	public Optional<WerewolfProfile> getWerewolf(UUID playerId)
	{
		return Optional.ofNullable(werewolves().getProfile(playerId));
	}

	@Override
	public boolean isWerewolf(UUID playerId)
	{
		return werewolves().getProfile(playerId) != null;
	}

	@Override
	public boolean isInWolfForm(UUID playerId)
	{
		WerewolfProfile profile = werewolves().getProfile(playerId);
		return profile != null && profile.isInWolfForm();
	}

	@Override
	public boolean isVampire(UUID playerId)
	{
		return playerId != null && werewolves().isVampire(playerId);
	}

	@Override
	public boolean isHuman(UUID playerId)
	{
		return playerId != null && werewolves().isHuman(playerId);
	}

	@Override
	public Set<UUID> getWerewolves()
	{
		Set<UUID> result = new HashSet<UUID>();
		for (WerewolfProfile profile : werewolves().getProfiles())
		{
			result.add(profile.uuid());
		}
		return result;
	}

	@Override
	public Set<UUID> getWerewolves(Clan clan)
	{
		Set<UUID> result = new HashSet<UUID>();
		for (WerewolfProfile profile : werewolves().getProfiles())
		{
			if (profile.clan() == clan)
			{
				result.add(profile.uuid());
			}
		}
		return result;
	}

	@Override
	public Optional<UUID> getAlpha(Clan clan)
	{
		for (WerewolfProfile profile : werewolves().getProfiles())
		{
			if (profile.alpha() && profile.clan() == clan)
			{
				return Optional.of(profile.uuid());
			}
		}
		return Optional.empty();
	}

	@Override
	public String getClanName(Clan clan)
	{
		return this.ctx.getClanManager().getClanName(Objects.requireNonNull(clan, "clan"));
	}

	@Override
	public MoonPhase getMoonPhase(World world)
	{
		return this.ctx.getMoonManager().getMoonPhase(Objects.requireNonNull(world, "world"));
	}

	@Override
	public boolean isTransformationNight(World world)
	{
		return this.ctx.getMoonManager().isTransformationNight(Objects.requireNonNull(world, "world"));
	}

	@Override
	public boolean isWerewolvesEnabled()
	{
		return this.ctx.isWerewolvesEnabled();
	}

	// Actions

	@Override
	public ActionResult infect(Player player, Clan clan, boolean turnNow)
	{
		checkMainThread();
		Objects.requireNonNull(player, "player");
		if (werewolves().isWerewolf(player))
		{
			return ActionResult.ALREADY_WEREWOLF;
		}
		return werewolves().makeWerewolf(player, turnNow, clan, WerewolfInfectionEvent.Source.API) ? ActionResult.SUCCESS : ActionResult.CANCELLED;
	}

	@Override
	public ActionResult setFullWerewolf(UUID playerId)
	{
		checkMainThread();
		Objects.requireNonNull(playerId, "playerId");
		if (!werewolves().isWerewolf(playerId))
		{
			Player player = this.ctx.getPlugin().getServer().getPlayer(playerId);
			if (player == null)
			{
				return ActionResult.PLAYER_OFFLINE;
			}
			ActionResult infected = infect(player, null, false);
			if (!infected.isSuccess())
			{
				return infected;
			}
		}
		return werewolves().makeFullWerewolf(playerId) ? ActionResult.SUCCESS : ActionResult.CANCELLED;
	}

	@Override
	public ActionResult cure(UUID playerId)
	{
		checkMainThread();
		Objects.requireNonNull(playerId, "playerId");
		if (!werewolves().isWerewolf(playerId))
		{
			return ActionResult.NOT_WEREWOLF;
		}
		return werewolves().unmakeWerewolf(playerId, WerewolfCureEvent.Cause.API) ? ActionResult.SUCCESS : ActionResult.CANCELLED;
	}

	@Override
	public ActionResult transform(Player player)
	{
		checkMainThread();
		Objects.requireNonNull(player, "player");
		UUID playerId = player.getUniqueId();
		if (!this.ctx.isWerewolvesEnabled())
		{
			return ActionResult.DISABLED;
		}
		if (!werewolves().isWerewolf(playerId))
		{
			return ActionResult.NOT_WEREWOLF;
		}
		if (werewolves().hasWerewolfSkin(playerId))
		{
			return ActionResult.ALREADY_IN_FORM;
		}
		if (!player.isOnline() || !werewolves().isWerewolvesAllowedInWorld(player) || werewolves().isInfectedThisNight(playerId))
		{
			return ActionResult.NOT_ALLOWED;
		}
		return werewolves().transform(player, WerewolfTransformEvent.Reason.API) ? ActionResult.SUCCESS : ActionResult.CANCELLED;
	}

	@Override
	public ActionResult untransform(Player player)
	{
		checkMainThread();
		Objects.requireNonNull(player, "player");
		UUID playerId = player.getUniqueId();
		if (!werewolves().isWerewolf(playerId))
		{
			return ActionResult.NOT_WEREWOLF;
		}
		if (!werewolves().hasWerewolfSkin(playerId))
		{
			return ActionResult.ALREADY_IN_FORM;
		}
		return werewolves().untransform(player, WerewolfTransformEvent.Reason.API) ? ActionResult.SUCCESS : ActionResult.CANCELLED;
	}

	@Override
	public ActionResult setLevel(UUID playerId, int level)
	{
		checkMainThread();
		Objects.requireNonNull(playerId, "playerId");
		if (!werewolves().isWerewolf(playerId))
		{
			return ActionResult.NOT_WEREWOLF;
		}
		return werewolves().setLevel(playerId, level, WerewolfLevelChangeEvent.Cause.API) ? ActionResult.SUCCESS : ActionResult.CANCELLED;
	}

	@Override
	public ActionResult setAlpha(Clan clan, UUID playerId)
	{
		checkMainThread();
		Objects.requireNonNull(clan, "clan");
		Objects.requireNonNull(playerId, "playerId");
		if (!werewolves().isWerewolf(playerId))
		{
			return ActionResult.NOT_WEREWOLF;
		}
		if (werewolves().getWerewolfClan(playerId) != clan)
		{
			return ActionResult.WRONG_CLAN;
		}
		return this.ctx.getClanManager().assignAlphaInClan(clan, playerId, NewAlphaEvent.Cause.API) ? ActionResult.SUCCESS : ActionResult.CANCELLED;
	}

	// Items

	@Override
	public ItemStack createItem(WerewolfItem item)
	{
		checkMainThread();
		return this.ctx.getItemManager().newItem(Objects.requireNonNull(item, "item"));
	}

	@Override
	public Optional<WerewolfItem> getItemType(ItemStack item)
	{
		return Optional.ofNullable(this.ctx.getItemManager().getItemType(item));
	}
}
