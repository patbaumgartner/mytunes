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
			assertThat(station.category()).isNotBlank();
			assertThat(station.streamUrl()).isNotBlank();
			assertThat(station.attribution()).isNotBlank();
		});
	}

	@Test
	void groupsChannelsIntoTheDocumentedCategories() {
		// Then the menu mirrors DevTunes FM's genre families
		assertThat(this.catalogue.categories()).containsExactly("Ambient", "Chill", "Beats", "Bass", "Electronic",
				"Trance", "Wave", "Hacker");
		assertThat(this.catalogue.categories())
			.allSatisfy((category) -> assertThat(this.catalogue.byCategory(category)).isNotEmpty());
	}

	@Test
	void everyCategoryLeadsWithAStreamThisRepositoryServesItself() {
		// Then playback is always demonstrable in every category, offline, because the
		// third-party mounts may refuse browser requests at any time
		assertThat(this.catalogue.categories())
			.allSatisfy((category) -> assertThat(this.catalogue.byCategory(category).getFirst().streamUrl())
				.doesNotStartWith("http"));
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
	void everyGeneratedChannelPointsAtACommittedAudioFile() {
		// Then a catalogue entry cannot outrun the generator's output
		assertThat(this.catalogue.all()).filteredOn(Station::selfHosted)
			.isNotEmpty()
			.allSatisfy((station) -> assertThat(java.nio.file.Path.of("src/main/web", station.streamUrl())).exists());
	}

	@Test
	void distinguishesSelfHostedChannelsFromLiveStreams() {
		// Then self-hosted loops repeat while live streams must not
		assertThat(this.catalogue.byIdOrFirst("mytunes-signal").selfHosted()).isTrue();
		assertThat(this.catalogue.byIdOrFirst("dronezone").selfHosted()).isFalse();
	}

	@Test
	void offersChannelsFromProvidersVerifiedToAnswerBrowsersWithCors() {
		// Then the catalogue is not hostage to a single third party refusing browsers:
		// Nightride FM and Radio Paradise answered an Origin-bearing GET with 200 and a
		// permissive Access-Control-Allow-Origin (see tools/generate-stations.py)
		assertThat(this.catalogue.all()).extracting(Station::attribution).contains("Nightride FM", "Radio Paradise");
		assertThat(this.catalogue.all()).filteredOn((station) -> station.attribution().equals("Nightride FM"))
			.allSatisfy((station) -> assertThat(station.streamUrl()).startsWith("https://stream.nightride.fm/"));
		assertThat(this.catalogue.all()).filteredOn((station) -> station.attribution().equals("Radio Paradise"))
			.allSatisfy((station) -> assertThat(station.streamUrl()).startsWith("https://stream.radioparadise.com/"));
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
