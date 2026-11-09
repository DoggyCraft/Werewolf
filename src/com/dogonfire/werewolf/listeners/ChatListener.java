package com.dogonfire.werewolf.listeners;

import java.util.List;
import java.util.Random;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.dogonfire.werewolf.Werewolf;

public class ChatListener implements Listener
{
	private static final List<String>	WOLF_MESSAGES	= List.of("*Grunt*", "*Grunt* *Grrrr*", "*Grunt* *Grunt*", "*Growl*", "*Grrroowl Grunt*", "*Grrrrr*", "*Rrrrrr*", "*Groooowl*",
			"*Grrrrr* *Grrr*", "*Hoooowl*", "*Rrraagh*", "*Grrawl*", "*Grrrrawl* *Growls*", "*HOOOOOWLLLL!*", "*Wimper*", "*Awooooo*");

	private Werewolf	plugin	= null;
	private Random		random	= new Random();

	public ChatListener(Werewolf plugin)
	{
		this.plugin = plugin;
	}

	private String getWerewolfLanguage(String message)
	{
		message = message.toLowerCase().replace("a", "arr");
		if (this.random.nextInt(2) == 0)
		{
			message = message.replace("r", "rr");
		}
		if (this.random.nextInt(2) == 0)
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
		if (!Werewolf.pluginEnabled || !this.plugin.getSettings().general().wolfChat())
		{
			return;
		}
		
		Player player = e.getPlayer();
		if (Werewolf.getWerewolfManager().hasWerewolfSkin(player.getUniqueId()))
		{
			e.setCancelled(true);

			Random r = new Random();

			String alternativeMessage = WOLF_MESSAGES.get(r.nextInt(WOLF_MESSAGES.size()));

			String message = getWerewolfLanguage(e.getMessage());
			
			String messageToSend = "";
			for (Player receiver : e.getRecipients())
			{
				if (Werewolf.getPermissionsManager().hasPermission(receiver, "werewolf.superlistener"))
				{
					messageToSend = String.format(e.getFormat(), "Werewolf " + player.getName(), e.getMessage());
					this.plugin.logDebug("Recipent for message has superlistener perm: " + receiver.getName() + " - Message: " + messageToSend);
				}
				else if ((receiver.isOp()) || (Werewolf.getPermissionsManager().hasPermission(receiver, "werewolf.listener")))
				{
					messageToSend = String.format(e.getFormat(), Werewolf.getWerewolfManager().getPlayerListName(player), e.getMessage());
					this.plugin.logDebug("Recipent for message is OP or has perm: " + receiver.getName() + " - Message: " + messageToSend);
				}
				else if (Werewolf.getWerewolfManager().isWerewolf(receiver.getUniqueId()))
				{
					messageToSend = String.format(e.getFormat(), Werewolf.getWerewolfManager().getPlayerListName(player), message);
					this.plugin.logDebug("Recipent for message is Werewolf: " + receiver.getName() + " - Message: " + messageToSend);
				}
				else
				{
					messageToSend = String.format(e.getFormat(), plugin.getSettings().general().chatPrefix(), alternativeMessage);
					this.plugin.logDebug("Recipent for message is Human (non-werewolf): " + receiver.getName() + " - Message: " + messageToSend);
				}
				receiver.sendMessage(messageToSend);
			}
			this.plugin.log(String.format(e.getFormat(), "Werewolf " + player.getName(), e.getMessage()));

			Werewolf.getWerewolfManager().growl(player);
		}
	}
}