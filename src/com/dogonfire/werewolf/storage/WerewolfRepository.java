package com.dogonfire.werewolf.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.werewolf.managers.ClanManager.ClanType;

/** werewolves.yml: one {@link WerewolfRecord} per player UUID */
public final class WerewolfRepository extends YamlRepository
{
	private static final String[]			KNOWN_KEYS	= { "PlayerName", "WolfState", "Clan", "WerewolfName", "Transformations", "LastTransform", "InfectedDate", "InfectedThisNight", "LastLevelNight", "Intent", "OriginalGroup" };

	private final Map<UUID, WerewolfRecord>	records		= new HashMap<UUID, WerewolfRecord>();
	private final Random					random		= new Random();

	public WerewolfRepository(File dataFolder, Logger logger, ExecutorService writer)
	{
		super(new File(dataFolder, "werewolves.yml"), logger, writer);
	}

	/** The player's record, or null if they are not in werewolves.yml */
	public WerewolfRecord get(UUID playerId)
	{
		return playerId == null ? null : this.records.get(playerId);
	}

	public boolean contains(UUID playerId)
	{
		return playerId != null && this.records.containsKey(playerId);
	}

	/** The player's record, created empty if they have none */
	public WerewolfRecord getOrCreate(UUID playerId)
	{
		WerewolfRecord record = this.records.get(playerId);
		if (record == null)
		{
			record = new WerewolfRecord(playerId, this);
			this.records.put(playerId, record);
			markDirty();
		}
		return record;
	}

	public void remove(UUID playerId)
	{
		if (this.records.remove(playerId) != null)
		{
			markDirty();
		}
	}

	public Collection<WerewolfRecord> all()
	{
		return Collections.unmodifiableCollection(this.records.values());
	}

	public Collection<UUID> ids()
	{
		return Collections.unmodifiableSet(this.records.keySet());
	}

	public List<WerewolfRecord> inClan(ClanType clan)
	{
		List<WerewolfRecord> members = new ArrayList<WerewolfRecord>();
		for (WerewolfRecord record : this.records.values())
		{
			if (record.clan == clan)
			{
				members.add(record);
			}
		}
		return members;
	}

	/** The record whose PlayerName matches, ignoring case, or null */
	public WerewolfRecord findByName(String name)
	{
		for (WerewolfRecord record : this.records.values())
		{
			if (name.equalsIgnoreCase(record.playerName))
			{
				return record;
			}
		}
		return null;
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
	}

	@Override
	protected void read(YamlConfiguration yaml)
	{
		boolean assignedClans = false;

		for (String key : yaml.getKeys(false))
		{
			UUID playerId;
			try
			{
				playerId = UUID.fromString(key);
			}
			catch (IllegalArgumentException ex)
			{
				keepUnknown(key, yaml.get(key));
				continue;
			}

			ConfigurationSection section = yaml.getConfigurationSection(key);
			if (section == null)
			{
				keepUnknown(key, yaml.get(key));
				continue;
			}

			WerewolfRecord record = new WerewolfRecord(playerId, this);
			record.playerName = section.getString("PlayerName");
			record.wolfState = parseEnum(WolfState.class, section.getString("WolfState"), key);
			record.clan = parseEnum(ClanType.class, section.getString("Clan"), key);
			record.werewolfName = section.getString("WerewolfName");
			record.transformations = section.getInt("Transformations");
			record.lastTransform = section.getString("LastTransform");
			record.infectedDate = section.getString("InfectedDate");
			record.infectedThisNight = section.getBoolean("InfectedThisNight");
			record.lastLevelNight = section.getString("LastLevelNight");
			record.intent = section.getBoolean("Intent");
			record.originalGroup = section.getString("OriginalGroup");
			record.extras = extraKeys(section, KNOWN_KEYS);

			// Werewolves from old versions may have no clan: they get a random one, once
			if (record.wolfState != null && record.clan == null)
			{
				record.clan = ClanType.values()[this.random.nextInt(ClanType.values().length)];
				assignedClans = true;
			}

			this.records.put(playerId, record);
		}

		logger().info("Loaded " + this.records.size() + " werewolves.");

		if (assignedClans)
		{
			markDirty();
		}
	}

	private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String key)
	{
		if (value == null)
		{
			return null;
		}
		try
		{
			return Enum.valueOf(type, value);
		}
		catch (IllegalArgumentException ex)
		{
			logger().warning("werewolves.yml: " + key + " has an invalid " + type.getSimpleName() + " '" + value + "', ignoring it.");
			return null;
		}
	}

	@Override
	protected void write(YamlConfiguration yaml)
	{
		for (WerewolfRecord record : this.records.values())
		{
			ConfigurationSection section = yaml.createSection(record.getId().toString());
			section.set("PlayerName", record.playerName);
			section.set("WolfState", record.wolfState != null ? record.wolfState.name() : null);
			section.set("Clan", record.clan != null ? record.clan.name() : null);
			section.set("WerewolfName", record.werewolfName);
			section.set("Transformations", record.transformations);
			section.set("LastTransform", record.lastTransform);
			section.set("InfectedDate", record.infectedDate);
			section.set("InfectedThisNight", record.infectedThisNight);
			section.set("LastLevelNight", record.lastLevelNight);
			section.set("Intent", record.intent ? Boolean.TRUE : null);
			section.set("OriginalGroup", record.originalGroup);
			writeExtras(section, record.extras);
		}
	}
}
