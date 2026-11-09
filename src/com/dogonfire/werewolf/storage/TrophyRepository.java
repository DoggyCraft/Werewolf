package com.dogonfire.werewolf.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** trophies.yml: the lore of placed trophy heads, keyed by a hash of the block position, under {@code Description} */
public final class TrophyRepository extends YamlRepository
{
	private final Map<String, List<String>>			descriptions	= new LinkedHashMap<String, List<String>>();
	private final Map<String, Map<String, Object>>	extras			= new HashMap<String, Map<String, Object>>();

	public TrophyRepository(File dataFolder, Logger logger, ExecutorService writer)
	{
		super(new File(dataFolder, "trophies.yml"), logger, writer);
	}

	/** The lore of the trophy placed at this position, or null */
	public List<String> getDescription(String key)
	{
		return this.descriptions.get(key);
	}

	public void put(String key, List<String> description)
	{
		this.descriptions.put(key, new ArrayList<String>(description));
		markDirty();
	}

	public void remove(String key)
	{
		if (this.descriptions.remove(key) != null | this.extras.remove(key) != null)
		{
			markDirty();
		}
	}

	@Override
	public int size()
	{
		return this.descriptions.size();
	}

	@Override
	protected void clear()
	{
		this.descriptions.clear();
		this.extras.clear();
	}

	@Override
	protected void read(YamlConfiguration yaml)
	{
		for (String key : yaml.getKeys(false))
		{
			ConfigurationSection section = yaml.getConfigurationSection(key);
			if (section == null)
			{
				keepUnknown(key, yaml.get(key));
				continue;
			}

			// Written as a list of lore lines; accept a single string too
			List<String> description = section.isList("Description") ? section.getStringList("Description") : null;
			if (description == null && section.getString("Description") != null)
			{
				description = new ArrayList<String>(List.of(section.getString("Description")));
			}
			if (description != null)
			{
				this.descriptions.put(key, description);
			}

			Map<String, Object> other = extraKeys(section, "Description");
			if (!other.isEmpty())
			{
				this.extras.put(key, other);
			}
		}

		logger().info("Loaded " + this.descriptions.size() + " Werewolf trophies.");
	}

	@Override
	protected void write(YamlConfiguration yaml)
	{
		for (Map.Entry<String, List<String>> entry : this.descriptions.entrySet())
		{
			yaml.createSection(entry.getKey()).set("Description", entry.getValue());
		}
		for (Map.Entry<String, Map<String, Object>> entry : this.extras.entrySet())
		{
			ConfigurationSection section = yaml.getConfigurationSection(entry.getKey());
			writeExtras(section != null ? section : yaml.createSection(entry.getKey()), entry.getValue());
		}
	}
}
