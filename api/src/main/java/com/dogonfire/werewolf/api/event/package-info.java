/**
 * Bukkit events fired by Werewolf, all on the main thread. Most are cancellable: they are fired before the change, at
 * the single place in the plugin where that change happens, so cancelling them vetoes it whatever caused it (the
 * game, an admin command or another plugin through {@link com.dogonfire.werewolf.api.WerewolfService}).
 */
package com.dogonfire.werewolf.api.event;
