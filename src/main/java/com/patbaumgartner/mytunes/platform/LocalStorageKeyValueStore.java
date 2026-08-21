package com.patbaumgartner.mytunes.platform;

import java.util.Optional;

import com.patbaumgartner.mytunes.persistence.KeyValueStore;
import org.graalvm.webimage.api.JSError;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Backs the application's key/value store with the browser's {@code localStorage}.
 * <p>
 * {@code localStorage} throws when a browser blocks storage, for example in private mode
 * or when the quota is exhausted. Those failures are contained here and reported as an
 * absent value or a skipped write, because losing a volume preference must never stop the
 * radio from playing.
 */
@Service
public class LocalStorageKeyValueStore implements KeyValueStore {

	private final @Nullable JSObject storage;

	public LocalStorageKeyValueStore() {
		this.storage = Js.optional(BrowserWindow.window(), "localStorage");
	}

	@Override
	public Optional<String> get(String key) {
		JSObject store = this.storage;
		if (store == null) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(Js.asString(Js.call(store, "getItem", key)));
		}
		catch (JSError ex) {
			return Optional.empty();
		}
	}

	@Override
	public void put(String key, String value) {
		JSObject store = this.storage;
		if (store == null) {
			return;
		}
		try {
			Js.call(store, "setItem", key, value);
		}
		catch (JSError ex) {
			BrowserConsole.warn("[mytunes] localStorage write refused for " + key + ": " + ex.getMessage());
		}
	}

	@Override
	public void remove(String key) {
		JSObject store = this.storage;
		if (store == null) {
			return;
		}
		try {
			Js.call(store, "removeItem", key);
		}
		catch (JSError ex) {
			BrowserConsole.warn("[mytunes] localStorage remove refused for " + key + ": " + ex.getMessage());
		}
	}

}
