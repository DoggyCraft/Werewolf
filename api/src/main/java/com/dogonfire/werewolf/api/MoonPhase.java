package com.dogonfire.werewolf.api;

/**
 * The eight moon phases, in the order Minecraft cycles through them: the world's day number (full time / 24000)
 * modulo 8, with day 0 being a full moon. The constant names are config keys ({@code Moon.PhaseNames.<Phase>}), so
 * they never change; the names shown to players are configured there.
 */
public enum MoonPhase
{
	/** Full moon (day 0 of the cycle) */
	FullMoon("Full Moon"),
	/** Waning gibbous (day 1) */
	WaningGibbous("Waning Gibbous"),
	/** Last quarter (day 2) */
	LastQuarter("Last Quarter"),
	/** Waning crescent (day 3) */
	WaningCrescent("Waning Crescent"),
	/** New moon (day 4) */
	NewMoon("New Moon"),
	/** Waxing crescent (day 5) */
	WaxingCrescent("Waxing Crescent"),
	/** First quarter (day 6) */
	FirstQuarter("First Quarter"),
	/** Waxing gibbous (day 7) */
	WaxingGibbous("Waxing Gibbous");

	private final String defaultName;

	MoonPhase(String defaultName)
	{
		this.defaultName = defaultName;
	}

	/** The English name, used when config.yml doesn't name the phase */
	public String getDefaultName()
	{
		return defaultName;
	}

	/**
	 * Parses a phase name: the constant name in any case, with or without underscores, spaces or dashes
	 * ({@code FullMoon}, {@code full_moon}, {@code "Full Moon"}).
	 *
	 * @return the phase, or null if the name matches none
	 */
	public static MoonPhase fromString(String name)
	{
		if (name == null)
		{
			return null;
		}

		String normalized = name.replace("_", "").replace(" ", "").replace("-", "");
		for (MoonPhase phase : values())
		{
			if (phase.name().equalsIgnoreCase(normalized))
			{
				return phase;
			}
		}
		return null;
	}
}
