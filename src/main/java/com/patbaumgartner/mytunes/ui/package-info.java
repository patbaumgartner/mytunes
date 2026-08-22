/**
 * The browser interface, built and driven from Java. Holds no JavaScript of its own; all
 * interop goes through the platform module.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(displayName = "UI",
		allowedDependencies = { "player", "stations", "themes", "persistence", "platform" })
package com.patbaumgartner.mytunes.ui;
