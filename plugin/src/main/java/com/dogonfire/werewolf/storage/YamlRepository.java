package com.dogonfire.werewolf.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * One YAML data file, held in memory. Subclasses parse the file into typed records ({@link #read}) and write them back
 * ({@link #write}) with the same keys. Changes only mark the repository dirty; {@link #flush()} (main thread) turns the
 * records into YAML text and hands it to the shared writer thread, which writes a temp file and moves it over the old
 * one. A file that fails to parse is copied to {@code <name>.broken-<timestamp>} and never written over.
 */
public abstract class YamlRepository
{
	private final File				file;
	private final Logger			logger;
	private final ExecutorService	writer;
	private volatile boolean		dirty;
	private boolean					readOnly;
	// Top-level keys the subclass did not understand, written back unchanged
	private Map<String, Object>		unknownKeys	= new LinkedHashMap<String, Object>();
	// Last modification time of the file as we read or wrote it; anything else means it was edited by hand
	private volatile FileTime		knownModified;

	protected YamlRepository(File file, Logger logger, ExecutorService writer)
	{
		this.file = file;
		this.logger = logger;
		this.writer = writer;
	}

	/** Parses the file into records. Unknown top-level keys should be passed to {@link #keepUnknown}. */
	protected abstract void read(YamlConfiguration yaml);

	/** Writes every record into an empty YamlConfiguration, with the same keys {@link #read} understands. */
	protected abstract void write(YamlConfiguration yaml);

	protected abstract void clear();

	public abstract int size();

	public File getFile()
	{
		return this.file;
	}

	public boolean isReadOnly()
	{
		return this.readOnly;
	}

	/** Called by records and subclasses whenever data changes */
	public void markDirty()
	{
		this.dirty = true;
	}

	protected Logger logger()
	{
		return this.logger;
	}

	protected void keepUnknown(String key, Object value)
	{
		this.unknownKeys.put(key, toPlain(value));
	}

	/** Startup: reads the file into memory. A missing file is an empty repository. */
	public void load()
	{
		load(true);
	}

	/**
	 * Parses the file first and only replaces the data in memory if that worked. At startup (or while the file is still
	 * unreadable from startup) a broken file leaves the repository empty and read-only, so it is never written over.
	 * On a reload, a broken file keeps the data in memory and the repository writable, and the file is written again
	 * from memory on the next save (a copy of the broken file is kept). Returns whether the file was read.
	 */
	private boolean load(boolean startup)
	{
		YamlConfiguration yaml = null;
		FileTime modified = null;

		if (this.file.exists())
		{
			yaml = new YamlConfiguration();
			try
			{
				modified = Files.getLastModifiedTime(this.file.toPath());
				yaml.load(this.file);
			}
			catch (IOException | InvalidConfigurationException ex)
			{
				String copy = keepBrokenCopy();
				this.logger.log(Level.SEVERE, "Could not read " + this.file.getName() + ": " + ex.getMessage());

				if (startup || this.readOnly)
				{
					clear();
					this.unknownKeys = new LinkedHashMap<String, Object>();
					this.dirty = false;
					this.readOnly = true;
					this.knownModified = modified;
					this.logger.severe(this.file.getName() + " will NOT be saved until it is fixed and the plugin is reloaded or restarted, so nothing in it is overwritten."
							+ (copy != null ? " A copy was kept as " + copy + "." : ""));
				}
				else
				{
					// Not read again until it changes; the data in memory is saved over it
					this.knownModified = modified;
					this.dirty = true;
					this.logger.severe("Kept the " + size() + " entries already loaded; " + this.file.getName() + " will be saved from them, undoing the edit."
							+ (copy != null ? " The edited file was kept as " + copy + "." : ""));
				}
				return false;
			}
		}

		clear();
		this.unknownKeys = new LinkedHashMap<String, Object>();
		this.dirty = false;
		this.readOnly = false;
		this.knownModified = modified;

		if (yaml != null)
		{
			read(yaml);
		}
		return true;
	}

	private String keepBrokenCopy()
	{
		String name = this.file.getName() + ".broken-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		try
		{
			Files.copy(this.file.toPath(), this.file.toPath().resolveSibling(name), StandardCopyOption.REPLACE_EXISTING);
			return name;
		}
		catch (IOException ex)
		{
			this.logger.warning("Could not copy " + this.file.getName() + " to " + name + ": " + ex.getMessage());
			return null;
		}
	}

	/**
	 * Re-reads the file if it was changed outside the plugin since it was last read or written (an admin edited it), or
	 * if it could not be read before. Otherwise the data in memory is kept. Call after pending writes have finished.
	 */
	public void reloadIfChanged()
	{
		FileTime current = null;
		try
		{
			current = this.file.exists() ? Files.getLastModifiedTime(this.file.toPath()) : null;
		}
		catch (IOException ignored)
		{
		}

		if (!this.readOnly && (current == null || current.equals(this.knownModified)))
		{
			return;
		}

		if (this.dirty && !this.readOnly)
		{
			this.logger.warning(this.file.getName() + " was changed on disk; reading it again (changes not yet saved are discarded if it can be read).");
		}
		if (load(false))
		{
			this.logger.info("Reloaded " + size() + " entries from " + this.file.getName() + ".");
		}
	}

	/** Main thread: if anything changed, snapshots the data to YAML text and queues the write. */
	public void flush()
	{
		if (!this.dirty || this.readOnly)
		{
			return;
		}

		YamlConfiguration yaml = new YamlConfiguration();
		write(yaml);
		for (Map.Entry<String, Object> entry : this.unknownKeys.entrySet())
		{
			if (!yaml.contains(entry.getKey()))
			{
				yaml.set(entry.getKey(), entry.getValue());
			}
		}
		final String text = yaml.saveToString();
		this.dirty = false;

		try
		{
			this.writer.execute(() -> writeFile(text));
		}
		catch (RejectedExecutionException ex)
		{
			this.dirty = true;
			this.logger.warning("Could not save " + this.file.getName() + ": storage is already closed.");
		}
	}

	private void writeFile(String text)
	{
		Path target = this.file.toPath();
		Path temp = target.resolveSibling(this.file.getName() + ".tmp");
		try
		{
			Files.createDirectories(target.getParent());
			Files.writeString(temp, text, StandardCharsets.UTF_8);
			try
			{
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException ex)
			{
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
			this.knownModified = Files.getLastModifiedTime(target);
		}
		catch (IOException ex)
		{
			// Try again on the next flush
			this.dirty = true;
			this.logger.log(Level.SEVERE, "Could not save " + this.file.getName() + ": " + ex.getMessage());
		}
	}

	/** Turns configuration sections into plain maps, so values can be kept after the YamlConfiguration is gone */
	protected static Object toPlain(Object value)
	{
		if (value instanceof ConfigurationSection section)
		{
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			for (String key : section.getKeys(false))
			{
				map.put(key, toPlain(section.get(key)));
			}
			return map;
		}
		return value;
	}

	/** The keys of {@code section} other than {@code known}, as plain values */
	protected static Map<String, Object> extraKeys(ConfigurationSection section, String... known)
	{
		Map<String, Object> extras = new LinkedHashMap<String, Object>();
		outer:
		for (String key : section.getKeys(false))
		{
			for (String knownKey : known)
			{
				if (knownKey.equals(key))
				{
					continue outer;
				}
			}
			extras.put(key, toPlain(section.get(key)));
		}
		return extras;
	}

	protected static void writeExtras(ConfigurationSection section, Map<String, Object> extras)
	{
		for (Map.Entry<String, Object> entry : extras.entrySet())
		{
			section.set(entry.getKey(), entry.getValue());
		}
	}
}
