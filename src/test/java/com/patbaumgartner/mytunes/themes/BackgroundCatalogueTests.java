package com.patbaumgartner.mytunes.themes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackgroundCatalogueTests {

	private final BackgroundCatalogue catalogue = new BackgroundCatalogue();

	@Test
	void offersSeveralBackgroundsSoRotationIsMeaningful() {
		// Then
		assertThat(this.catalogue.all()).hasSizeGreaterThan(1);
	}

	@Test
	void describesEveryBackgroundCompletely() {
		// Then
		assertThat(this.catalogue.all()).allSatisfy((background) -> {
			assertThat(background.id()).isNotBlank();
			assertThat(background.name()).isNotBlank();
			assertThat(background.accent()).startsWith("#");
			assertThat(background.assetPath()).startsWith("backgrounds/").endsWith(".svg");
		});
	}

	@Test
	void servesArtworkFromThisRepositoryRatherThanAThirdParty() {
		// Then redistribution rights stay with this project
		assertThat(this.catalogue.all())
			.allSatisfy((background) -> assertThat(background.assetPath()).doesNotStartWith("http"));
	}

	@Test
	void usesUniqueIdentifiersBecauseTheyArePersisted() {
		// Then
		assertThat(this.catalogue.all().stream().map(Background::id)).doesNotHaveDuplicates();
	}

	@Test
	void fallsBackToTheFirstBackgroundForAnUnknownIdentifier() {
		// Then
		assertThat(this.catalogue.byId("gone")).isEmpty();
		assertThat(this.catalogue.byIdOrFirst("gone")).isEqualTo(this.catalogue.first());
	}

	@Test
	void cyclesBackToTheStartAfterTheLastBackground() {
		// Given
		String last = this.catalogue.all().getLast().id();

		// Then
		assertThat(this.catalogue.next(last)).isEqualTo(this.catalogue.first());
	}

}
