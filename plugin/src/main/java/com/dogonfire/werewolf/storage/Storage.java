package com.dogonfire.werewolf.storage;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Owns the data files and the single writer thread they share (one thread, so writes happen in order). Everything is
 * held in memory: {@link #load()} reads all files at startup, {@link #flush()} (main thread, every few minutes and on
 * /ww save) queues a write for each changed file, and {@link #close()} flushes and waits for the writes on disable.
 */
public final class Storage
{
	private static final long			CLOSE_TIMEOUT_SECONDS	= 15;

	private final Logger				logger;
	private final ExecutorService		writer;
	private final WerewolfRepository	werewolves;
	private final ClanRepository		clans;
	private final HunterRepository		hunters;
	private final TrophyRepository		trophies;
	private final List<YamlRepository>	repositories;

	public Storage(File dataFolder, Logger logger)
	{
		this.logger = logger;
		this.writer = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "Werewolf-Storage");
			thread.setDaemon(true);
			return thread;
		});
		this.werewolves = new WerewolfRepository(dataFolder, logger, this.writer);
		this.clans = new ClanRepository(dataFolder, logger, this.writer);
		this.hunters = new HunterRepository(dataFolder, logger, this.writer);
		this.trophies = new TrophyRepository(dataFolder, logger, this.writer);
		this.repositories = List.of(this.werewolves, this.clans, this.hunters, this.trophies);
	}

	public WerewolfRepository werewolves()
	{
		return this.werewolves;
	}

	public ClanRepository clans()
	{
		return this.clans;
	}

	public HunterRepository hunters()
	{
		return this.hunters;
	}

	public TrophyRepository trophies()
	{
		return this.trophies;
	}

	public void load()
	{
		for (YamlRepository repository : this.repositories)
		{
			repository.load();
		}
	}

	/** Main thread: queues a write for every file with unsaved changes. Does not wait. */
	public void flush()
	{
		for (YamlRepository repository : this.repositories)
		{
			repository.flush();
		}
	}

	/**
	 * /ww reload: re-reads the files that were edited on disk (or could not be read before), so hand edits win over
	 * unsaved changes; the other files keep what is in memory and are saved.
	 */
	public void reload()
	{
		awaitWrites(5);
		for (YamlRepository repository : this.repositories)
		{
			repository.reloadIfChanged();
		}
		flush();
	}

	/** Waits until every write queued so far has finished, or the timeout passes. Returns false on timeout. */
	private boolean awaitWrites(long timeoutSeconds)
	{
		if (this.writer.isShutdown())
		{
			return true;
		}

		Future<?> marker = this.writer.submit(() -> {
		});
		try
		{
			marker.get(timeoutSeconds, TimeUnit.SECONDS);
			return true;
		}
		catch (Exception ex)
		{
			this.logger.warning("Timed out waiting for data files to be saved.");
			return false;
		}
	}

	/** onDisable: flushes everything and waits for the writes to finish */
	public void close()
	{
		flush();
		this.writer.shutdown();
		try
		{
			if (!this.writer.awaitTermination(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
			{
				this.logger.severe("Data files were not saved within " + CLOSE_TIMEOUT_SECONDS + " seconds; some changes may be lost.");
			}
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
		}

		for (YamlRepository repository : this.repositories)
		{
			if (repository.isReadOnly())
			{
				this.logger.severe(repository.getFile().getName() + " could not be read at startup and was not saved. Fix it before the next start.");
			}
		}
	}
}
