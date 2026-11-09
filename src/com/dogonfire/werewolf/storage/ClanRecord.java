package com.dogonfire.werewolf.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.dogonfire.werewolf.managers.ClanManager.ClanType;

/** One clan's section in clans.yml (the key is the ClanType name). Setters mark the repository dirty. */
public final class ClanRecord
{
	private final ClanType			clan;
	private final YamlRepository	owner;

	UUID							alpha;			// Alpha
	String							homeWorld;		// Home.World; null = no home
	double							homeX;			// Home.X
	double							homeY;			// Home.Y
	double							homeZ;			// Home.Z
	String							lastCallTime;	// LastCallTime, "HH:mm:ss dd-MM-yyyy"
	Map<String, Object>				extras	= new LinkedHashMap<String, Object>();

	ClanRecord(ClanType clan, YamlRepository owner)
	{
		this.clan = clan;
		this.owner = owner;
	}

	public ClanType getClan()
	{
		return this.clan;
	}

	public UUID getAlpha()
	{
		return this.alpha;
	}

	public void setAlpha(UUID alpha)
	{
		this.alpha = alpha;
		this.owner.markDirty();
	}

	public String getHomeWorld()
	{
		return this.homeWorld;
	}

	public double getHomeX()
	{
		return this.homeX;
	}

	public double getHomeY()
	{
		return this.homeY;
	}

	public double getHomeZ()
	{
		return this.homeZ;
	}

	public void setHome(String world, double x, double y, double z)
	{
		this.homeWorld = world;
		this.homeX = x;
		this.homeY = y;
		this.homeZ = z;
		this.owner.markDirty();
	}

	public String getLastCallTime()
	{
		return this.lastCallTime;
	}

	public void setLastCallTime(String lastCallTime)
	{
		this.lastCallTime = lastCallTime;
		this.owner.markDirty();
	}

	boolean isEmpty()
	{
		return this.alpha == null && this.homeWorld == null && this.lastCallTime == null && this.extras.isEmpty();
	}
}
