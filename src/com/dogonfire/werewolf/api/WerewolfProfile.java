package com.dogonfire.werewolf.api;

import java.util.UUID;

/**
 * An immutable snapshot of one werewolf, as returned by {@link WerewolfService#getWerewolf(UUID)}. It does not change
 * when the werewolf does; ask the service again for current values.
 *
 * @param uuid the player's UUID
 * @param clan the werewolf's clan
 * @param form the form the werewolf is in right now ({@link Form#HUMAN} while offline)
 * @param infected true for an infected werewolf that hasn't transformed yet, false for a full werewolf
 * @param level the werewolf's level: the number of transformation nights it has transformed under
 * @param werewolfName the name shown for the werewolf in wolf form (may be empty until it first transforms)
 * @param intent whether the werewolf has switched on /ww intent (a higher chance that its bites infect)
 * @param alpha whether the werewolf is its clan's alpha
 */
public record WerewolfProfile(UUID uuid, Clan clan, Form form, boolean infected, int level, String werewolfName, boolean intent, boolean alpha)
{
	/** True for a full werewolf, false for an infected one that hasn't transformed yet */
	public boolean isFullWerewolf()
	{
		return !infected;
	}

	/** True when the werewolf is in wolf form right now */
	public boolean isInWolfForm()
	{
		return form == Form.WOLF;
	}
}
