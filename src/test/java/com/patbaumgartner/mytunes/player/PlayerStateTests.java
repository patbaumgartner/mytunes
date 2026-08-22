package com.patbaumgartner.mytunes.player;

import com.patbaumgartner.mytunes.persistence.InMemoryKeyValueStore;
import com.patbaumgartner.mytunes.persistence.Preferences;
import com.patbaumgartner.mytunes.persistence.PreferencesStore;
import com.patbaumgartner.mytunes.stations.StationCatalogue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The player state machine holds no browser references, which is exactly what makes these
 * tests possible on a plain JVM even though the application itself only ever runs inside
 * WebAssembly.
 */
class PlayerStateTests {

	private StationCatalogue stations;

	private InMemoryKeyValueStore backing;

	private PlayerState player;

	@BeforeEach
	void setUp() {
		this.stations = new StationCatalogue();
		this.backing = new InMemoryKeyValueStore();
		this.player = new PlayerState(this.stations, new PreferencesStore(this.backing));
	}

	@Test
	void startsIdleOnTheFirstStation() {
		// Then
		assertThat(this.player.status()).isEqualTo(PlaybackStatus.IDLE);
		assertThat(this.player.station()).isEqualTo(this.stations.first());
	}

	@Test
	void togglingPlayRequestsPlaybackAndThenPausesIt() {
		// When
		this.player.togglePlay();

		// Then
		assertThat(this.player.status()).isEqualTo(PlaybackStatus.LOADING);
		assertThat(this.player.playRequested()).isTrue();

		// When
		this.player.confirmPlaying();
		this.player.togglePlay();

		// Then
		assertThat(this.player.status()).isEqualTo(PlaybackStatus.PAUSED);
		assertThat(this.player.playRequested()).isFalse();
	}

	@Test
	void keepsPlayingWhenTheStationChangesWhilePlaybackWasRequested() {
		// Given
		this.player.requestPlay();
		this.player.confirmPlaying();

		// When
		this.player.nextStation();

		// Then the intent to play survives the switch, so the new stream starts on its
		// own
		assertThat(this.player.playRequested()).isTrue();
		assertThat(this.player.status()).isEqualTo(PlaybackStatus.LOADING);
	}

	@Test
	void doesNotStartPlayingJustBecauseTheStationChanged() {
		// When
		this.player.nextStation();

		// Then
		assertThat(this.player.status()).isEqualTo(PlaybackStatus.IDLE);
	}

	@Test
	void wrapsAroundInBothDirections() {
		// Given
		String first = this.stations.first().id();

		// When
		this.player.previousStation();

		// Then
		assertThat(this.player.station().id()).isEqualTo(this.stations.all().getLast().id());

		// When
		this.player.nextStation();

		// Then
		assertThat(this.player.station().id()).isEqualTo(first);
	}

	@Test
	void unmutesWhenTheListenerRaisesTheVolume() {
		// Given
		this.player.toggleMute();

		// When
		this.player.volume(0.4);

		// Then
		assertThat(this.player.preferences().muted()).isFalse();
		assertThat(this.player.preferences().volume()).isEqualTo(0.4);
	}

	@Test
	void reportsAFailedStreamWithAMessageTheInterfaceCanShow() {
		// When
		this.player.fail("  ");

		// Then a blank reason must still produce something readable
		assertThat(this.player.status()).isEqualTo(PlaybackStatus.FAILED);
		assertThat(this.player.errorMessage()).isEqualTo("Stream unavailable");
	}

	@Test
	void persistsEveryPreferenceChangeSoItSurvivesAReload() {
		// When
		this.player.selectStation("dronezone");
		this.player.selectBackground("night-pines");
		this.player.volume(0.33);

		// Then
		PlayerState restored = new PlayerState(this.stations, new PreferencesStore(this.backing));
		restored.restore(new Preferences("mytunes-signal", "dawn-lake", 0.7, false));
		assertThat(restored.station().id()).isEqualTo("dronezone");
		assertThat(restored.preferences().backgroundId()).isEqualTo("night-pines");
		assertThat(restored.preferences().volume()).isEqualTo(0.33);
	}

	@Test
	void notifiesListenersSoTheInterfaceCanRerender() {
		// Given
		int[] renders = { 0 };
		this.player.onChange((state) -> renders[0]++);

		// When
		this.player.togglePlay();

		// Then
		assertThat(renders[0]).isPositive();
	}

	@Test
	void fallsBackToAKnownStationWhenAStoredIdNoLongerExists() {
		// When
		this.player.selectStation("a-station-that-was-removed");

		// Then
		assertThat(this.player.station()).isEqualTo(this.stations.first());
	}

}
