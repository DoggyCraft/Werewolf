/**
 * Werewolf's public API: the stable surface other plugins may compile against. Get the {@link
 * com.dogonfire.werewolf.api.WerewolfService} from Bukkit's ServicesManager; listen to the events in
 * {@link com.dogonfire.werewolf.api.event}. Nothing outside these two packages is part of the API (the deprecated
 * {@code com.dogonfire.werewolf.WerewolfAPI} is kept only for old plugins).
 * <p>
 * Everything is main-thread only unless its documentation says it is thread-safe.
 */
package com.dogonfire.werewolf.api;
