package com.dogonfire.werewolf.integrations;

import org.bukkit.event.Listener;

/**
 * A listener for an optional plugin. Classes implementing this may reference that plugin's classes, so they must only
 * be constructed (by {@link com.dogonfire.werewolf.Werewolf}'s feature start, on enable and on /ww reload) after
 * checking the plugin is present.
 */
public interface IntegrationListener extends Listener
{
	/** Called when the integration stops (disable, or /ww reload), to undo anything it changed in the other plugin */
	default void shutdown()
	{
	}
}
