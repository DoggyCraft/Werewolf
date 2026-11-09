package com.dogonfire.werewolf.storage;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.werewolf.managers.ClanManager.ClanType;

/** clans.yml: one {@link ClanRecord} per clan (alpha, home, last call). Clan points are not stored. */
public final class ClanRepository extends YamlRepository
{
	private final Map<ClanType, ClanRecord> records = new EnumMap<ClanType, ClanRecord>(ClanType.class);

	public ClanRepository(File dataFolder, Logger logger, ExecutorService writer)
	{
		super(new File(dataFolder, "clans.yml"), logger, writer);
		clear();
	}

	/** Every clan has a record, possibly empty */
	public ClanRecord get(ClanType clan)
	{
		return this.records.get(clan);
	}

	@Override
	public int size()
	{
		int size = 0;
		for (ClanRecord record : this.records.values())
		{
			if (!record.isEmpty())
			{
				size++;
			}
		}
		return size;
	}

	@Override
	protected void clear()
	{
		for (ClanType clan : ClanType.values())
		{
			this.records.put(clan, new ClanRecord(clan, this));
		}
	}

	@Override
	protected void read(YamlConfiguration yaml)
	{
		for (String key : yaml.getKeys(false))
		{
			ClanType clan = null;
			try
			{
				clan = ClanType.valueOf(key);
			}
			catch (IllegalArgumentException ignored)
			{
			}

			ConfigurationSection section = yaml.getConfigurationSection(key);
			if (clan == null || section == null)
			{
				keepUnknown(key, yaml.get(key));
				continue;
			}

			ClanRecord record = this.records.get(clan);
			String alpha = section.getString("Alpha");
			if (alpha != null)
			{
				try
				{
					record.alpha = UUID.fromString(alpha);
				}
				catch (IllegalArgumentException ex)
				{
					logger().warning("clans.yml: " + key + " has an invalid Alpha '" + alpha + "', ignoring it.");
				}
			}

			ConfigurationSection home = section.getConfigurationSection("Home");
			if (home != null && home.getString("World") != null)
			{
				record.homeWorld = home.getString("World");
				record.homeX = home.getDouble("X");
				record.homeY = home.getDouble("Y");
				record.homeZ = home.getDouble("Z");
			}

			record.lastCallTime = section.getString("LastCallTime");
			record.extras = extraKeys(section, "Alpha", "Home", "LastCallTime");
		}

		logger().info("Loaded " + size() + " clans.");
	}

	@Override
	protected void write(YamlConfiguration yaml)
	{
		for (ClanRecord record : this.records.values())
		{
			if (record.isEmpty())
			{
				continue;
			}

			ConfigurationSection section = yaml.createSection(record.getClan().name());
			if (record.homeWorld != null)
			{
				section.set("Home.X", record.homeX);
				section.set("Home.Y", record.homeY);
				section.set("Home.Z", record.homeZ);
				section.set("Home.World", record.homeWorld);
			}
			section.set("Alpha", record.alpha != null ? record.alpha.toString() : null);
			section.set("LastCallTime", record.lastCallTime);
			writeExtras(section, record.extras);
		}
	}
}
