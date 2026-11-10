package com.dogonfire.werewolf.api;

/** The outcome of a {@link WerewolfService} action */
public enum ActionResult
{
	/** The change was made (for transformations: started; the skin changes a tick later) */
	SUCCESS,
	/** A listener cancelled the event the action fires, so nothing changed */
	CANCELLED,
	/** The player is not a werewolf */
	NOT_WEREWOLF,
	/** The player is already a werewolf */
	ALREADY_WEREWOLF,
	/** The werewolf is already in the requested form */
	ALREADY_IN_FORM,
	/** The werewolf belongs to a different clan */
	WRONG_CLAN,
	/** The player has to be online for this action */
	PLAYER_OFFLINE,
	/**
	 * Not possible right now: the world is not in {@code AllowedWorlds}, or the werewolf was infected this night (it
	 * can't transform before the next transformation night)
	 */
	NOT_ALLOWED,
	/** Werewolves are switched off with /ww off */
	DISABLED;

	/** True for {@link #SUCCESS} */
	public boolean isSuccess()
	{
		return this == SUCCESS;
	}
}
