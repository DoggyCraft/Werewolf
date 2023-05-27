package com.dogonfire.werewolf.disguises;

import java.util.UUID;

import com.dogonfire.werewolf.managers.ClanManager;
import org.bukkit.entity.Player;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.managers.ClanManager.ClanType;


// Let's abstract disguise commands away, so that we don't depend on a particular Disguise plugin
public interface IWerewolfDisguiseFactory
{
	abstract class WerewolfDisguise
	{
		String	werewolfName;
		UUID	accountId;
		String	accountName;
		String  texture;
		String  textureSignature;
		boolean isAlpha;
		ClanType clanType;

		public WerewolfDisguise(String werewolfName, ClanType clanType, boolean isAlpha) {
			this(clanType, isAlpha);

			if (werewolfName == null || werewolfName.isEmpty())
			{
				werewolfName = "Werewolf";
			}
			this.werewolfName = werewolfName;
		}

		public WerewolfDisguise(ClanType clanType, boolean isAlpha)
		{
			this.clanType = clanType;
			this.isAlpha = isAlpha;

			ClanManager clanManager = Werewolf.getClanManager();
			if (isAlpha) {
				this.accountId = clanManager.getWerewolfAccountIdForAlpha(clanType);
				this.accountName = clanManager.getWerewolfAccountForAlpha(clanType);
				this.texture = clanManager.getWerewolfTextureForAlpha(clanType);
				this.textureSignature = clanManager.getWerewolfTextureSignatureForAlpha(clanType);
			}
			else {
				this.accountName = clanManager.getWerewolfAccountForClan(clanType);
				this.accountId = clanManager.getWerewolfAccountIdForClan(clanType);
				this.texture = clanManager.getWerewolfTextureForClan(clanType);
				this.textureSignature = clanManager.getWerewolfTextureSignatureForClan(clanType);
			}
		}

		public String getWerewolfName()
		{
			return werewolfName;
		}

		public String getSkinAccountName()
		{
			return accountName;
		}

		public String getSkinTextureValue()
		{
			return texture;
		}

		public String getSkinTextureSignature()
		{
			return textureSignature;
		}

		public UUID getSkinAccountUUID()
		{
			return accountId;
		}

		public void setWerewolfName(String customName)
		{
			werewolfName = customName;
		}

		public void setSkinAccountName(String skinAccountname)
		{
			accountName = skinAccountname;
		}

		public void setSkinAccountUUID(UUID skinAccountId)
		{
			accountId = skinAccountId;
		}

		public void setSkinAccountUUID(String skinAccountId)
		{
			accountId = UUID.fromString(skinAccountId);
		}
		
		public abstract boolean disguise(Player player, String werewolfName);

		public abstract boolean undisguise(Player player);
	}

	WerewolfDisguise newDisguise(ClanType clanType, boolean isAlpha);

	boolean isEnabled();
} 
