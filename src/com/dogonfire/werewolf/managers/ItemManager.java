package com.dogonfire.werewolf.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.dogonfire.werewolf.items.Recipes;
import com.dogonfire.werewolf.Werewolf;
import com.dogonfire.werewolf.WerewolfAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class ItemManager
{
	/** The silver armor pieces. Each piece has its own infection protection (purity) and defense against supernatural attackers, set in config.yml (Items.SilverArmor). */
	public enum ArmorPiece
	{
		HELMET(Material.IRON_HELMET, "Helmet", "SilverHelmetRecipe"),
		CHESTPLATE(Material.IRON_CHESTPLATE, "Chestplate", "SilverArmorRecipe"),
		LEGGINGS(Material.IRON_LEGGINGS, "Leggings", "SilverLeggingsRecipe"),
		BOOTS(Material.IRON_BOOTS, "Boots", "SilverBootsRecipe");

		private final Material	material;
		private final String	configName;
		private final String	recipeKey;

		ArmorPiece(Material material, String configName, String recipeKey)
		{
			this.material = material;
			this.configName = configName;
			this.recipeKey = recipeKey;
		}

		public Material getMaterial()
		{
			return material;
		}

		public String getConfigName()
		{
			return configName;
		}

		public String getRecipeKey()
		{
			return recipeKey;
		}

		public ItemStack getWorn(PlayerInventory inventory)
		{
			switch (this)
			{
				case HELMET:
					return inventory.getHelmet();
				case CHESTPLATE:
					return inventory.getChestplate();
				case LEGGINGS:
					return inventory.getLeggings();
				default:
					return inventory.getBoots();
			}
		}

		public void setWorn(PlayerInventory inventory, ItemStack item)
		{
			switch (this)
			{
				case HELMET:
					inventory.setHelmet(item);
					break;
				case CHESTPLATE:
					inventory.setChestplate(item);
					break;
				case LEGGINGS:
					inventory.setLeggings(item);
					break;
				default:
					inventory.setBoots(item);
					break;
			}
		}
	}

	private Werewolf plugin;
	private NamespacedKey namespacedKey;

	public ItemManager(Werewolf plugin)
	{
		this.plugin = plugin;
		this.namespacedKey = new NamespacedKey(plugin, "ww-item");
	}

	private boolean isWerewolfItem(ItemStack item, String itemName)
	{
		if (item == null || !item.hasItemMeta())
		{
			return false;
		}
		ItemMeta itemMeta = item.getItemMeta();
		if (itemMeta == null)
		{
			return false;
		}
		PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
		return dataContainer.has(namespacedKey, PersistentDataType.STRING) && Objects.equals(dataContainer.get(namespacedKey, PersistentDataType.STRING), itemName);
	}
	
	/** Marks a werewolf head trophy, so placing it remembers its lore (TrophyManager) */
	public void tagWerewolfTrophy(ItemMeta itemMeta)
	{
		itemMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, "WerewolfTrophy");
	}

	public boolean isWerewolfTrophy(ItemStack item)
	{
		return isWerewolfItem(item, "WerewolfTrophy");
	}

	public void setupRecipes()
	{
		if(plugin.getSettings().items().craftableInfectionPotion())
		{			
			Bukkit.addRecipe(Recipes.infectionPotion(plugin));
			plugin.logDebug("Craftable infection potions are enabled");
		}
		
		if(plugin.getSettings().items().craftableCurePotion())
		{
			Bukkit.addRecipe(Recipes.curePotion(plugin));
			plugin.logDebug("Craftable cure potions are enabled");
		}

		if(plugin.getSettings().items().craftableWolfbanePotion())
		{
			Bukkit.addRecipe(Recipes.wolfbanePotion(plugin));
			plugin.logDebug("Craftable wolfbane potions are enabled");
		}

		if(plugin.getSettings().items().craftableSilverSword())
		{
			Bukkit.addRecipe(Recipes.silverSwordRecipe(plugin));
			plugin.logDebug("Craftable silver swords are enabled");
		}

		if(plugin.getSettings().items().craftableSilverArmor())
		{
			for (ArmorPiece piece : ArmorPiece.values())
			{
				Bukkit.addRecipe(Recipes.silverArmorRecipe(plugin, piece));
			}
			plugin.logDebug("Craftable silver armors are enabled");
		}

		if(plugin.getSettings().items().craftableWerewolfTracker())
		{
			Bukkit.addRecipe(Recipes.werewolfTracker(plugin));
			plugin.logDebug("Craftable werewolf trackers are enabled");
		}

		if(plugin.getSettings().items().craftableVampireTracker() && plugin.vampireEnabled)
		{
			Bukkit.addRecipe(Recipes.vampireTracker(plugin));
			plugin.logDebug("Craftable vampire trackers are enabled");
		}

		if(plugin.getSettings().items().craftableLoreBook())
		{
			Bukkit.addRecipe(Recipes.loreBook(plugin));
			plugin.logDebug("Craftable lore books are enabled");
		}
	}

	public ItemStack newInfectionPotion()
	{
		ItemStack potion = new ItemStack(Material.POTION);

		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		potionMeta.setDisplayName(ChatColor.GOLD + Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.InfectionPotionTitle, ChatColor.GOLD));

	    List<String> pages = new ArrayList<String>();
	    
	    pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.InfectionPotionDescription1, ChatColor.GRAY));
	    pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.InfectionPotionDescription2, ChatColor.GRAY));
	    
	    potionMeta.setLore(pages);
	    //potionMeta.setMainEffect(PotionEffectType.NAUSEA);
	    potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 0), true);

		// Add NBT data
		potionMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, "InfectionPotion");

		potion.setItemMeta(potionMeta);

		return potion;
	}

	public ItemStack newCurePotion()
	{
		ItemStack potion = new ItemStack(Material.POTION);

		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		potionMeta.setDisplayName(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CurePotionTitle, ChatColor.GOLD));

	    List<String> pages = new ArrayList<String>();
	    
	    pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CurePotionDescription1, ChatColor.GRAY));
	    pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CurePotionDescription2, ChatColor.GRAY));
	    
	    potionMeta.setLore(pages);
	    //potionMeta.setMainEffect(PotionEffectType.NAUSEA);
	    potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 0), true);

		// Add NBT data
		potionMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, "CurePotion");

		potion.setItemMeta(potionMeta);

		return potion;
	}
	
	public ItemStack newWolfbanePotion()
	{
		ItemStack potion = new ItemStack(Material.SPLASH_POTION);
		//Potion potion = new Potion(PotionType.POISON, 2);
		//potion.setSplash(true);
		
		//ItemStack itemStack = potion.toItemStack(1);

		//PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		potionMeta.setDisplayName(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WolfbanePotionTitle, ChatColor.GOLD));

	    List<String> pages = new ArrayList<String>();

	    Werewolf.getLanguageManager().setAmount(String.valueOf(plugin.getSettings().items().wolfbaneUntransformChance()));
	    pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WolfbanePotionDescription1, ChatColor.LIGHT_PURPLE));
	    pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.WolfbanePotionDescription2, ChatColor.GRAY));
	    
	    potionMeta.setLore(pages);
	    potionMeta.setBasePotionType(PotionType.POISON);
	    //potionMeta.setMainEffect(PotionEffectType.NAUSEA);
	    potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.NAUSEA, 300, 0), true);

	    //itemStack.setItemMeta(potionMeta);
		
		//return itemStack;

		// Add NBT data
		potionMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, "WolfbanePotion");
	    
	    potion.setItemMeta(potionMeta);
		
		return potion;
	}

	public ItemStack newSilverSword(int level)
	{
		ItemStack weapon = new ItemStack(Material.IRON_SWORD);

		ItemMeta itemMeta = weapon.getItemMeta();

		itemMeta.setDisplayName(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverSwordTitle, ChatColor.GOLD));

		List<String> pages = new ArrayList<String>();

	    Werewolf.getLanguageManager().setAmount(String.valueOf(plugin.getSettings().items().silverSwordMultiplier()));
	    pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverSwordDescription1, ChatColor.LIGHT_PURPLE));
	    pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverSwordDescription2, ChatColor.GRAY));

		itemMeta.setLore(pages);

		// Add NBT data
		itemMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, "SilverSword");

		weapon.setItemMeta(itemMeta);

		return weapon;
	}

	/** The silver chestplate */
	public ItemStack newSilverArmor(int level)
	{
		return newSilverArmor(ArmorPiece.CHESTPLATE);
	}

	/** All pieces share the "SilverArmor" tag; which piece it is follows from the slot it is worn in */
	public ItemStack newSilverArmor(ArmorPiece piece)
	{
		ItemStack armor = new ItemStack(piece.getMaterial());

		ItemMeta itemMeta = armor.getItemMeta();

		itemMeta.setDisplayName(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverArmorTitle, ChatColor.GOLD));

		List<String> pages = new ArrayList<String>();

		Werewolf.getLanguageManager().setAmount(String.valueOf(plugin.getSettings().items().silverArmorPurity().get(piece)));
		pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverArmorPurity, ChatColor.LIGHT_PURPLE));
		Werewolf.getLanguageManager().setAmount(String.valueOf(plugin.getSettings().items().silverArmorDefense().get(piece)));
		pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverArmorDefense, ChatColor.LIGHT_PURPLE));
		pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverArmorHumansOnly, ChatColor.GRAY));

		itemMeta.setLore(pages);

		// Add NBT data
		itemMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, "SilverArmor");

		armor.setItemMeta(itemMeta);

		return armor;
	}

	public ItemStack newWerewolfTracker()
	{
		return newTracker("WerewolfTracker", LanguageManager.LANGUAGESTRING.WerewolfTrackerTitle, LanguageManager.LANGUAGESTRING.WerewolfTrackerDescription);
	}

	public ItemStack newVampireTracker()
	{
		return newTracker("VampireTracker", LanguageManager.LANGUAGESTRING.VampireTrackerTitle, LanguageManager.LANGUAGESTRING.VampireTrackerDescription);
	}

	private ItemStack newTracker(String tag, LanguageManager.LANGUAGESTRING title, LanguageManager.LANGUAGESTRING description)
	{
		ItemStack tracker = new ItemStack(Material.COMPASS);

		ItemMeta itemMeta = tracker.getItemMeta();

		itemMeta.setDisplayName(Werewolf.getLanguageManager().getLanguageString(title, ChatColor.GOLD));

		List<String> pages = new ArrayList<String>();
		pages.add(Werewolf.getLanguageManager().getLanguageString(description, ChatColor.LIGHT_PURPLE));
		pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrackerClearDescription, ChatColor.GRAY));
		pages.add(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.TrackerHumansOnly, ChatColor.GRAY));

		itemMeta.setLore(pages);

		itemMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, tag);

		tracker.setItemMeta(itemMeta);

		return tracker;
	}

	public ItemStack newLoreBook()
	{
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);

		// Add pages
		List<String> bookPages = new ArrayList<String>();

		bookPages.add("§lThe Werewolf infection\n§r\nThe werewolf curse is a ancient curse carried by some wolves who only come out at night.\n§r\nAn human infected with the werewolf curse will turn into his werewolf form every fullmoon.");
		
		if(plugin.getSettings().clans().enabled())
		{
			bookPages.add("§lWerewolves in combat\n§r\nThe werewolf is a ferocious creature. A werewolf does " + plugin.getSettings().wolfForm().handDamage() + " damage with his hands and " + plugin.getSettings().wolfForm().itemDamage() + " with an item in his hands. Because of his unique physique, a werewolf takes " + 100*plugin.getSettings().wolfForm().armorMultiplier() + " %  damage\n§r\n");		
		}
		else
		{
			bookPages.add("§lWerewolves in combat\n§r\nThe werewolf is a ferocious creature. A werewolf does " + plugin.getSettings().wolfForm().handDamage() + " damage with his hands and " + plugin.getSettings().wolfForm().itemDamage() + " with an item in his hands. Because of his unique physique, a werewolf takes " + 100*plugin.getSettings().wolfForm().armorMultiplier() + " %  damage\n§r\nThe leader of a clan, an alpha werewolf, takes " + 100*plugin.getSettings().wolfForm().armorMultiplier()/2 + " %  damage and does " + 2*plugin.getSettings().wolfForm().handDamage() + "  damage!");					
		}		
		
		bookPages.add("§lTransformation\n§r\nDuring transformation, a werewolf will drop his clothes to the ground. But for each fullmoon, a werewolf will gain more control over his condition.\nAfter having experienced enough fullmoons, the werewolf may be able");
		bookPages.add("§rto not drop his clothes or even transform at will!");

		bookPages.add("§lGetting infected\n§r\nYou can be infected in 3 ways:\n§r\n 1) Getting bitten by a wolf at night\n§r\n 2) Drinking a werewolf potion\n§r\n 3) Being bitten by a werewolf.");
		bookPages.add("§lWerewolf language\n§r\nWhen in werewolf form, werewolves speak in a language only understood by themselves.\n§r\nA non-infected player will only hear wolf noises when they chat.");
		
		if(plugin.getSettings().clans().enabled())
		{
			bookPages.add("§lClans\n§r\nWerewolves are flock animals and they live in groups. There are three werewolf clans fighting for power.\n§r\nWerewolf players can check their clan info with the /ww clan command.");
			bookPages.add("§lThe Witherfangs\n§r\nWhen a human drinks a werewolf potion, he will turn into a Witherfang werewolf.\n§r\nWitherfang werewolves has yellow eyes, grey fur are fast and can jump higher than other werewolves.");
			bookPages.add("§lThe Silvermanes\n§r\nWhen a human is bitten by a wild wolf, he will turn into a Silvermane werewolf.\n§r\nSilvermane werewolves has blue eyes, grey fur and can take more damage than other werewolves.");
			bookPages.add("§lThe Bloodmoons\n§r\nWhen a human is bitten by a werewolf, he will turn into a Bloodmoon werewolf.\n§r\nThe Bloodmoon werewolves has red eyes, black fur and do more damage than other werewolves.");
		}
		
		if(plugin.getSettings().general().trophiesEnabled())
		{
			bookPages.add("§lTrophies\n§r\nIf you able to slay a werewolf using a sword, you may be able to cut the of the werewolf and keep it as a trophy!");
		}

		bookPages.add("§lCuring the infection\n§r\nA werewolf can drink the werewolf cure potion to cure himself.");		
		if(plugin.getSettings().items().craftableCurePotion())
		{
			bookPages.add("§lCrafting the Cure\n§r\nIn a workbench, place the following ingredients:");			
			
			int n = 1;
			for(ItemStack ingredient : Recipes.curePotion(plugin).getIngredientList())
			{
				bookPages.add("\n§r\n " + n + ") " + ingredient.getType().name()); 
				n++;
			}			
		}		

		if(plugin.getSettings().items().craftableInfectionPotion())
		{
			bookPages.add("§lThe infection potion\n§r\nA potion can be made that infects the drinker with the werewolf infection. This special potion must be used during a fullmoon.");		
			bookPages.add("§lCrafting the Infection\n§r\nIn a workbench, place the following ingredients:");			
			
			int n = 1;
			for(ItemStack ingredient : Recipes.infectionPotion(plugin).getIngredientList())
			{
				bookPages.add("\n§r\n " + n + ") " + ingredient.getType().name()); 
				n++;
			}			
		}		

		if(plugin.getSettings().items().craftableWolfbanePotion())
		{
			bookPages.add("§lThe wolfbane potion\n§r\nA potion can be made that severely hurts the werewolf and has a chance to untransform him into human form. This special potion must be used during a fullmoon.");		
			bookPages.add("§lCrafting the Wolfbane\n§r\nIn a workbench, place the following ingredients:");			
			
			int n = 1;
			for(ItemStack ingredient : Recipes.wolfbanePotion(plugin).getIngredientList())
			{
				bookPages.add("\n§r\n " + n + ") " + ingredient.getType().name()); 
				n++;
			}			
		}		
		
		bookPages.add("§lHunting werewolves\n§r\nWerewolves are vulnerable to silver. Hitting a werewolf with a silver sword will do x" + plugin.getSettings().items().silverSwordMultiplier() + " damage compared to a normal sword.\n§r\nOnly a werewolf hunter using the /ww hunt command can use this item.");
		if(plugin.getSettings().items().craftableSilverSword())
		{
			bookPages.add("§lThe Silver Sword\n§r\nBecause a werewolf is especially vulnerable to silver, a sword can be made that severely hurts the werewolf.");		
			bookPages.add("§lCrafting the Silver Sword\n§r\nIn a workbench, place the following ingredients:");			
			
			int n = 1;
			for(ItemStack ingredient : Recipes.silverSwordRecipe(plugin).getIngredientMap().values())
			{
				if(ingredient!=null)
				{
					bookPages.add("\n§r\n " + n + ") " + ingredient.getType().name()); 
					n++;
				}
			}						
		}
								
		int purity = 0;
		int defense = 0;
		for (ArmorPiece piece : ArmorPiece.values())
		{
			purity += plugin.getSettings().items().silverArmorPurity().get(piece);
			defense += plugin.getSettings().items().silverArmorDefense().get(piece);
		}
		bookPages.add("§lHunting werewolves\n§r\nSilver armor keeps the werewolf curse at bay. A full set gives a " + Math.min(100, purity) + "% chance to resist a werewolf bite and takes " + Math.min(100, defense) + "% off the damage of werewolf attacks.\n§r\nWerewolves cannot wear silver.");
		if(plugin.getSettings().items().craftableSilverArmor())
		{
			bookPages.add("§lThe Silver Armor\n§r\nAny iron armor piece can be turned into silver armor in a workbench, surrounded by the following ingredients:");

			int n = 1;
			for(ItemStack ingredient : Recipes.silverArmorRecipe(plugin).getIngredientMap().values())
			{
				if(ingredient!=null)
				{
					bookPages.add("\n§r\n " + n + ") " + ingredient.getType().name());
					n++;
				}
			}
		}

		if(plugin.getSettings().items().craftableWerewolfTracker())
		{
			bookPages.add("§lThe Werewolf Tracker\n§r\nA compass surrounded by eight rabbit feet points a hunter to the nearest werewolf.\n§r\nRight-click it to lock onto a werewolf, left-click it to let go. Only humans can use it.");
		}

		BookMeta bookMeta = (BookMeta) book.getItemMeta();
		bookMeta.setTitle(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.LoreBookTitle, ChatColor.GOLD));//ChatColor.GOLD + "The Big Book of Werewolves");
		bookMeta.setAuthor("DogOnFire");
		bookMeta.setPages(bookPages);
		book.setItemMeta(bookMeta);

		// Add title
		ItemMeta itemMeta = book.getItemMeta();

		itemMeta.setDisplayName(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.LoreBookTitle, ChatColor.GOLD));

		// Add description
		List<String> pages = new ArrayList<String>();

		pages.add(ChatColor.GRAY + "All you ever wanted to know about werewolves");
		pages.add(ChatColor.GRAY + "but were afraid to ask");

		itemMeta.setLore(pages);

		// Add NBT data
		itemMeta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, "LoreBook");

		book.setItemMeta(itemMeta);

		return book;
	}

	public boolean isSilverSword(ItemStack IS)
	{
		return isWerewolfItem(IS, "SilverSword");
	}

	public boolean isCurePotion(ItemStack IS)
	{
		return isWerewolfItem(IS, "CurePotion");
	}

	public boolean isInfectionPotion(ItemStack IS)
	{
		return isWerewolfItem(IS, "InfectionPotion");
	}
	
	public boolean isWolfbanePotion(ItemStack IS)
	{
		return isWerewolfItem(IS, "WolfbanePotion");
	}

	public boolean isLoreBook(ItemStack IS)
	{
		return isWerewolfItem(IS, "LoreBook");
	}

	public boolean isSilverArmor(ItemStack IS)
	{
		return isWerewolfItem(IS, "SilverArmor");
	}

	public boolean isWerewolfTracker(ItemStack IS)
	{
		return isWerewolfItem(IS, "WerewolfTracker");
	}

	public boolean isVampireTracker(ItemStack IS)
	{
		return isWerewolfItem(IS, "VampireTracker");
	}

	public boolean isWearingSilverArmor(Player player)
	{
		for (ArmorPiece piece : ArmorPiece.values())
		{
			if (isSilverArmor(piece.getWorn(player.getInventory())))
			{
				return true;
			}
		}
		return false;
	}

	/** Summed purity of the silver armor worn: the % chance to resist a werewolf or vampire infection */
	public int getSilverArmorPurity(Player player)
	{
		return sumWornSilverArmor(player, plugin.getSettings().items().silverArmorPurity());
	}

	/** Summed defense of the silver armor worn: the % of damage taken off attacks from werewolves and vampires */
	public int getSilverArmorDefense(Player player)
	{
		return sumWornSilverArmor(player, plugin.getSettings().items().silverArmorDefense());
	}

	private int sumWornSilverArmor(Player player, Map<ArmorPiece, Integer> values)
	{
		int total = 0;
		for (ArmorPiece piece : ArmorPiece.values())
		{
			if (isSilverArmor(piece.getWorn(player.getInventory())))
			{
				Integer value = values.get(piece);
				total += value != null ? value : 0;
			}
		}
		return Math.max(0, Math.min(100, total));
	}

	/**
	 * Takes silver armor off a player who may not wear it (anyone but a human), putting it in their inventory or
	 * dropping it if there is no room. Returns true if anything was taken off.
	 */
	public boolean removeForbiddenSilverArmor(Player player)
	{
		if (WerewolfAPI.isHuman(player))
		{
			return false;
		}

		boolean removed = false;
		PlayerInventory inventory = player.getInventory();
		for (ArmorPiece piece : ArmorPiece.values())
		{
			ItemStack worn = piece.getWorn(inventory);
			if (!isSilverArmor(worn))
			{
				continue;
			}

			piece.setWorn(inventory, null);
			for (ItemStack leftover : inventory.addItem(worn).values())
			{
				player.getWorld().dropItemNaturally(player.getLocation(), leftover);
			}
			removed = true;
		}

		if (removed)
		{
			player.sendMessage(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverArmorCannotWear, ChatColor.RED));
		}
		return removed;
	}

	public boolean isHoldingSilverSword(Player player)
	{
		return isSilverSword(player.getInventory().getItemInMainHand()) || isSilverSword(player.getInventory().getItemInOffHand());
	}

	/** Runs once a second: werewolves and vampires can't wear silver armor, and silver burns werewolves holding a silver sword */
	public void update()
	{
		for (Player player : plugin.getServer().getOnlinePlayers())
		{
			boolean werewolf = Werewolf.getWerewolfManager().isWerewolf(player);
			if (isWearingSilverArmor(player))
			{
				removeForbiddenSilverArmor(player);
			}

			if (!werewolf || plugin.getSettings().items().silverSwordPenalty() <= 0 || player.isDead() || !isHoldingSilverSword(player))
			{
				continue;
			}

			player.damage(plugin.getSettings().items().silverSwordPenalty());
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Werewolf.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.SilverBurnsWerewolf, ChatColor.RED)));
		}
	}
}
