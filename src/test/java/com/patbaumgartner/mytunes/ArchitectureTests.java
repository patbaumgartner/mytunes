package com.patbaumgartner.mytunes;

import com.enofex.taikai.Taikai;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture convention verification.
 * <p>
 * Uses {@code checkAll()} so one run reports every violation instead of stopping at the
 * first.
 */
class ArchitectureTests {

	@Test
	void architectureRulesHold() {
		// Given
		Taikai taikai = Taikai.builder()
			.namespace("com.patbaumgartner.mytunes")
			.failOnEmpty(true)
			// Module boundaries and cycle freedom are verified in ModularityTests with
			// dedicated ArchUnit rules that encode the allowed dependencies per module.
			.java((java) -> java.noUsageOfDeprecatedAPIs().utilityClassesShouldBeFinalAndHavePrivateConstructor())
			.test((test) -> test.junit((junit) -> junit.methodsShouldNotBeAnnotatedWithDisabled()
				.methodsShouldContainAssertionsOrVerifications()))
			.build();

		// When
		String failure = capture(taikai);

		// Then
		assertThat(failure).as("Taikai architecture violations").isEmpty();
	}

	private static String capture(final Taikai taikai) {
		try {
			taikai.checkAll();
			return "";
		}
		catch (AssertionError ex) {
			return (ex.getMessage() == null) ? "Taikai reported a violation with no message" : ex.getMessage();
		}
	}

}
