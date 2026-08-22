package com.patbaumgartner.mytunes.persistence;

import java.util.Optional;

/**
 * Reads and writes {@link Preferences} through a {@link KeyValueStore}.
 * <p>
 * Every key is namespaced and the stored format carries {@link #SCHEMA_VERSION}. A record
 * written by a future, unknown schema is discarded rather than half-read, so a
 * forward-incompatible change degrades to defaults instead of restoring a corrupt player
 * state. Nothing secret is ever stored: these are display and playback preferences only.
 */
public class PreferencesStore {

	public static final int SCHEMA_VERSION = 1;

	private static final String PREFIX = "mytunes.";

	private static final String KEY_SCHEMA = PREFIX + "schema";

	private static final String KEY_STATION = PREFIX + "station";

	private static final String KEY_BACKGROUND = PREFIX + "background";

	private static final String KEY_VOLUME = PREFIX + "volume";

	private static final String KEY_MUTED = PREFIX + "muted";

	private final KeyValueStore store;

	public PreferencesStore(KeyValueStore store) {
		this.store = store;
	}

	public Preferences load(Preferences defaults) {
		Optional<Integer> schema = this.store.get(KEY_SCHEMA).flatMap(PreferencesStore::parseInt);
		if (schema.isEmpty() || schema.get() > SCHEMA_VERSION) {
			return defaults;
		}
		return new Preferences(this.store.get(KEY_STATION).orElse(defaults.stationId()),
				this.store.get(KEY_BACKGROUND).orElse(defaults.backgroundId()),
				this.store.get(KEY_VOLUME).flatMap(PreferencesStore::parseDouble).orElse(defaults.volume()),
				this.store.get(KEY_MUTED).map(Boolean::parseBoolean).orElse(defaults.muted()));
	}

	public void save(Preferences preferences) {
		this.store.put(KEY_SCHEMA, Integer.toString(SCHEMA_VERSION));
		this.store.put(KEY_STATION, preferences.stationId());
		this.store.put(KEY_BACKGROUND, preferences.backgroundId());
		this.store.put(KEY_VOLUME, Double.toString(preferences.volume()));
		this.store.put(KEY_MUTED, Boolean.toString(preferences.muted()));
	}

	private static Optional<Integer> parseInt(String value) {
		try {
			return Optional.of(Integer.valueOf(value));
		}
		catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

	/**
	 * {@code Double.valueOf} accepts "NaN" and "Infinity", so a
	 * {@link NumberFormatException} guard alone is not enough: a non-finite volume
	 * survives clamping and throws when it reaches {@code HTMLMediaElement.volume},
	 * taking the interface down. Only a finite number is a value.
	 */
	private static Optional<Double> parseDouble(String value) {
		try {
			double parsed = Double.parseDouble(value);
			return Double.isFinite(parsed) ? Optional.of(parsed) : Optional.empty();
		}
		catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

}
