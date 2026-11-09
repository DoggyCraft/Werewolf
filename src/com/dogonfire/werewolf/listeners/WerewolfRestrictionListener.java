package com.dogonfire.werewolf.listeners;

import java.util.List;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.events.WerewolfTransformEvent;
import com.dogonfire.werewolf.managers.LanguageManager;

/**
 * Wolf form restrictions: blocked commands, blocked potion effects, the meat-only diet and no transforming in
 * creative/spectator.
 */
public class WerewolfRestrictionListener implements Listener
{
	public static final String	BYPASS_BLOCKED_COMMANDS	= "werewolf.bypass.blockedcommands";

	private final Werewolf		plugin;

	public WerewolfRestrictionListener(Werewolf plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onCommand(PlayerCommandPreprocessEvent event)
	{
		if (!Werewolf.pluginEnabled)
		{
			return;
		}

		Player player = event.getPlayer();
		if (!Werewolf.getWerewolfManager().isWerewolf(player) || player.hasPermission(BYPASS_BLOCKED_COMMANDS))
		{
			return;
		}

		String command = event.getMessage();
		LanguageManager.LANGUAGESTRING message = null;

		if (isBlocked(command, this.plugin.blockedCommandsAll))
		{
			message = LanguageManager.LANGUAGESTRING.BlockedCommand;
		}
		else if (Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()) && isBlocked(command, this.plugin.blockedCommandsWolfForm))
		{
			message = LanguageManager.LANGUAGESTRING.BlockedCommandWolfForm;
		}
		else if (Werewolf.getClanManager().isAlpha(player.getUniqueId()) && isBlocked(command, this.plugin.blockedCommandsAlpha))
		{
			message = LanguageManager.LANGUAGESTRING.BlockedCommandAlpha;
		}

		if (message != null)
		{
			event.setCancelled(true);
			player.sendMessage(Werewolf.getLanguageManager().getLanguageString(message, ChatColor.RED));
		}
	}

	/**
	 * Matches whole words, so "/v o" blocks "/v o Steve" but not "/v open", and "/skin" doesn't block "/skins".
	 * "/plugin:command" is matched as "/command" too.
	 */
	private static boolean isBlocked(String message, List<String> blockedCommands)
	{
		if (blockedCommands.isEmpty())
		{
			return false;
		}

		String command = normalize(message);
		String unprefixed = command;
		int space = command.indexOf(' ');
		int colon = command.indexOf(':');
		if (colon > 0 && (space < 0 || colon < space))
		{
			unprefixed = command.substring(colon + 1);
		}

		for (String blocked : blockedCommands)
		{
			String entry = normalize(blocked);
			if (entry.isEmpty())
			{
				continue;
			}

			if (matches(command, entry) || matches(unprefixed, entry))
			{
				return true;
			}
		}

		return false;
	}

	private static String normalize(String command)
	{
		String result = command.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
		while (result.startsWith("/"))
		{
			result = result.substring(1);
		}
		return result;
	}

	private static boolean matches(String command, String entry)
	{
		return command.equals(entry) || command.startsWith(entry + " ");
	}

	@EventHandler(ignoreCancelled = true)
	public void onPotionEffect(EntityPotionEffectEvent event)
	{
		if (!Werewolf.pluginEnabled || !(event.getEntity() instanceof Player) || event.getNewEffect() == null)
		{
			return;
		}

		if (event.getAction() != EntityPotionEffectEvent.Action.ADDED && event.getAction() != EntityPotionEffectEvent.Action.CHANGED)
		{
			return;
		}

		Player player = (Player) event.getEntity();
		if (Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()) && Werewolf.getWolfFormManager().isBlockedPotion(player, event.getNewEffect().getType()))
		{
			event.setCancelled(true);
		}
	}

	private boolean isOnDiet(Player player)
	{
		if (!this.plugin.dietEnabled || !Werewolf.getWerewolfManager().isWerewolf(player))
		{
			return false;
		}

		return this.plugin.dietHumanForm || Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId());
	}

	@EventHandler(ignoreCancelled = true)
	public void onConsume(PlayerItemConsumeEvent event)
	{
		if (!Werewolf.pluginEnabled)
		{
			return;
		}

		Player player = event.getPlayer();
		if (!Werewolf.getWolfFormManager().isBlockedFood(event.getItem().getType()) || !isOnDiet(player))
		{
			return;
		}

		player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfTryEat, ChatColor.RED));

		if (this.plugin.dietPreventConsumption)
		{
			event.setCancelled(true);
		}
		else
		{
			restoreHungerNextTick(player);
		}
	}

	// Cake is eaten by right-clicking the placed block, which doesn't fire PlayerItemConsumeEvent
	@EventHandler(ignoreCancelled = true)
	public void onEatCake(PlayerInteractEvent event)
	{
		if (!Werewolf.pluginEnabled || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null)
		{
			return;
		}

		Block block = event.getClickedBlock();
		if (block.getType() != Material.CAKE && !Tag.CANDLE_CAKES.isTagged(block.getType()))
		{
			return;
		}

		Player player = event.getPlayer();
		if (!Werewolf.getWolfFormManager().isBlockedFood(Material.CAKE) || !isOnDiet(player))
		{
			return;
		}

		if (this.plugin.dietPreventConsumption)
		{
			if (player.getFoodLevel() < 20 || player.getGameMode() == GameMode.CREATIVE)
			{
				player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WerewolfTryEat, ChatColor.RED));
			}
			event.setCancelled(true);
		}
		else
		{
			restoreHungerNextTick(player);
		}
	}

	private void restoreHungerNextTick(final Player player)
	{
		final int foodLevel = player.getFoodLevel();
		final float saturation = player.getSaturation();

		this.plugin.getServer().getScheduler().runTaskLater(this.plugin, new Runnable()
		{
			public void run()
			{
				if (player.isOnline())
				{
					player.setFoodLevel(Math.min(player.getFoodLevel(), foodLevel));
					player.setSaturation(Math.min(player.getSaturation(), saturation));
				}
			}
		}, 1L);
	}

	@EventHandler(ignoreCancelled = true)
	public void onTransform(WerewolfTransformEvent event)
	{
		if (!event.isToWolfForm() || event.getReason() == WerewolfTransformEvent.Reason.FORCED || event.getReason() == WerewolfTransformEvent.Reason.REFRESH)
		{
			return;
		}

		GameMode gameMode = event.getPlayer().getGameMode();
		if (gameMode != GameMode.CREATIVE && gameMode != GameMode.SPECTATOR)
		{
			return;
		}

		event.setCancelled(true);

		if (event.getReason() == WerewolfTransformEvent.Reason.CONTROLLED || event.getReason() == WerewolfTransformEvent.Reason.COMMAND)
		{
			event.getPlayer().sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CannotTransformInGameMode, ChatColor.RED));
		}
	}
}
