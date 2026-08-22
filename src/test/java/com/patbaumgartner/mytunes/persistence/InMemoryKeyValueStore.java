package com.patbaumgartner.mytunes.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** An in-memory {@link KeyValueStore} so persistence can be tested without a browser. */
public final class InMemoryKeyValueStore implements KeyValueStore {

	private final Map<String, String> values = new HashMap<>();

	@Override
	public Optional<String> get(String key) {
		return Optional.ofNullable(this.values.get(key));
	}

	@Override
	public void put(String key, String value) {
		this.values.put(key, value);
	}

	@Override
	public void remove(String key) {
		this.values.remove(key);
	}

	public Map<String, String> contents() {
		return Map.copyOf(this.values);
	}

}
