package com.dogonfire.werewolf.managers;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfContext;

import net.milkbowl.vault.permission.Permission;

/**
 * Permission checks and permission groups. With Vault they go through Vault's permission provider, which is looked up on
 * use because the permissions plugin may enable after us; without one, checks fall back to the plugin.yml defaults.
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
		this.plugin.logDebug("Checking for perm.. Player: " + player.getName() + " - Node: " + node);
		Permission vaultPermission = vaultPermission();
		if (vaultPermission != null)
		{
			this.plugin.logDebug("Vault is enabled, checking there...");
			return vaultPermission.has(player, node);
		}

		// We check the permissions the other way, using the defaults given through plugin.yml
		this.plugin.logDebug("No vault. Checking the old way..");
		List<org.bukkit.permissions.Permission> permissions = this.plugin.getDescription().getPermissions();

		for (org.bukkit.permissions.Permission permission : permissions)
		{
			Map<String, Boolean> permissionChildren = permission.getChildren();

			if ((permissionChildren.containsKey(node) && permissionChildren.get(node).booleanValue() == true) && permission.getName().contains(node))
			{
				this.plugin.logDebug("Heyy, it was the correct perm finally... Perm: " + permission.getName() + " - Node: " + node);
				this.plugin.logDebug("Current permissionDefault: " + permission.getDefault().toString());
				if (permission.getDefault() == PermissionDefault.TRUE)
				{
					this.plugin.logDebug("It was default true! Returning true!");
					return true;
				}
				else if (permission.getDefault() == PermissionDefault.OP)
				{
					this.plugin.logDebug("It was for operators!");
					if (player.isOp())
					{
						this.plugin.logDebug("Player is an operator! Returning true!");
						return true;
					}
					this.plugin.logDebug("Player is not an operator!");
				}
				else if (permission.getDefault() == PermissionDefault.NOT_OP)
				{
					this.plugin.logDebug("It was for non-operators!");
					if (!player.isOp())
					{
						this.plugin.logDebug("Player is not an operator! Returning true!");
						return true;
					}
					this.plugin.logDebug("Player is an operator!");
				}
				break;
			}
		}
		this.plugin.logDebug("Returning false!");
		return false;
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
