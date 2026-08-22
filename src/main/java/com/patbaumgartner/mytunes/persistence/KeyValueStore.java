package com.patbaumgartner.mytunes.persistence;

import java.util.Optional;

/**
 * A string keyed store. The domain depends on this interface rather than on
 * {@code localStorage}, which keeps player and preference logic testable on a plain JVM
 * where no browser exists.
 */
public interface KeyValueStore {

	Optional<String> get(String key);

	void put(String key, String value);

	void remove(String key);

}
