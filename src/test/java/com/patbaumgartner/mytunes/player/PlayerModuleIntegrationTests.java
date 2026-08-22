package com.patbaumgartner.mytunes.player;

import com.patbaumgartner.mytunes.persistence.InMemoryKeyValueStore;
import com.patbaumgartner.mytunes.persistence.Preferences;
import com.patbaumgartner.mytunes.persistence.PreferencesStore;
import com.patbaumgartner.mytunes.stations.StationCatalogue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wires the player module with its real collaborators on a plain JVM, exactly as
 * {@code MyTunesApplication.main} does in the browser.
 * <p>
 * The full object graph cannot be built outside a browser, because the platform module's
 * classes hold live DOM, audio and {@code localStorage} references. Wiring one module and
 * its dependencies is the part that <em>is</em> supported off-browser, and it verifies
 * what the browser tests cannot isolate: that the module's collaborators are sufficient
 * and that the constructors compose.
 * <p>
 * The storage abstraction is satisfied by an in-memory implementation, which is exactly
 * why {@code persistence} declares an interface rather than depending on the browser.
 */
class PlayerModuleIntegrationTests {

	private final PlayerState player = new PlayerState(new StationCatalogue(),
			new PreferencesStore(new InMemoryKeyValueStore()));

	@Test
	void bootstrapsThePlayerModuleWithItsDeclaredDependencies() {
		// Then the graph wired and the module's collaborators respond
		assertThat(this.player).isNotNull();
		assertThat(this.player.station()).isNotNull();
		assertThat(this.player.status()).isEqualTo(PlaybackStatus.IDLE);
	}

	@Test
	void persistsThroughTheInjectedStoreRatherThanTheBrowser() {
		// When
		this.player.selectStation("dronezone");
		this.player.volume(0.33);

		// Then
		assertThat(this.player.preferences().stationId()).isEqualTo("dronezone");
		assertThat(this.player.preferences().volume()).isEqualTo(0.33);
	}

	@Test
	void restoresDefaultsWhenNothingHasBeenStored() {
		// Given
		Preferences defaults = new Preferences("mytunes-signal", "dawn-lake", 0.7, false);

		// When
		this.player.restore(defaults);

		// Then
		assertThat(this.player.preferences()).isEqualTo(defaults);
	}

}
