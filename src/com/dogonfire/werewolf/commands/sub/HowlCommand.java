package com.dogonfire.werewolf.commands.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.commands.SubCommand;
import com.dogonfire.werewolf.managers.LanguageManager.LANGUAGESTRING;

/** /howl, for werewolves in wolf form. A clan alpha's howl also makes nearby creatures nauseous */
public class HowlCommand extends SubCommand
{
	public HowlCommand(Werewolf plugin)
	{
		super(plugin, "howl", "werewolf.howl", true, null, null);
	}

	@Override
	public void execute(CommandSender sender, Player player, String[] args)
	{
		if (!Werewolf.getWerewolfManager().isWolfForm(player.getUniqueId()))
		{
			send(player, ChatColor.RED + "You are not in wolf form");
			return;
		}
		long cooldownLeft = Werewolf.getWerewolfManager().getHowlCooldownLeft(player.getUniqueId());
		if (cooldownLeft > 0)
		{
			lang().setAmount("" + cooldownLeft);
			send(player, LANGUAGESTRING.AbilityOnCooldown, ChatColor.RED);
			return;
		}
		Werewolf.getWerewolfManager().setLastHowl(player.getUniqueId());

		Werewolf.getWerewolfManager().howl(player);

		if (Werewolf.getClanManager().isAlpha(player.getUniqueId()))
		{
			for (Entity entity : player.getNearbyEntities(17.5D, 17.5D, 17.5D))
			{
				if (entity instanceof LivingEntity livingEntity)
				{
					livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 3));
				}
			}
		}
	}
}
