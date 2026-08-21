package com.patbaumgartner.mytunes.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PreferencesStoreTests {

	private static final Preferences DEFAULTS = new Preferences("mytunes-signal", "dawn-lake", 0.7, false);

	@Test
	void returnsDefaultsWhenNothingHasBeenStored() {
		// Given
		PreferencesStore store = new PreferencesStore(new InMemoryKeyValueStore());

		// When
		Preferences restored = store.load(DEFAULTS);

		// Then
		assertThat(restored).isEqualTo(DEFAULTS);
	}

	@Test
	void restoresEverythingItSaved() {
		// Given
		PreferencesStore store = new PreferencesStore(new InMemoryKeyValueStore());
		Preferences saved = new Preferences("dronezone", "night-pines", 0.25, true);

		// When
		store.save(saved);

		// Then
		assertThat(store.load(DEFAULTS)).isEqualTo(saved);
	}

	@Test
	void writesTheSchemaVersionSoFutureMigrationsArePossible() {
		// Given
		InMemoryKeyValueStore backing = new InMemoryKeyValueStore();
		PreferencesStore store = new PreferencesStore(backing);

		// When
		store.save(DEFAULTS);

		// Then
		assertThat(backing.contents()).containsEntry("mytunes.schema",
				Integer.toString(PreferencesStore.SCHEMA_VERSION));
	}

	@Test
	void fallsBackToDefaultsWhenTheStoredSchemaIsNewerThanThisBuildUnderstands() {
		// Given a record written by a future, unknown version
		InMemoryKeyValueStore backing = new InMemoryKeyValueStore();
		backing.put("mytunes.schema", Integer.toString(PreferencesStore.SCHEMA_VERSION + 1));
		backing.put("mytunes.station", "some-station-that-no-longer-exists");

		// When
		Preferences restored = new PreferencesStore(backing).load(DEFAULTS);

		// Then a half-read record must never become player state
		assertThat(restored).isEqualTo(DEFAULTS);
	}

	@Test
	void ignoresUnparseableValuesRatherThanFailingToStart() {
		// Given
		InMemoryKeyValueStore backing = new InMemoryKeyValueStore();
		backing.put("mytunes.schema", "1");
		backing.put("mytunes.volume", "not-a-number");

		// When
		Preferences restored = new PreferencesStore(backing).load(DEFAULTS);

		// Then
		assertThat(restored.volume()).isEqualTo(DEFAULTS.volume());
	}

	@Test
	void clampsVolumeIntoTheRangeTheAudioElementAccepts() {
		// Given, When, Then
		assertThat(new Preferences("s", "b", 4.2, false).volume()).isEqualTo(1.0);
		assertThat(new Preferences("s", "b", -3.0, false).volume()).isEqualTo(0.0);
	}

}
