package com.dogonfire.werewolf.integrations;

import org.bukkit.event.Listener;

/**
 * A listener for an optional plugin. Classes implementing this may reference that plugin's classes, so they must only
 * be constructed (by {@link com.dogonfire.werewolf.Werewolf#onEnable()}) after checking the plugin is present.
 */
public interface IntegrationListener extends Listener
{
	/** Called from onDisable, to undo anything the integration changed in the other plugin */
	default void shutdown()
	{
	}
}
