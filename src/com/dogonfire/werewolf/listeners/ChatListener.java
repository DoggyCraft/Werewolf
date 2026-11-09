package com.dogonfire.werewolf.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;

/**
 * Werewolves in wolf form can't talk: humans see wolf noises, werewolves a garbled message, listeners the real one.
 * Only registered while Chat.WolfChat is on.
 * <p>
 * The chat event usually runs on an async thread, so it only reads thread-safe state (the wolf-form map, which is a
 * concurrent map, and the immutable settings) to decide whether to cancel; the per-recipient messages, which need the
 * werewolf data and permission checks, are sent from the main thread.
 */
public class ChatListener implements Listener
{
	private static final List<String>	WOLF_MESSAGES	= List.of("*Grunt*", "*Grunt* *Grrrr*", "*Grunt* *Grunt*", "*Growl*", "*Grrroowl Grunt*", "*Grrrrr*", "*Rrrrrr*", "*Groooowl*",
			"*Grrrrr* *Grrr*", "*Hoooowl*", "*Rrraagh*", "*Grrawl*", "*Grrrrawl* *Growls*", "*HOOOOOWLLLL!*", "*Wimper*", "*Awooooo*");

	private final Werewolf				plugin;
	private final WerewolfContext		ctx;

	public ChatListener(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	private static String getWerewolfLanguage(String message)
	{
		ThreadLocalRandom random = ThreadLocalRandom.current();
		message = message.toLowerCase().replace("a", "arr");
		if (random.nextInt(2) == 0)
		{
			message = message.replace("r", "rr");
		}
		if (random.nextInt(2) == 0)
		{
			message = message.replace("f", "woof");
		}
		else
		{
			message = message.replace("o", "awoo");
		}
		return message;
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e)
	{
		if (!this.ctx.isWerewolvesEnabled())
		{
			return;
		}

		Player player = e.getPlayer();
		if (!this.ctx.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			return;
		}

		e.setCancelled(true);

		// Everything the main thread needs, copied from the event now
		List<UUID> recipients = new ArrayList<UUID>();
		for (Player receiver : e.getRecipients())
		{
			recipients.add(receiver.getUniqueId());
		}
		String format = e.getFormat();
		String original = e.getMessage();
		String garbled = getWerewolfLanguage(original);
		String noise = WOLF_MESSAGES.get(ThreadLocalRandom.current().nextInt(WOLF_MESSAGES.size()));
		String prefix = this.plugin.getSettings().general().chatPrefix();

		Runnable send = () -> sendWolfChat(player, recipients, format, original, garbled, noise, prefix);
		if (!e.isAsynchronous())
		{
			send.run();
		}
		else if (this.plugin.isEnabled())
		{
			this.plugin.getServer().getScheduler().runTask(this.plugin, send);
		}
	}

	private void sendWolfChat(Player player, List<UUID> recipients, String format, String original, String garbled, String noise, String prefix)
	{
		if (!player.isOnline())
		{
			return;
		}

		String listName = this.ctx.getWerewolfManager().getPlayerListName(player);
		if (listName == null)
		{
			listName = player.getName();
		}
		for (UUID receiverId : recipients)
		{
			Player receiver = this.plugin.getServer().getPlayer(receiverId);
			if (receiver == null)
			{
				continue;
			}

			String messageToSend;
			if (this.ctx.getPermissionsManager().hasPermission(receiver, "werewolf.superlistener"))
			{
				messageToSend = String.format(format, "Werewolf " + player.getName(), original);
				this.plugin.logDebug("Recipent for message has superlistener perm: " + receiver.getName() + " - Message: " + messageToSend);
			}
			else if (receiver.isOp() || this.ctx.getPermissionsManager().hasPermission(receiver, "werewolf.listener"))
			{
				messageToSend = String.format(format, listName, original);
				this.plugin.logDebug("Recipent for message is OP or has perm: " + receiver.getName() + " - Message: " + messageToSend);
			}
			else if (this.ctx.getWerewolfManager().isWerewolf(receiver.getUniqueId()))
			{
				messageToSend = String.format(format, listName, garbled);
				this.plugin.logDebug("Recipent for message is Werewolf: " + receiver.getName() + " - Message: " + messageToSend);
			}
			else
			{
				messageToSend = String.format(format, prefix, noise);
				this.plugin.logDebug("Recipent for message is Human (non-werewolf): " + receiver.getName() + " - Message: " + messageToSend);
			}
			receiver.sendMessage(messageToSend);
		}
		this.plugin.log(String.format(format, "Werewolf " + player.getName(), original));

		this.ctx.getWerewolfManager().growl(player);
	}
}
