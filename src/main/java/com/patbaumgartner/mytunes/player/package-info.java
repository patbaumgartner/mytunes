/**
 * The player state machine. Deliberately free of browser APIs so that playback logic can
 * be unit tested on a plain JVM.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(displayName = "Player",
		allowedDependencies = { "stations", "persistence" })
package com.patbaumgartner.mytunes.player;
