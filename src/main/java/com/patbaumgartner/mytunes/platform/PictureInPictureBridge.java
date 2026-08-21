package com.patbaumgartner.mytunes.platform;

import java.util.function.Consumer;

import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Opens a floating, always-on-top mini player through the Document Picture-in-Picture
 * API.
 * <p>
 * The API hands back a second {@code Window}, which arrives in Java as an ordinary
 * {@link JSObject}. Its document can therefore be driven with exactly the same {@link Js}
 * vocabulary as the main page, so the mini player needs no additional JavaScript.
 * <p>
 * Support is feature detected and the request must happen inside a user gesture. Where
 * the API is missing, or the browser refuses the request, the caller is told and the main
 * player is unaffected.
 * <p>
 * The window belongs to the browser that owns it. It stays visible while the tab is
 * hidden or the user works in other applications, which is the useful part, but it cannot
 * outlive the browser process: no web application can keep a window after the browser is
 * closed.
 */
@Service
public class PictureInPictureBridge {

	private final Dom dom;

	private @Nullable JSObject pipDocument;

	public PictureInPictureBridge(Dom dom) {
		this.dom = dom;
	}

	public boolean supported() {
		return Js.optional(BrowserWindow.window(), "documentPictureInPicture") != null;
	}

	public boolean open(int width, int height, Consumer<JSObject> build, Consumer<String> onFailure) {
		JSObject api = Js.optional(BrowserWindow.window(), "documentPictureInPicture");
		if (api == null) {
			onFailure.accept("This browser has no Document Picture-in-Picture support");
			return false;
		}
		JSObject options = JSObject.create();
		Js.set(options, "width", width);
		Js.set(options, "height", height);

		Object pending = Js.call(api, "requestWindow", options);
		if (!(pending instanceof JSObject promise)) {
			onFailure.accept("Picture-in-Picture request did not return a promise");
			return false;
		}
		Js.call(promise, "then", (Consumer<JSObject>) (pipWindow) -> adopt(pipWindow, build));
		Js.call(promise, "catch", (Consumer<JSObject>) (error) -> onFailure
			.accept("Picture-in-Picture refused: " + Js.getString(error, "message")));
		return true;
	}

	private void adopt(JSObject pipWindow, Consumer<JSObject> build) {
		JSObject document = Js.object(pipWindow, "document");
		this.pipDocument = document;
		copyStylesheets(document);
		build.accept(Js.object(document, "body"));
	}

	/**
	 * The Picture-in-Picture document starts empty, so the page's own stylesheet is
	 * linked into it by URL. Cloning the rules instead would require reading them back,
	 * which cross-origin stylesheets forbid.
	 */
	private void copyStylesheets(JSObject document) {
		JSObject link = Js.callObject(document, "createElement", "link");
		Js.set(link, "rel", "stylesheet");
		Js.set(link, "href", "styles.css");
		Js.call(Js.object(document, "head"), "appendChild", link);
	}

	public boolean isOpen() {
		return this.pipDocument != null;
	}

	public Dom dom() {
		return this.dom;
	}

}
