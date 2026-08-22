/**
 * Browser and platform integration. Every JavaScript interop call in myTunes lives in
 * this module, so the domain modules stay free of DOM APIs and remain testable on a plain
 * JVM.
 */
@org.jspecify.annotations.NullMarked
@org.springframework.modulith.ApplicationModule(displayName = "Platform / Browser",
		allowedDependencies = { "persistence" })
package com.patbaumgartner.mytunes.platform;
