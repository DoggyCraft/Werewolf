package com.dogonfire.werewolf.disguises;

import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;
import com.dogonfire.werewolf.api.Clan;
import com.dogonfire.werewolf.config.Settings;

// Let's abstract disguise commands away, so that we don't depend on a particular Disguise plugin
public interface IWerewolfDisguiseFactory
{
	/** The wolf form skin of one clan, or of its alpha. The texture is read from the settings each time it is used. */
	public abstract class WerewolfDisguise
	{
		protected final WerewolfContext	ctx;
		private final Clan				clan;
		private final boolean			alpha;

		public WerewolfDisguise(WerewolfContext ctx, Clan clan, boolean alpha)
		{
			this.ctx = ctx;
			this.clan = clan;
			this.alpha = alpha;
		}

		public Clan getClan()
		{
			return this.clan;
		}

		public boolean isAlpha()
		{
			return this.alpha;
		}

		/** The configured texture (Clans.Skins.&lt;Clan&gt;, or the clan's alpha skin) */
		public Settings.Skin getSkin()
		{
			return this.ctx.getSettings().clans().skin(this.clan, this.alpha);
		}

		@Override
		public String toString()
		{
			return (this.alpha ? "alpha " : "") + this.clan.name() + " skin";
		}

		/** Whether this texture value is one of the configured werewolf skins (any clan, alpha or not) */
		protected boolean isWerewolfTexture(String value)
		{
			if (value == null)
			{
				return false;
			}
			Settings.Clans clans = this.ctx.getSettings().clans();
			if (value.equals(clans.alphaSkin().value()))
			{
				return true;
			}
			for (Clan type : Clan.values())
			{
				if (value.equals(clans.skin(type, false).value()) || value.equals(clans.skin(type, true).value()))
				{
					return true;
				}
			}
			return false;
		}

		public abstract boolean disguise(Player player, String werewolfName);

		public abstract boolean undisguise(Player player);
	}

	public WerewolfDisguise newDisguise(Clan clan, boolean alpha);

	/** Called on /ww reload: backends that register the clan skins up front register them again. Others read them on use. */
	default void reloadSkins()
	{
	}

	/**
	 * On join, for a player not in wolf form: removes a werewolf skin the backend kept from before (a restart or crash
	 * while in wolf form). Backends whose skins don't outlive a session have nothing to do. Returns whether it did anything.
	 */
	default boolean clearLeftover(Player player)
	{
		return false;
	}
}
