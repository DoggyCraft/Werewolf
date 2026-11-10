package com.dogonfire.werewolf.managers;


import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;

import net.milkbowl.vault.permission.Permission;

/**
 * Permission checks and permission groups. With Vault they go through Vault's permission provider, which is looked up on
 * use because the permissions plugin may enable after us. Permission checks use Bukkit directly.
 */
public class PermissionsManager
{
	private final Werewolf			plugin;
	private final WerewolfContext	ctx;

	public PermissionsManager(WerewolfContext ctx)
	{
		this.ctx = ctx;
		this.plugin = ctx.getPlugin();
	}

	private Permission vaultPermission()
	{
		if (!this.ctx.isVaultEnabled())
		{
			return null;
		}

		RegisteredServiceProvider<Permission> provider = this.plugin.getServer().getServicesManager().getRegistration(Permission.class);
		return provider != null ? provider.getProvider() : null;
	}

	public boolean hasPermission(Player player, String node)
	{
		// Bukkit's own check covers plugin.yml defaults, ops, parent/child nodes and any permissions plugin
		// (with or without Vault), so there's no need to go through Vault here
		return player.hasPermission(node);
	}

	/** The player's primary group, or "" without a Vault permission provider */
	public String getGroup(Player player)
	{
		Permission vaultPermission = vaultPermission();
		if (vaultPermission != null)
		{
			String group = vaultPermission.getPrimaryGroup(player);
			return group != null ? group : "";
		}
		return "";
	}

	public boolean isInGroup(Player player, String groupName)
	{
		Permission vaultPermission = vaultPermission();
		return vaultPermission != null && groupName != null && !groupName.isEmpty() && vaultPermission.playerInGroup(player, groupName);
	}

	public void addGroup(Player player, String groupName)
	{
		Permission vaultPermission = vaultPermission();
		if (vaultPermission != null)
		{
			vaultPermission.playerAddGroup(player, groupName);
		}
	}

	/**
	 * Takes the player out of the werewolf group (if they are in it) and puts them back in {@code originalGroup}, unless
	 * that is unknown or the werewolf group itself.
	 */
	public void leaveWerewolfGroup(Player player, String werewolfGroup, String originalGroup)
	{
		Permission vaultPermission = vaultPermission();
		if (vaultPermission == null)
		{
			return;
		}

		if (werewolfGroup != null && !werewolfGroup.isEmpty() && vaultPermission.playerInGroup(player, werewolfGroup))
		{
			vaultPermission.playerRemoveGroup(player, werewolfGroup);
		}

		if (originalGroup != null && !originalGroup.isEmpty() && !originalGroup.equalsIgnoreCase(werewolfGroup) && !vaultPermission.playerInGroup(player, originalGroup))
		{
			this.plugin.logDebug("Putting " + player.getName() + " into group " + originalGroup);
			vaultPermission.playerAddGroup(player, originalGroup);
		}
	}
}
