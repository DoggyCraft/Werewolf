package com.dogonfire.werewolf.listeners;

import java.util.Collection;
import java.util.Random;

import com.dogonfire.werewolf.managers.DamageManager;
import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.managers.TrophyManager;
import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.disguises.WerewolfDisguiseAPI;
import com.dogonfire.werewolf.api.event.WerewolfCureEvent;
import com.dogonfire.werewolf.api.event.WerewolfTransformEvent;
import com.dogonfire.werewolf.tasks.CheckTransformationTask;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class PlayerListener implements Listener
{
	public static final String	IMMUNE_WOLFBANE	= "werewolf.immunewolfbane";

	private final Werewolf	plugin;
	private final WerewolfContext ctx;
	private Random		random	= new Random();

	public PlayerListener(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		if (!ctx.isWerewolvesEnabled())
		{
			return;
		}
		ctx.getWerewolfManager().unsetWerewolfSkin(event.getPlayer().getUniqueId(), true, WerewolfTransformEvent.Reason.FORCED);
		
		if (ctx.getWerewolfManager().isWerewolf(event.getPlayer().getUniqueId()))
		{
			// Clear wolf form effects left over from quitting (or a crash) while in wolf form
			ctx.getWolfFormManager().removeWolfForm(event.getPlayer(), true);
		}

		this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new CheckTransformationTask(ctx, event.getPlayer().getUniqueId()), 20L);
	}

	@EventHandler
	public void onPlayerWorldChange(PlayerChangedWorldEvent event)
	{
		ctx.getWerewolfManager().unsetWerewolfSkin(event.getPlayer().getUniqueId(), true, WerewolfTransformEvent.Reason.FORCED);
	}

	/*
	 * Old workaround for disguise plugins swallowing hits on disguised players: an arm swing aimed at a nearby werewolf
	 * damages it. The built-in and SkinsRestorer backends only change the player's skin, so real hits always land and
	 * this would double-hit; it is only kept for LibsDisguises, and skipped while the werewolf still has invulnerability
	 * frames from a hit that did land.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerAnimation(PlayerAnimationEvent event)
	{
		if (!ctx.isWerewolvesEnabled() || ctx.getDisguises().getBackend() != WerewolfDisguiseAPI.Backend.LIBSDISGUISES)
		{
			return;
		}

		if (event.getAnimationType() != PlayerAnimationType.ARM_SWING)
		{
			return;
		}

		Player nearestWerewolf = ctx.getWerewolfManager().getNearestWerewolf(event.getPlayer().getUniqueId());
		if (nearestWerewolf != null && nearestWerewolf.getNoDamageTicks() <= nearestWerewolf.getMaximumNoDamageTicks() / 2 && DamageManager.canPlayerHit(event.getPlayer(), nearestWerewolf.getLocation()))
		{
			double damage = DamageManager.getDamageFromItemInHand(event.getPlayer());

			plugin.logDebug("Doing " + damage + " damage to " + nearestWerewolf.getName());

			nearestWerewolf.damage(damage, event.getPlayer());
		}
	}

	@EventHandler
	public void onFlightAttempt(PlayerToggleFlightEvent event)
	{
		Player player = event.getPlayer();
		if (!ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event)
	{
		if (!ctx.isWerewolvesEnabled())
		{
			return;
		}
		if (!ctx.getWerewolfManager().hasWerewolfSkin(event.getPlayer().getUniqueId()))
		{
			return;
		}
		Player player = event.getPlayer();
		
		if (Math.random() < 0.05D)
		{
			for (Entity entity : player.getNearbyEntities(this.plugin.getSettings().wolfForm().wolfDistance(), this.plugin.getSettings().wolfForm().wolfDistance(), this.plugin.getSettings().wolfForm().wolfDistance()))
			{
				if (entity instanceof Wolf)
				{
					Wolf wolf = (Wolf) entity;
					if ((!wolf.isTamed()) || (wolf.getOwner() == null))
					{
						player.sendMessage(ChatColor.LIGHT_PURPLE + "A wild wolf joins your pack!");

						double maxHealth = 20;
						AttributeInstance healthAttribute = wolf.getAttribute(Attribute.MAX_HEALTH);
						healthAttribute.setBaseValue(maxHealth);
						wolf.setHealth(maxHealth);
						wolf.setOwner(player);
						wolf.setTamed(true);

					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		if (event.getEntity().getKiller() == null || event.getEntity().getKiller().getUniqueId() == null)
		{
			if (this.plugin.getSettings().general().renameWerewolves() && ctx.getWerewolfManager().hasWerewolfSkin(event.getEntity().getUniqueId()))
			{
				String werewolfName = "Werewolf";
				if (plugin.getSettings().names().enabled()) {
					werewolfName = ctx.getWerewolfManager().getWerewolfName(event.getEntity().getUniqueId());
				}
				switch (this.random.nextInt(3))
				{
					case 0:
						event.setDeathMessage(werewolfName + " died");
						break;
					case 1:
						event.setDeathMessage(werewolfName + " was killed");
						break;
					case 2:
						event.setDeathMessage(werewolfName + " died");
						break;
					default:
						break;
				}
			}
			return;
		}

		String killerName = event.getEntity().getKiller().getName();
		String victimName = event.getEntity().getName();

		if (this.plugin.getSettings().general().renameWerewolves())
		{
			if (ctx.getWerewolfManager().hasWerewolfSkin(event.getEntity().getKiller().getUniqueId()))
			{
				String werewolfName = "Werewolf";
				if (plugin.getSettings().names().enabled()) {
					werewolfName = ctx.getWerewolfManager().getWerewolfName(event.getEntity().getKiller().getUniqueId());
				}
				switch (this.random.nextInt(4))
				{
					case 0:
						event.setDeathMessage(victimName + " was slaughtered by " + werewolfName);
						break;
					case 1:
						event.setDeathMessage(victimName + " was ripped apart by " + werewolfName);
						break;
					case 2:
						event.setDeathMessage(victimName + " was torn to pieces by " + werewolfName);
						break;
					case 3:
						event.setDeathMessage(victimName + " was eaten by " + werewolfName);
						break;
					default:
						break;
				}
			}
			else if (ctx.getWerewolfManager().hasWerewolfSkin(event.getEntity().getUniqueId()))
			{
				String werewolfName = "Werewolf";
				if (plugin.getSettings().names().enabled()) {
					werewolfName = ctx.getWerewolfManager().getWerewolfName(event.getEntity().getUniqueId());
				}
				if (killerName != null)
				{
					event.setDeathMessage(werewolfName + " was slain by " + killerName);
				}
				else
				{
					event.setDeathMessage(werewolfName + " died");
				}
			}
		}

		TrophyManager trophyManager = ctx.getTrophyManager();
		if (trophyManager != null && ctx.getWerewolfManager().hasWerewolfSkin(event.getEntity().getUniqueId()))
		{
			Player killer = this.plugin.getServer().getPlayer(killerName);

			if (killer != null)
			{
				switch (killer.getInventory().getItemInMainHand().getType())
				{
					case NETHERITE_SWORD:
					case DIAMOND_SWORD:
					case GOLDEN_SWORD:
					case IRON_SWORD:
					case STONE_SWORD:
					case WOODEN_SWORD:
					{
						org.bukkit.inventory.ItemStack trophy = trophyManager.getTrophyFromWerewolfPlayer(killer.getUniqueId(), event.getEntity().getUniqueId());
						event.getDrops().add(trophy);

						event.getEntity().getKiller().sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouCutTheHeadOfWerewolf, ChatColor.WHITE));
					} break;

					default:
					  break;
				}
			}
		}

		if (ctx.getWerewolfManager().hasWerewolfSkin(event.getEntity().getUniqueId()))
		{
			if (this.plugin.getSettings().general().cureWerewolfWhenSlain())
			{
				ctx.getWerewolfManager().unmakeWerewolf(event.getEntity().getUniqueId(), WerewolfCureEvent.Cause.SLAIN);
			}
			else
			{
				ctx.getWerewolfManager().unsetWerewolfSkin(event.getEntity().getUniqueId(), false, WerewolfTransformEvent.Reason.FORCED);

				ctx.getWerewolfManager().setInfectedThisNight(event.getEntity().getUniqueId(), event.getEntity().getName(), true);
			}
		}
	}
	
	@EventHandler
	public void onPotionSplash(PotionSplashEvent event)
	{
		if (!ctx.getItemManager().isWolfbanePotion(event.getPotion().getItem())) 
		{
			return;
		}
		
		Collection<LivingEntity> affectedEntity = event.getAffectedEntities();
		for (LivingEntity livingEntity : affectedEntity)
		{
			if (livingEntity instanceof Player)
			{
				Player player = (Player) livingEntity;

				if (player.hasPermission(IMMUNE_WOLFBANE) || !ctx.getWerewolfManager().isWerewolf(player))
				{
					continue;
				}

				// In human form, wolfbane holds the beast back: the /ww transform cooldown starts over
				if (ctx.getWerewolfManager().isFullWerewolf(player.getUniqueId()) && !ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
				{
					ctx.getWerewolfManager().restartTransformCooldown(player.getUniqueId());
					if (plugin.getSettings().cooldowns().transform() > 0)
					{
						ctx.getLanguageManager().setAmount("" + plugin.getSettings().cooldowns().transform());
						player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WolfbaneHumanForm, ChatColor.LIGHT_PURPLE));
					}
					continue;
				}

				if (ctx.getWerewolfManager().isWolfForm(player.getUniqueId()) && random.nextInt(100) < plugin.getSettings().items().wolfbaneUntransformChance())
				{
					this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 4 * 20, 2, false, false)), 16L);
					
					player.spawnParticle(Particle.FIREWORK, player.getLocation(), 100);
					DustOptions dustOptions = new DustOptions(Color.BLACK, 1);
					player.spawnParticle(Particle.DUST, player.getLocation().add(new Vector(0, 1, 0)), 100, dustOptions);
					player.spawnParticle(Particle.DUST, player.getLocation().add(new Vector(0, 2, 0)), 100, dustOptions);

					ctx.getWerewolfManager().untransform(player, WerewolfTransformEvent.Reason.WOLFBANE);
				}
			}
		}

		event.setCancelled(true);
	}
	
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event)
	{
		if (ctx.getWerewolfManager().isWolfForm(event.getPlayer().getUniqueId()))
		{
			ctx.getWerewolfManager().disguiseWerewolf(event.getPlayer());
		}
	}
}
