package com.patbaumgartner.mytunes;

import com.enofex.taikai.Taikai;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture and Spring convention verification.
 * <p>
 * Uses {@code checkAll()} so one run reports every violation instead of stopping at the
 * first.
 */
class ArchitectureTests {

	/**
	 * Spring Boot's AOT step writes {@code __BeanDefinitions} and similar classes next to
	 * the compiled sources. They are generated build output rather than authored code, so
	 * holding them to this project's conventions would report defects nobody can fix.
	 */
	private static final ImportOption AUTHORED_CODE_ONLY = (location) -> !location.contains("__BeanDefinitions")
			&& !location.contains("__Autowiring") && !location.contains("__TestContext")
			&& !location.contains("/spring-aot/");

	@Test
	void architectureRulesHold() {
		// Given
		Taikai taikai = Taikai.builder()
			.classes(new ClassFileImporter().withImportOption(AUTHORED_CODE_ONLY)
				.importPackages("com.patbaumgartner.mytunes"))
			.failOnEmpty(true)
			// Cyclic dependencies are verified in ModularityTests with Spring Modulith,
			// which
			// understands the declared module boundaries and is the stronger check.
			// Taikai's
			// variant requires a namespace, and a namespace cannot be combined with the
			// filtered
			// class import that keeps generated AOT output out of these rules.
			.java((java) -> java.noUsageOfDeprecatedAPIs().utilityClassesShouldBeFinalAndHavePrivateConstructor())
			.spring((spring) -> spring.noAutowiredFields()
				.boot((boot) -> boot.applicationClassShouldResideInPackage("com.patbaumgartner.mytunes")))
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
