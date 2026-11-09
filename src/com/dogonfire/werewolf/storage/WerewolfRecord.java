package com.dogonfire.werewolf.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.dogonfire.werewolf.managers.ClanManager.ClanType;

/**
 * One player's section in werewolves.yml (the key is their UUID). Dates are kept as the strings the file holds, so
 * they are written back in the same format. Setters mark the repository dirty.
 */
public final class WerewolfRecord
{
	private final UUID				id;
	private final YamlRepository	owner;

	String							playerName;			// PlayerName
	WolfState						wolfState;			// WolfState
	ClanType						clan;				// Clan
	String							werewolfName;		// WerewolfName
	int								transformations;	// Transformations (the werewolf's level)
	String							lastTransform;		// LastTransform, "HH:mm:ss dd-MM-yyyy"
	String							infectedDate;		// InfectedDate, "HH:mm dd-MM-yyyy"
	boolean							infectedThisNight;	// InfectedThisNight
	String							lastLevelNight;		// LastLevelNight, MoonManager.getNightKey
	boolean							intent;				// Intent, only written when true
	String							originalGroup;		// OriginalGroup, permission group before wolf form
	Map<String, Object>				extras	= new LinkedHashMap<String, Object>();

	WerewolfRecord(UUID id, YamlRepository owner)
	{
		this.id = id;
		this.owner = owner;
	}

	private void changed()
	{
		this.owner.markDirty();
	}

	public UUID getId()
	{
		return this.id;
	}

	public String getPlayerName()
	{
		return this.playerName;
	}

	public void setPlayerName(String playerName)
	{
		this.playerName = playerName;
		changed();
	}

	public WolfState getWolfState()
	{
		return this.wolfState;
	}

	public void setWolfState(WolfState wolfState)
	{
		this.wolfState = wolfState;
		changed();
	}

	public ClanType getClan()
	{
		return this.clan;
	}

	public void setClan(ClanType clan)
	{
		this.clan = clan;
		changed();
	}

	public String getWerewolfName()
	{
		return this.werewolfName;
	}

	public void setWerewolfName(String werewolfName)
	{
		this.werewolfName = werewolfName;
		changed();
	}

	public int getTransformations()
	{
		return this.transformations;
	}

	public void setTransformations(int transformations)
	{
		this.transformations = transformations;
		changed();
	}

	public String getLastTransform()
	{
		return this.lastTransform;
	}

	public void setLastTransform(String lastTransform)
	{
		this.lastTransform = lastTransform;
		changed();
	}

	public String getInfectedDate()
	{
		return this.infectedDate;
	}

	public void setInfectedDate(String infectedDate)
	{
		this.infectedDate = infectedDate;
		changed();
	}

	public boolean isInfectedThisNight()
	{
		return this.infectedThisNight;
	}

	public void setInfectedThisNight(boolean infectedThisNight)
	{
		this.infectedThisNight = infectedThisNight;
		changed();
	}

	public String getLastLevelNight()
	{
		return this.lastLevelNight;
	}

	public void setLastLevelNight(String lastLevelNight)
	{
		this.lastLevelNight = lastLevelNight;
		changed();
	}

	public boolean hasIntent()
	{
		return this.intent;
	}

	public void setIntent(boolean intent)
	{
		this.intent = intent;
		changed();
	}

	public String getOriginalGroup()
	{
		return this.originalGroup;
	}

	public void setOriginalGroup(String originalGroup)
	{
		this.originalGroup = originalGroup;
		changed();
	}
}
