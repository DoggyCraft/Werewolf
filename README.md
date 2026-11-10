Werewolf
======

![Werewolf](https://raw.githubusercontent.com/DoggyCraft/Werewolf/master/img/Werewolf.jpg)

The plugin for adding Werewolves into Minecraft!

Background
---------
To become a Werewolf, you have to get bitten by a wolf or a Werewolf at night and contract the Werewolf infection. From the next night on, the infection turns the player into a Werewolf whenever they are under an open sky. To avoid turning, stay inside at night.

*	In wolf form, the player's skin changes to a Werewolf skin
*	In wolf form, players only show up under their werewolf name in the player list and chat
*	In wolf form, werewolves speak in a language that sounds like growling to normal players
*	A Werewolf turns into wolf form during a full moon when under an open sky, and back into human form at dawn
*	Werewolves grow stronger with every full moon, unlocking perks like controlled transformation and gold immunity
*	A Werewolf only eats meat, and gains health from hunting mobs
*	A Werewolf hits hard unarmed, moves faster, jumps higher and has night vision
*	A Werewolf can't wear armor in wolf form, but has a high natural defense
*	A Werewolf can growl with /growl and howl with /howl
*	Werewolves are pack creatures, live in clans led by an alpha, and lead wild wolves
*	Werewolves are vulnerable to silver weapons and wolfbane
*	Werewolves and vampires (VampireRevamp) exclude each other: vampires can't catch the werewolf infection, and werewolf blood rejects vampirism
*	The infection can be cured with the werewolf cure potion

Installation
---------
*	Run a [Paper](https://papermc.io/) (recommended) or Spigot server on Minecraft 26.1 or newer, with Java 25.
*	Download `Werewolf-<version>.jar` from the [releases](https://github.com/DoggyCraft/Werewolf/releases) and drop it into the `plugins` folder.
*	Werewolf skins work out of the box on Paper. On Spigot, also install [LibsDisguises](https://www.spigotmc.org/resources/libs-disguises.32453/) or [SkinsRestorer](https://modrinth.com/plugin/skinsrestorer).
*	Optional: [Vault](https://www.spigotmc.org/resources/vault.34315/) for bounties and shop signs, [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/), [VampireRevamp](https://www.spigotmc.org/resources/vampirerevamp.75479/).
*	Start the server, then adjust `plugins/Werewolf/config.yml`. Commands and permissions are listed in [plugin.yml](plugin/src/main/resources/plugin.yml).

Links
---------
*	[Werewolf at SpigotMC](https://www.spigotmc.org/resources/werewolf.7442/)
*	[Releases](https://github.com/DoggyCraft/Werewolf/releases)
*	[License (GPLv3)](LICENSE)

Building
---------
Werewolf is built with Maven and JDK 25:

```bash
git clone https://github.com/DoggyCraft/Werewolf.git
cd Werewolf
mvn clean package        # or: nix develop -c mvn clean package
```

The plugin jar ends up in `plugin/target/Werewolf-<version>.jar`, the API jars (with sources and javadoc) in `api/target/`.

API
---------
Other plugins can read werewolf state, change it through the same code paths the game uses, and listen to (and veto) Werewolf's events. The public API is the `com.dogonfire.werewolf.api` and `com.dogonfire.werewolf.api.event` packages; everything else is internal and may change.

### Dependency

Depend on the `werewolf-api` artifact (it only contains the API packages, so you can't accidentally use internals) with `provided` scope, since Werewolf itself supplies those classes at runtime, and add `depend: [Werewolf]` (or `softdepend`) to your `plugin.yml`.

It is published through [JitPack](https://jitpack.io/#DoggyCraft/Werewolf) (no account or token needed; use a release tag as the version):

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.DoggyCraft.Werewolf</groupId>
    <artifactId>werewolf-api</artifactId>
    <version>0.12.0</version>
    <scope>provided</scope>
</dependency>
```

The API is compiled against the Spigot API, so it works on both Spigot and Paper.

### Using the service

`WerewolfService` is registered in Bukkit's services manager while Werewolf is enabled:

```java
RegisteredServiceProvider<WerewolfService> rsp = getServer().getServicesManager().getRegistration(WerewolfService.class);
if (rsp == null)
{
    return; // Werewolf is not installed or not enabled
}
WerewolfService werewolves = rsp.getProvider();

Optional<WerewolfProfile> profile = werewolves.getWerewolf(player.getUniqueId());
profile.ifPresent(p -> getLogger().info(player.getName() + " is a level " + p.level() + " " + werewolves.getClanName(p.clan()) + " werewolf"));

// Actions fire the normal events with an API reason, so other plugins can veto them
ActionResult result = werewolves.infect(player, Clan.Potion, false);
if (result == ActionResult.CANCELLED)
{
    player.sendMessage("Something protected you from the infection");
}

player.getInventory().addItem(werewolves.createItem(WerewolfItem.SILVER_SWORD));
```

Reads: `getWerewolf`, `isWerewolf`, `isInWolfForm`, `isHuman`, `isVampire`, `getWerewolves()`, `getWerewolves(Clan)`, `getAlpha`, `getClanName`, `getMoonPhase`, `isTransformationNight`, `isWerewolvesEnabled`. Actions (each returns an `ActionResult`): `infect`, `setFullWerewolf`, `cure`, `transform`, `untransform`, `setLevel`, `setAlpha`. Items: `createItem(WerewolfItem)`, `getItemType(ItemStack)`.

**Threading:** call the service on the main server thread. Only `getWerewolf`, `isWerewolf`, `isInWolfForm`, `getWerewolves`, `getAlpha` and `getClanName` are thread-safe (they read a snapshot), so they may be used from async chat handlers. Actions called off the main thread throw `IllegalStateException`.

### Events

All in `com.dogonfire.werewolf.api.event`, fired on the main thread. The cancellable ones are fired before the change, so cancelling vetoes it:

| Event | When | Notes |
|---|---|---|
| `WerewolfInfectionEvent` | A player is about to become a werewolf | `getSource()`, `getClan()`/`setClan()`, `getChance()`/`setChance()` (bites fire before the infection roll), `getBiter()` |
| `WerewolfCureEvent` | A werewolf is about to be cured | `getCause()`; the player may be offline |
| `WerewolfTransformEvent` | A werewolf is about to change form | `isToWolfForm()`, `getReason()`; can't be cancelled when `isCancellable()` is false (quit, death) |
| `WerewolfLevelChangeEvent` | A werewolf's level is about to change | `getOldLevel()`, `getNewLevel()`/`setNewLevel()`, `getCause()` |
| `NewAlphaEvent` | A clan is about to get a new alpha | `getOldAlphaId()`, `getNewAlphaId()`, `getCause()` |
| `TrackerTargetEvent` | A hunter's tracker compass locks onto or updates a target | Cancel to skip or lose the target |
| `WerewolfSniffEvent` | A werewolf sniffs a player to pick up their scent | Cancel to stop the sniff |

The old static `com.dogonfire.werewolf.WerewolfAPI` still works but is deprecated; use the service instead. It is not part of `werewolf-api`; code that still needs it has to depend on the full plugin artifact (`com.github.DoggyCraft.Werewolf:werewolf`).

### PlaceholderAPI

`%werewolf_werewolf%` ("Werewolf" or empty), `%werewolf_clan%` / `%werewolf_clan_name%` (clan display name), `%werewolf_clan_id%`, `%werewolf_form%` ("Wolf", "Human" or empty), `%werewolf_level%`, `%werewolf_alpha%` (true/false), `%werewolf_infected%` (true/false), `%werewolf_name%` (name in wolf form) and `%werewolf_moonphase%` (the moon phase in the player's world).
