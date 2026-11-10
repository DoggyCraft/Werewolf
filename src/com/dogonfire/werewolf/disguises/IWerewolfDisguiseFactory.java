package com.dogonfire.werewolf.disguises;

import java.util.UUID;

import org.bukkit.entity.Player;

import com.dogonfire.werewolf.WerewolfContext;

// Let's abstract disguise commands away, so that we don't depend on a particular Disguise plugin
public interface IWerewolfDisguiseFactory
{
	public abstract class WerewolfDisguise
	{
		// Account called Werewolf. No clan has this skin...
		private static final UUID		DEFAULT_ACCOUNT_ID		= UUID.fromString("c21f069b-137c-4992-8edc-e1a12995ff0f");
		private static final String		DEFAULT_TEXTURE			= "eyJ0aW1lc3RhbXAiOjE1NjUxMjM3NTE5NDEsInByb2ZpbGVJZCI6IjQ1NjZlNjlmYzkwNzQ4ZWU4ZDcxZDdiYTVhYTAwZDIwIiwicHJvZmlsZU5hbWUiOiJUaGlua29mZGVhdGgiLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzRkMWUwOGIwYmI3ZTlmNTkwYWYyNzc1ODEyNWJiZWQxNzc4YWM2Y2VmNzI5YWVkZmNiOTYxM2U5OTExYWU3NSJ9LCJDQVBFIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjBjYzA4ODQwNzAwNDQ3MzIyZDk1M2EwMmI5NjVmMWQ2NWExM2E2MDNiZjY0YjE3YzgwM2MyMTQ0NmZlMTYzNSJ9fX0=";
		private static final String		DEFAULT_SIGNATURE		= "ef4Zb4mm9alUQUjk28gO85/Rug8DFYPyVAjT1hq+csjw23FVpCIQPvl3Bc4/zcDGIUiGGX/hi9G1U6LMcGyQg/4ONbMLFZtB7P1i9Dak/sIqZstMuBnWblK0//cdnFwEpZh+psdvsZUHOq6k37omwwc1wboIsqbe6r+23EZzZ9fBCWnk3qiYoWK+CMMhSJrhiamYIxjoMafJWeIkKhCUWiKrDJQw47NVnVTeEG4B8t4pRKs4L1wMsUavRgKQpWD9Lb/ivE0mNhQrLFT0IxEu7v5+e47OaEWhyL8OZ8/hcY/D7+Mf86M4jOP1AcKsvh4KdklHHZyrwSlGFcHKMvA6q1BLdj2BT4DlsqGF9eTv8aGwwXHxTi5L5JzeIwaE/6wZebolNWvZs/aXop2gMEn9yBFju82KeB89yrC75vUxHzH2zT6393/BV6le6m3mWRFAMJfXzLVSUaOK8CgBGnEOkc1Gy+LSXLayCgvBPzF0Rn0m0elIf/h4NV4pPhCe6Prh0lFKVpc5u2keihOhm7LpweBOBq8e4JN7eOUmojVKkAvAXWXYfAqjg49M0Uau3s+UeNqt+REcB9Di7J1++GKKtBBErJDChU6jt+kmkMJ+7frbIEV/1U2TA7FMIYQbvvZR3JJj9W9xI2L0GYadO/sAa1uYxjHiOTa12Jy4k6meobw=";

		protected final WerewolfContext	ctx;
		private final UUID				accountId;
		private final String			accountName;

		public WerewolfDisguise(WerewolfContext ctx, UUID accountId, String accountName)
		{
			this.ctx = ctx;
			ctx.getPlugin().logDebug("Creating disguise - AccountId: " + accountId + " - AccountName: " + accountName);
			this.accountId = accountId != null ? accountId : DEFAULT_ACCOUNT_ID;
			this.accountName = accountName.isEmpty() ? "Werewolf" : accountName;
		}

		public String getSkinAccountName()
		{
			return this.accountName;
		}

		public UUID getSkinAccountUUID()
		{
			return this.accountId;
		}

		public String getSkinTextureValue(Player player)
		{
			UUID playerId = player.getUniqueId();
			if (this.ctx.getClanManager().isAlpha(playerId))
			{
				return this.ctx.getClanManager().getWerewolfTextureForAlpha(this.ctx.getWerewolfManager().getWerewolfClan(playerId));
			}

			if (this.ctx.getWerewolfManager().isWerewolf(playerId) && this.ctx.getSettings().clans().enabled())
			{
				return this.ctx.getClanManager().getWerewolfTextureForClan(this.ctx.getWerewolfManager().getWerewolfClan(playerId));
			}
			return DEFAULT_TEXTURE;
		}

		public String getSkinTextureSignature(Player player)
		{
			UUID playerId = player.getUniqueId();
			if (this.ctx.getClanManager().isAlpha(playerId))
			{
				return this.ctx.getClanManager().getWerewolfTextureSignatureForAlpha(this.ctx.getWerewolfManager().getWerewolfClan(playerId));
			}

			if (this.ctx.getWerewolfManager().isWerewolf(playerId) && this.ctx.getSettings().clans().enabled())
			{
				return this.ctx.getClanManager().getWerewolfTextureSignatureForClan(this.ctx.getWerewolfManager().getWerewolfClan(playerId));
			}
			return DEFAULT_SIGNATURE;
		}

		public abstract boolean disguise(Player player, String werewolfName);

		public abstract boolean undisguise(Player player);
	}

	public WerewolfDisguise newDisguise(UUID disguiseAccountId, String disguiseAccountName);

	/** Called on /ww reload: backends that register the clan skins up front register them again. Others read them on use. */
	default void reloadSkins()
	{
	}
}
