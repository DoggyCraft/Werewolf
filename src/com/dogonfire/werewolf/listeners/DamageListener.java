package com.dogonfire.werewolf.listeners;

import java.util.Random;

import com.dogonfire.werewolf.WerewolfAPI;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;

import com.dogonfire.werewolf.managers.ClanManager;
import com.dogonfire.werewolf.managers.DamageManager;
import com.dogonfire.werewolf.managers.WolfFormManager;
import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.events.WerewolfInfectionEvent;
import com.dogonfire.werewolf.disguises.IWerewolfDisguiseFactory.WerewolfDisguise;

public class DamageListener implements Listener
{
	private Werewolf plugin;

	public DamageListener(Werewolf plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler
	public void onEntityDamage1(EntityDamageEvent event)
	{
		if (!Werewolf.pluginEnabled)
		{
			return;
		}

		if (!event.isCancelled() && (event.getEntity() instanceof Player))
		{
			Player player = (Player) event.getEntity();

			// Before trying to use Disguises, check if Disguises is
			// enabled...
			if (Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()) && plugin.disguisesEnabled)
			{
				WerewolfDisguise skin = Werewolf.getSkinManager().getSkin(player);

				if (skin != null)
				{
					Werewolf.getWerewolfManager().setPouncing(player.getUniqueId());
				}
				else
				{
					this.plugin.logDebug("onEntityDamage: Skin is null for " + player.getName());
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage2(EntityDamageEvent event)
	{
		if (!Werewolf.pluginEnabled)
		{
			return;
		}

		if (event.getEntity() instanceof Player)
		{
			Player werewolfPlayer = (Player) event.getEntity();

			if (Werewolf.getWerewolfManager().hasWerewolfSkin(werewolfPlayer.getUniqueId()))
			{
				double damage = event.getDamage();
				WolfFormManager.Stats stats = Werewolf.getWolfFormManager().getStats(werewolfPlayer.getUniqueId());

				// Gold weapons cut through a werewolf's natural defence until it is mature enough to be immune
				boolean goldHit = isGoldWeaponHit(event) && !Werewolf.getWerewolfManager().isGoldImmune(werewolfPlayer.getUniqueId());
				boolean silverHit = isSilverSwordHit(event);
				boolean bypassDefense = goldHit || (silverHit && this.plugin.silverSwordBypassesDefense);

				switch (event.getCause())
				{
					case FALL:
						damage = 0;
						break;
					case CONTACT:
					case LAVA:
                    case FIRE:
					case LIGHTNING:
					case BLOCK_EXPLOSION:
					case ENTITY_ATTACK:
                    case ENTITY_SWEEP_ATTACK:
                    case PROJECTILE:
                    case THORNS:
                    case FREEZE:
						// Per-clan natural defence (alphas have their own, lower value)
						if (!bypassDefense)
						{
							damage *= stats.defense;
						}
						break;

					default:
						break;
				}

				if (silverHit)
				{
					damage *= DamageManager.SilverSwordMultiplier;
				}

				event.setDamage(damage);

				if (damage == 0.0D)
				{
					event.setCancelled(true);
					return;
				}

				try
				{
					WerewolfDisguise skin = Werewolf.getSkinManager().getSkin(werewolfPlayer);

					if (skin != null)
					{
						werewolfPlayer.playEffect(EntityEffect.HURT);

						Werewolf.getWerewolfManager().growl(werewolfPlayer);
					}
				}
				catch (NullPointerException e)
				{
					// well, looks like there is no Lib's Disguises there
					werewolfPlayer.playEffect(EntityEffect.HURT);

					Werewolf.getWerewolfManager().growl(werewolfPlayer);
				}
			}
		}

		if (event instanceof EntityDamageByEntityEvent)
		{
			EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
			if (damageEvent.getDamager() instanceof Player)
			{
				Player player = (Player) damageEvent.getDamager();

				this.plugin.logDebug(player.getName() + " is doing damage to " + damageEvent.getEntityType().name());

				if (Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
				{
					WolfFormManager.Stats stats = Werewolf.getWolfFormManager().getStats(player.getUniqueId());
					double damage;

					if (player.getInventory().getItemInMainHand().getType().equals(Material.AIR))
					{
						damage = stats.fistDamage;
					}
					else
					{
						damage = stats.itemDamage;
					}

					event.setDamage(damage);
				}
			}

			applySilverArmor(damageEvent);
		}

		if (event.getDamage() <= 0.0D)
		{
			event.setDamage(1.0D);
		}
	}

	/*
	 * Bite infections only happen for hits that actually land: MONITOR with ignoreCancelled, so hits cancelled by
	 * region/PvP plugins (WorldGuard no-PvP regions, PvPManager protection...) at any priority don't infect.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBite(EntityDamageByEntityEvent event)
	{
		if (!Werewolf.pluginEnabled || !(event.getEntity() instanceof Player))
		{
			return;
		}

		Player victim = (Player) event.getEntity();

		if (event.getDamager() instanceof Player)
		{
			Player player = (Player) event.getDamager();

			if (Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()) && !WerewolfAPI.isVampire(victim))
			{
				if (Werewolf.getPermissionsManager().hasPermission(player, "werewolf.infectother") && Werewolf.getPermissionsManager().hasPermission(victim, "werewolf.becomeinfected") && !Werewolf.getWerewolfManager().isWerewolf(victim))
				{
					this.plugin.logDebug(player.getName() + " is doing risk damage to " + victim.getName());

					if (Math.random() < Werewolf.getWerewolfManager().getBiteInfectionRisk(player.getUniqueId()) && Werewolf.getWerewolfManager().makeWerewolf(victim, false, Werewolf.getWerewolfManager().getWerewolfClan(player.getUniqueId()), WerewolfInfectionEvent.Source.WEREWOLF_BITE))
					{
						Werewolf.getLanguageManager().setPlayerName(player.getName());
						victim.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.BiteVictim, ChatColor.LIGHT_PURPLE));

						Werewolf.getLanguageManager().setPlayerName(victim.getName());
						player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.BiteAttacker, ChatColor.LIGHT_PURPLE));

						victim.getLocation().getWorld().playEffect(player.getLocation(), Effect.SMOKE, 100);
						victim.getLocation().getWorld().playEffect(player.getLocation().add(new Vector(0, 1, 0)), Effect.SMOKE, 100);
						victim.getLocation().getWorld().playEffect(player.getLocation().add(new Vector(0, 2, 0)), Effect.SMOKE, 100);

						this.plugin.log(player.getName() + " infected " + victim.getName() + " with the werewolf infection!");
					}
				}
			}
		}

		if (!Werewolf.getWerewolfManager().hasWerewolfSkin(victim.getUniqueId()) && (Werewolf.getPermissionsManager().hasPermission(victim, "werewolf.becomeinfected") || victim.isOp())
				&& (!Werewolf.getWerewolfManager().isWerewolf(victim) && (event.getDamager() instanceof Wolf) && Math.random() < this.plugin.wildWolfInfectionRisk))
		{
			Tameable tameable = (Tameable) event.getDamager();

			if (!tameable.isTamed())
			{
				Random random = new Random();
				if (Werewolf.getWerewolfManager().makeWerewolf(victim, false, ClanManager.ClanType.values()[random.nextInt(ClanManager.ClanType.values().length)], WerewolfInfectionEvent.Source.WILD_WOLF_BITE))
				{
					victim.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.BiteVictim, ChatColor.LIGHT_PURPLE));

					plugin.log(victim.getName() + " contracted the werewolf infection from a wild wolf!");
				}
			}
		}
	}

	private boolean isGoldWeaponHit(EntityDamageEvent event)
	{
		if (!(event instanceof EntityDamageByEntityEvent))
		{
			return false;
		}

		Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
		if (!(damager instanceof LivingEntity) || ((LivingEntity) damager).getEquipment() == null)
		{
			return false;
		}

		switch (((LivingEntity) damager).getEquipment().getItemInMainHand().getType())
		{
			case GOLDEN_SWORD:
			case GOLDEN_AXE:
			case GOLDEN_SPEAR:
				return true;
			default:
				return false;
		}
	}

	/** A wolf form werewolf hit by a silver sword: takes the silver multiplier and (by default) loses its natural defence */
	private boolean isSilverSwordHit(EntityDamageEvent event)
	{
		if (!(event instanceof EntityDamageByEntityEvent) || (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK))
		{
			return false;
		}

		Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
		if (!(damager instanceof LivingEntity) || ((LivingEntity) damager).getEquipment() == null)
		{
			return false;
		}

		return Werewolf.getItemManager().isSilverSword(((LivingEntity) damager).getEquipment().getItemInMainHand());
	}

	/** Silver armor takes a % off attacks by werewolves (in either form) and vampires */
	private void applySilverArmor(EntityDamageByEntityEvent event)
	{
		if (!(event.getEntity() instanceof Player))
		{
			return;
		}

		Player victim = (Player) event.getEntity();
		Player attacker = null;
		if (event.getDamager() instanceof Player)
		{
			attacker = (Player) event.getDamager();
		}
		else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player)
		{
			attacker = (Player) ((Projectile) event.getDamager()).getShooter();
		}

		if (attacker == null || attacker.equals(victim) || !WerewolfAPI.isHuman(victim))
		{
			return;
		}

		if (!Werewolf.getWerewolfManager().isWerewolf(attacker) && !WerewolfAPI.isVampire(attacker))
		{
			return;
		}

		int defense = Werewolf.getItemManager().getSilverArmorDefense(victim);
		if (defense > 0)
		{
			event.setDamage(event.getDamage() * (1.0D - defense / 100.0D));
		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event)
	{
		if (event.getEntity().getKiller() == null)
		{
			return;
		}
		if (!(event.getEntity().getKiller() instanceof Player))
		{
			return;
		}

		Player killer = event.getEntity().getKiller();

		if (Werewolf.getWerewolfManager().hasWerewolfSkin(killer.getUniqueId()))
		{
			int health = 0;
			switch (event.getEntity().getType())
			{
                case WARDEN:
                case ENDER_DRAGON:
                case WITHER:
                    health = 5;
                    break;
				case PLAYER:
				case VILLAGER:
				case WANDERING_TRADER:
				case PILLAGER:
				case VINDICATOR:
				case EVOKER:
				case ILLUSIONER:
				case RAVAGER:
				case HORSE:
				case DONKEY:
				case MULE:
				case COW:
				case MOOSHROOM:
				case ZOGLIN:
				case PIGLIN_BRUTE:
				case POLAR_BEAR:
                case ELDER_GUARDIAN:
					health = 4;
					break;
				case ENDERMAN:
				case ZOMBIE:
				case ZOMBIE_VILLAGER:
				case LLAMA:
				case TRADER_LLAMA:
				case PANDA:
				case DROWNED:
				case HUSK:
				case DOLPHIN:
				case PIGLIN:
				case STRIDER:
				case ZOMBIFIED_PIGLIN:
				case WITCH:
                case GUARDIAN:
                case CAMEL:
                case SNIFFER:
					health = 3;
					break;
				case SHEEP:
				case GOAT:
				case WOLF:
				case CAT:
				case OCELOT:
				case RABBIT:
				case PIG:
				case TURTLE:
				case FOX:
				case SPIDER:
                case SQUID:
                case GLOW_SQUID:
                case PHANTOM:
					health = 2;
					break;
                case BLAZE:
				case STRAY:
				case CHICKEN:
				case PARROT:
				case AXOLOTL:
				case BEE:
				case COD:
				case SALMON:
				case PUFFERFISH:
				case TROPICAL_FISH:
                case FROG:
                case TADPOLE:
                case BAT:
					health = 1;
					break;
				case ALLAY:
				case ENDERMITE:
				case SKELETON:
				case WITHER_SKELETON:
				case SNOW_GOLEM:
				case MAGMA_CUBE:
				case SILVERFISH:
				case VEX:
				default:
					break;
			}

			if (health > 0 && killer.getHealth() < killer.getAttribute(Attribute.MAX_HEALTH).getValue())
			{
				Werewolf.getLanguageManager().setAmount("" + health);
				killer.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.KilledMob, ChatColor.LIGHT_PURPLE));

				if (killer.getHealth() + health > killer.getAttribute(Attribute.MAX_HEALTH).getValue())
				{
					killer.setHealth(killer.getAttribute(Attribute.MAX_HEALTH).getValue());
				}
				else
				{
					killer.setHealth(killer.getHealth() + health);
				}
			}

			if (this.plugin.useClans)
			{
				ClanManager.ClanType clan = Werewolf.getWerewolfManager().getWerewolfClan(killer.getUniqueId());
				Werewolf.getClanManager().handleMobKill(killer, clan, event.getEntity().getType());
			}

			if (event.getEntity() instanceof Player)
			{
				 if(WerewolfAPI.isVampire((Player)event.getEntity()))
				 {
					 Werewolf.getWerewolfScoreboardManager().setVampireKillsForPlayer(killer,
							 Werewolf.getStatisticsManager().increaseVampireKills(killer.getUniqueId()));
				 }
				 else
				 {
					 Werewolf.getWerewolfScoreboardManager().setHumanKillsForPlayer(killer,
							 Werewolf.getStatisticsManager().increaseHumanKills(killer.getUniqueId()));
				 }
			}
			else
			{
				Werewolf.getWerewolfScoreboardManager().setMobKillsForPlayer(killer, Werewolf.getStatisticsManager().increaseMobKills(killer.getUniqueId()));
			}
		}

		if (!(event.getEntity() instanceof Player))
		{
			return;
		}

		Player werewolf = (Player) event.getEntity();

		if (!Werewolf.getWerewolfManager().hasWerewolfSkin(werewolf.getUniqueId()))
		{
			return;
		}

		if (this.plugin.vaultEnabled && !Werewolf.getWerewolfManager().hasWerewolfSkin(killer.getUniqueId()))
		{
			Werewolf.getHuntManager().handleKill(killer.getUniqueId(), killer.getName());
		}
	}
}