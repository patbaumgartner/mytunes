package com.patbaumgartner.mytunes.stations;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StationCatalogueTests {

	private final StationCatalogue catalogue = new StationCatalogue();

	@Test
	void offersEveryStationWithAStreamAndAnAttribution() {
		// Then
		assertThat(this.catalogue.all()).isNotEmpty().allSatisfy((station) -> {
			assertThat(station.id()).isNotBlank();
			assertThat(station.name()).isNotBlank();
			assertThat(station.genre()).isNotBlank();
			assertThat(station.streamUrl()).isNotBlank();
			assertThat(station.attribution()).isNotBlank();
		});
	}

	@Test
	void startsOnAStreamThisRepositoryServesItself() {
		// Then a reviewer can always demonstrate playback without a third party
		assertThat(this.catalogue.first().streamUrl()).doesNotStartWith("http");
	}

	@Test
	void usesUniqueIdentifiersBecauseTheyArePersisted() {
		// Then
		assertThat(this.catalogue.all().stream().map(Station::id)).doesNotHaveDuplicates();
	}

	@Test
	void neverReusesCredentialBearingStreamUrls() {
		// Then signed CDN parameters belong to someone else's session and must not appear
		// here
		assertThat(this.catalogue.all())
			.allSatisfy((station) -> assertThat(station.streamUrl()).doesNotContain("Policy=")
				.doesNotContain("Signature=")
				.doesNotContain("Key-Pair-Id=")
				.doesNotContain("client_id="));
	}

	@Test
	void resolvesAKnownIdentifier() {
		// Then
		assertThat(this.catalogue.byId("dronezone")).isPresent();
		assertThat(this.catalogue.byIdOrFirst("dronezone").name()).isEqualTo("Drone Zone");
	}

	@Test
	void fallsBackToTheFirstStationForAnUnknownIdentifier() {
		// Then a station removed between releases must not break a returning listener
		assertThat(this.catalogue.byId("gone")).isEmpty();
		assertThat(this.catalogue.byIdOrFirst("gone")).isEqualTo(this.catalogue.first());
	}

	@Test
	void wrapsAroundWhenSteppingPastEitherEnd() {
		// Given
		String first = this.catalogue.first().id();
		String last = this.catalogue.all().getLast().id();

		// Then
		assertThat(this.catalogue.previous(first).id()).isEqualTo(last);
		assertThat(this.catalogue.next(last).id()).isEqualTo(first);
	}

}
