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
*	Start the server, then adjust `plugins/Werewolf/config.yml`. Commands and permissions are listed in [plugin.yml](src/resources/plugin.yml).

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

The jar ends up in `target/`.
