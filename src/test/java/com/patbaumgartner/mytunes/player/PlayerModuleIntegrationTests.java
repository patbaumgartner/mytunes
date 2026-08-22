package com.patbaumgartner.mytunes.player;

import com.patbaumgartner.mytunes.persistence.InMemoryKeyValueStore;
import com.patbaumgartner.mytunes.persistence.KeyValueStore;
import com.patbaumgartner.mytunes.persistence.Preferences;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Refreshes a real Spring application context for the player module on a plain JVM.
 * <p>
 * The full application context cannot be started outside a browser, because the platform
 * module's beans hold live DOM, audio and {@code localStorage} references. Bootstrapping
 * one module and its declared dependencies is the part that <em>is</em> supported
 * off-browser, and it verifies what the browser tests cannot isolate: that the declared
 * module dependencies are sufficient, that component scanning finds these beans, and that
 * constructor injection wires them.
 * <p>
 * The storage abstraction is satisfied by an in-memory implementation, which is exactly
 * why {@code persistence} declares an interface rather than depending on the browser.
 */
@ApplicationModuleTest(BootstrapMode.DIRECT_DEPENDENCIES)
@Import(PlayerModuleIntegrationTests.InMemoryStorage.class)
class PlayerModuleIntegrationTests {

	@TestConfiguration
	static class InMemoryStorage {

		@Bean
		KeyValueStore keyValueStore() {
			return new InMemoryKeyValueStore();
		}

	}

	private final PlayerState player;

	PlayerModuleIntegrationTests(PlayerState player) {
		this.player = player;
	}

	@Test
	void bootstrapsThePlayerModuleWithItsDeclaredDependencies() {
		// Then the context refreshed and the module's beans are wired
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
