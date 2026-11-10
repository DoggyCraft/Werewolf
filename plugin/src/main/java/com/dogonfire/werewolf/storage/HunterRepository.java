package com.dogonfire.werewolf.storage;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** hunters.yml: werewolf kills per hunter, keyed by player name, and the bounty pool under {@value #BOUNTY_KEY} */
public final class HunterRepository extends YamlRepository
{
	// Can't be a player name (no '-' in those)
	private static final String				BOUNTY_KEY	= "total-bounty";

	private final Map<String, HunterRecord>	records		= new LinkedHashMap<String, HunterRecord>();
	private int								totalBounty;

	public HunterRepository(File dataFolder, Logger logger, ExecutorService writer)
	{
		super(new File(dataFolder, "hunters.yml"), logger, writer);
	}

	/** The hunter's record, or null */
	public HunterRecord get(String name)
	{
		return this.records.get(name);
	}

	public HunterRecord getOrCreate(String name)
	{
		HunterRecord record = this.records.get(name);
		if (record == null)
		{
			record = new HunterRecord(name, this);
			this.records.put(name, record);
			markDirty();
		}
		return record;
	}

	public Set<String> names()
	{
		return Collections.unmodifiableSet(this.records.keySet());
	}

	/** Money in the werewolf bounty, paid to the next hunter who kills a werewolf */
	public int getTotalBounty()
	{
		return this.totalBounty;
	}

	public void setTotalBounty(int totalBounty)
	{
		if (this.totalBounty != totalBounty)
		{
			this.totalBounty = totalBounty;
			markDirty();
		}
	}

	@Override
	public int size()
	{
		return this.records.size();
	}

	@Override
	protected void clear()
	{
		this.records.clear();
		this.totalBounty = 0;
	}

	@Override
	protected void read(YamlConfiguration yaml)
	{
		for (String key : yaml.getKeys(false))
		{
			if (key.equals(BOUNTY_KEY))
			{
				this.totalBounty = Math.max(0, yaml.getInt(key));
				continue;
			}

			ConfigurationSection section = yaml.getConfigurationSection(key);
			if (section == null)
			{
				keepUnknown(key, yaml.get(key));
				continue;
			}

			HunterRecord record = new HunterRecord(key, this);
			record.kills = section.getInt("Kills");
			record.lastKill = section.getString("LastKill");
			record.extras = extraKeys(section, "Kills", "LastKill");
			this.records.put(key, record);
		}

		logger().info("Loaded " + this.records.size() + " werewolf hunters.");
	}

	@Override
	protected void write(YamlConfiguration yaml)
	{
		if (this.totalBounty > 0)
		{
			yaml.set(BOUNTY_KEY, this.totalBounty);
		}
		for (HunterRecord record : this.records.values())
		{
			ConfigurationSection section = yaml.createSection(record.getName());
			section.set("Kills", record.kills);
			section.set("LastKill", record.lastKill);
			writeExtras(section, record.extras);
		}
	}
}
