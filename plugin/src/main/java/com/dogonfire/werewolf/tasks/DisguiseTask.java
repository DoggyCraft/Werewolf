package com.dogonfire.werewolf.tasks;

import com.dogonfire.werewolf.managers.LanguageManager;
import com.dogonfire.werewolf.managers.WerewolfScoreboardManager;
import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DisguiseTask implements Runnable
{
	private final Werewolf	plugin;
	private final WerewolfContext ctx;
	private Player		player	= null;

	public DisguiseTask(WerewolfContext ctx, Player p)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
		this.player = p;
	}

	private void dropArmor()
	{
		PlayerInventory inventory = this.player.getInventory();
		for (ItemStack stack : inventory.getArmorContents())
		{
			if (stack != null && !stack.getType().equals(Material.AIR) && stack.getAmount() != 0)
			{
				if (ctx.getWerewolfManager().hasToDropItems(this.player.getUniqueId()))
				{
					this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
				}
				else
				{
					int slot = this.player.getInventory().firstEmpty();
					if (slot > -1)
					{
						this.player.getInventory().setItem(slot, stack);
					}
					else
					{
						this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
					}
				}
			}
		}

		this.player.getInventory().setArmorContents(new ItemStack[] { new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR) });
	}

	private void dropMainHandItem()
	{
		PlayerInventory inventory = this.player.getInventory();

		ItemStack stack = inventory.getItemInMainHand();
		if ((stack == null) || (stack.getAmount() == 0) || (stack.getType().equals(Material.AIR)))
		{
			return;
		}
		if (ctx.getWerewolfManager().hasToDropItems(this.player.getUniqueId()))
		{
			this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
		}
		else
		{
			int slot = inventory.firstEmpty();
			if (slot > -1)
			{
				inventory.setItem(slot, stack);
			}
			else
			{
				this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
			}
		}
		inventory.setItemInMainHand(null);
	}

	private void dropOffHandItem()
	{
		PlayerInventory inventory = this.player.getInventory();

		ItemStack stack = inventory.getItemInOffHand();
		if ((stack == null) || (stack.getAmount() == 0) || (stack.getType().equals(Material.AIR)))
		{
			return;
		}
		if (ctx.getWerewolfManager().hasToDropItems(this.player.getUniqueId()))
		{
			this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
		}
		else
		{
			int slot = inventory.firstEmpty();
			if (slot > -1)
			{
				inventory.setItem(slot, stack);
			}
			else
			{
				this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
			}
		}
		this.player.getInventory().setItemInOffHand(null);
	}

	public void run()
	{
		if (this.player == null)
		{
			this.plugin.logDebug("DisguiseTask::Run(): Player is null!");
			return;
		}

		// Left, or was cured, in the tick since the transformation was started; or a second transformation was started
		// in the same tick
		if (!this.player.isOnline() || !ctx.getWerewolfManager().isWerewolf(this.player) || ctx.getWerewolfManager().hasWerewolfSkin(this.player.getUniqueId()))
		{
			return;
		}

		final Player nauseous = this.player;
		this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> nauseous.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 1, false, false)), 1L);

		ctx.getWolfFormManager().applyWolfForm(this.player);

		dropArmor();
		dropOffHandItem();
		dropMainHandItem();
		
		Boolean renamed = false;

		if (plugin.getSettings().names().enabled())
		{
			String werewolfName = ctx.getWerewolfManager().getWerewolfName(player.getUniqueId());

			if (werewolfName != "")
			{
				Boolean inUse = false;

				// We check if anyone else online maybe already uses this
				// randomly generated name...
				for (Player otherPlayer : plugin.getServer().getOnlinePlayers())
				{
					if (otherPlayer.getPlayerListName().contains(werewolfName))
					{
						inUse = true;
					}
				}

				// if there are noone else with the name right now, just use it
				if (inUse == false)
				{
					player.setPlayerListName(ChatColor.GOLD + werewolfName);
					renamed = true;
				}
				else
				{ // oh boy, someone already use it. Time to use integers
					int n = 1;

					while (!renamed)
					{
						renamed = true;

						// Go through all online players and check for
						// WerewolfName1, WerewolfName2 etc. until one is found
						// where the number isn't taken
						for (Player otherPlayer : plugin.getServer().getOnlinePlayers())
						{
							if (otherPlayer.getPlayerListName().contains(werewolfName + n))
							{
								n++;
							}
						}

						// Now let's try to use this number :))
						try
						{
							player.setPlayerListName(ChatColor.GOLD + werewolfName + n);
						}
						catch (Exception ex)
						{
							n++;

							renamed = false;
						}
					}
				}
			}
		}

		if (renamed == false)
		{
			int n = 1;

			while (!renamed)
			{
				renamed = true;

				// Go through all online players and check for Werewolf1,
				// Werewolf2 etc. until one is found where the number isn't
				// taken
				for (Player otherPlayer : plugin.getServer().getOnlinePlayers())
				{
					if (otherPlayer.getPlayerListName().contains("Werewolf" + n))
					{
						n++;
					}
				}

				// Now let's try to use this number :))
				try
				{
					player.setPlayerListName(ChatColor.GOLD + "Werewolf" + n);
				}
				catch (Exception ex)
				{
					n++;

					renamed = false;
				}
			}
		}

		ctx.getWerewolfManager().pushPlayerData(this.player);
		ctx.getStatisticsManager().clearStatistics(this.player.getUniqueId());

		WerewolfScoreboardManager scoreboards = ctx.getWerewolfScoreboardManager();
		if (scoreboards != null)
		{
			scoreboards.newPlayerHuntingScoreboard(this.player);
		}

		// Before trying to disguise, check if Disguises are enabled...
		if (ctx.isDisguisesEnabled())
		{
			ctx.getSkinManager().setWerewolfSkin(this.player, this.player.getPlayerListName());
		}

		ctx.getWerewolfManager().setLastTransformation(this.player.getUniqueId());

		if (this.plugin.getSettings().general().werewolfGroupEnabled())
		{
			// Still in the werewolf group (left over from a crash): keep the original group stored from before
			String werewolfGroup = this.plugin.getSettings().general().werewolfGroupName();
			String originalGroup = ctx.getPermissionsManager().getGroup(this.player);
			if (!originalGroup.isEmpty() && !originalGroup.equalsIgnoreCase(werewolfGroup))
			{
				ctx.getWerewolfManager().setOriginalPermissionGroup(this.player.getUniqueId(), originalGroup);
			}
			ctx.getPermissionsManager().addGroup(this.player, werewolfGroup);
		}

		ctx.getLanguageManager().setAmount("" + ctx.getWerewolfManager().getNumberOfTransformations(this.player.getUniqueId()));

		this.player.sendMessage(ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.Transform, ChatColor.LIGHT_PURPLE));

		if (ctx.getPermissionsManager().hasPermission(this.player, "werewolf.howl"))
		{
			ctx.getLanguageManager().setType("/howl");
			String message = ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.InfoCommandHowl, ChatColor.AQUA);
			this.player.sendMessage(message);
		}

		if (ctx.getPermissionsManager().hasPermission(this.player, "werewolf.growl"))
		{
			ctx.getLanguageManager().setType("/growl");
			String message = ctx.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.InfoCommandGrowl, ChatColor.AQUA);
			this.player.sendMessage(message);
		}

		this.plugin.log(this.player.getName() + " turned into a werewolf!");
	}
}
