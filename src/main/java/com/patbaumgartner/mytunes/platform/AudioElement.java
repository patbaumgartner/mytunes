package com.patbaumgartner.mytunes.platform;

import java.util.function.Consumer;

import org.graalvm.webimage.api.JSObject;
import org.springframework.stereotype.Component;

/**
 * An HTML audio element created and driven entirely from Java.
 * <p>
 * {@code play()} returns a promise that rejects when the browser's autoplay policy blocks
 * playback before a user gesture. That rejection is surfaced to the caller rather than
 * swallowed, because "the browser refused to start audio" is a state the interface must
 * be able to show.
 */
@Component
public final class AudioElement {

	private final JSObject element;

	public AudioElement(Dom dom) {
		this.element = dom.create("audio");
		Js.set(this.element, "preload", "none");
		// No crossOrigin attribute: plain playback needs no CORS mode, and requesting it
		// makes
		// some Icecast hosts reject the request outright.
		dom.append(dom.body(), this.element);
	}

	public void source(String url) {
		Js.set(this.element, "src", url);
	}

	public void play(Consumer<String> onFailure) {
		Object promise = Js.call(this.element, "play");
		if (promise instanceof JSObject pending) {
			Js.call(pending, "catch", (Consumer<JSObject>) error -> onFailure
				.accept(error == null ? "playback blocked" : String.valueOf(Js.get(error, "message"))));
		}
	}

	public void pause() {
		Js.call(this.element, "pause");
	}

	public void load() {
		Js.call(this.element, "load");
	}

	public boolean paused() {
		return Js.getBoolean(this.element, "paused", true);
	}

	public void volume(double value) {
		Js.set(this.element, "volume", Math.clamp(value, 0.0, 1.0));
	}

	public void muted(boolean value) {
		Js.set(this.element, "muted", value);
	}

	public void on(String event, Consumer<JSObject> listener) {
		Js.call(this.element, "addEventListener", event, listener);
	}

}
