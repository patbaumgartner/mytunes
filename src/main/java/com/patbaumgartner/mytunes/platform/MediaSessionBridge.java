package com.patbaumgartner.mytunes.platform;

import java.util.function.Consumer;

import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;

/**
 * Publishes playback metadata and transport actions to the operating system through the
 * Media Session API, which is what drives lock-screen, notification-shade and keyboard
 * media keys.
 * <p>
 * Every member is feature detected. Where {@code navigator.mediaSession} is absent the
 * methods do nothing, so an unsupported browser loses the operating-system integration
 * and keeps the player.
 */
public class MediaSessionBridge {

	private final @Nullable JSObject mediaSession;

	public MediaSessionBridge() {
		JSObject navigator = Js.optional(BrowserWindow.window(), "navigator");
		this.mediaSession = (navigator == null) ? null : Js.optional(navigator, "mediaSession");
	}

	public boolean supported() {
		return this.mediaSession != null;
	}

	public void metadata(String title, String artist, String album, String artworkPath) {
		JSObject session = this.mediaSession;
		if (session == null) {
			return;
		}
		JSObject window = BrowserWindow.window();
		JSObject constructor = Js.optional(window, "MediaMetadata");
		JSObject reflect = Js.optional(window, "Reflect");
		JSObject arrayType = Js.optional(window, "Array");
		if (constructor == null || reflect == null || arrayType == null) {
			return;
		}

		JSObject artworkEntry = JSObject.create();
		Js.set(artworkEntry, "src", artworkPath);
		Js.set(artworkEntry, "sizes", "512x512");
		Js.set(artworkEntry, "type", "image/svg+xml");

		JSObject init = JSObject.create();
		Js.set(init, "title", title);
		Js.set(init, "artist", artist);
		Js.set(init, "album", album);
		Js.set(init, "artwork", Js.callObject(arrayType, "of", artworkEntry));

		// Reflect.construct performs "new" without a JavaScript snippet, which is what
		// keeps the interop surface limited to the two accessors in BrowserWindow.
		Js.set(session, "metadata",
				Js.callObject(reflect, "construct", constructor, Js.callObject(arrayType, "of", init)));
	}

	public void playbackState(String state) {
		JSObject session = this.mediaSession;
		if (session != null) {
			Js.set(session, "playbackState", state);
		}
	}

	public void action(String action, Consumer<JSObject> handler) {
		JSObject session = this.mediaSession;
		if (session != null) {
			Js.call(session, "setActionHandler", action, handler);
		}
	}

}
