package com.dogonfire.werewolf.storage;

import java.util.LinkedHashMap;
import java.util.Map;

/** One hunter's section in hunters.yml (the key is the player name). Setters mark the repository dirty. */
public final class HunterRecord
{
	private final String			name;
	private final YamlRepository	owner;

	int								kills;		// Kills
	String							lastKill;	// LastKill, "HH:mm dd-MM-yyyy"
	Map<String, Object>				extras	= new LinkedHashMap<String, Object>();

	HunterRecord(String name, YamlRepository owner)
	{
		this.name = name;
		this.owner = owner;
	}

	public String getName()
	{
		return this.name;
	}

	public int getKills()
	{
		return this.kills;
	}

	public void setKills(int kills)
	{
		this.kills = kills;
		this.owner.markDirty();
	}

	public String getLastKill()
	{
		return this.lastKill;
	}

	public void setLastKill(String lastKill)
	{
		this.lastKill = lastKill;
		this.owner.markDirty();
	}
}
