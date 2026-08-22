package com.patbaumgartner.mytunes.platform;

import org.graalvm.webimage.api.JSObject;
import org.graalvm.webimage.api.JSString;
import org.jspecify.annotations.Nullable;

/**
 * Application logging for a program whose only output device is a browser tab.
 * <p>
 * The usual SLF4J backends are excluded from this image, because Web Image cannot link
 * log4j's caller resolution and a browser has no files to write to. The browser console
 * is the real log sink here, and routing through it keeps diagnostics visible in
 * developer tools and in the console output the Playwright suite asserts on.
 */
public final class BrowserConsole {

	private static final @Nullable JSObject CONSOLE = Js.optional(BrowserWindow.window(), "console");

	public static void log(String message) {
		write("log", message);
	}

	public static void warn(String message) {
		write("warn", message);
	}

	private static void write(String level, String message) {
		JSObject console = CONSOLE;
		if (console == null) {
			return;
		}
		Js.call(console, level, JSString.of(message));
	}

	private BrowserConsole() {
	}

}
