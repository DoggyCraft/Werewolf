package com.dogonfire.werewolf.api;

/**
 * The eight moon phases, in the order Minecraft cycles through them: the world's day number (full time / 24000)
 * modulo 8, with day 0 being a full moon. The constant names are config keys ({@code Moon.PhaseNames.<Phase>}), so
 * they never change; the names shown to players are configured there.
 */
public enum MoonPhase
{
	FullMoon("Full Moon"),
	WaningGibbous("Waning Gibbous"),
	LastQuarter("Last Quarter"),
	WaningCrescent("Waning Crescent"),
	NewMoon("New Moon"),
	WaxingCrescent("Waxing Crescent"),
	FirstQuarter("First Quarter"),
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
