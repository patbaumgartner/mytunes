package com.patbaumgartner.mytunes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the module boundaries that Spring Modulith used to enforce from the
 * {@code package-info} declarations, now expressed directly as ArchUnit rules. The
 * boundary that matters most here is that only the platform module may touch the browser,
 * which is what keeps the domain testable on a JVM even though the application only ever
 * runs inside WebAssembly.
 */
class ModularityTests {

	private static final JavaClasses CLASSES = new ClassFileImporter().importPackages("com.patbaumgartner.mytunes");

	@Test
	void modulesRespectTheirDeclaredBoundaries() {
		// Given the same allowed dependencies the package-info files used to declare
		Architectures.LayeredArchitecture architecture = Architectures.layeredArchitecture()
			.consideringOnlyDependenciesInLayers()
			.layer("app")
			.definedBy("com.patbaumgartner.mytunes")
			.layer("stations")
			.definedBy("com.patbaumgartner.mytunes.stations..")
			.layer("player")
			.definedBy("com.patbaumgartner.mytunes.player..")
			.layer("persistence")
			.definedBy("com.patbaumgartner.mytunes.persistence..")
			.layer("themes")
			.definedBy("com.patbaumgartner.mytunes.themes..")
			.layer("platform")
			.definedBy("com.patbaumgartner.mytunes.platform..")
			.layer("ui")
			.definedBy("com.patbaumgartner.mytunes.ui..")
			.whereLayer("ui")
			.mayOnlyBeAccessedByLayers("app")
			.whereLayer("platform")
			.mayOnlyBeAccessedByLayers("ui", "app")
			.whereLayer("player")
			.mayOnlyBeAccessedByLayers("ui", "app")
			.whereLayer("themes")
			.mayOnlyBeAccessedByLayers("ui", "app")
			.whereLayer("stations")
			.mayOnlyBeAccessedByLayers("player", "ui", "app")
			.whereLayer("persistence")
			.mayOnlyBeAccessedByLayers("player", "platform", "ui", "app");

		// When
		var violations = architecture.evaluate(CLASSES).getFailureReport().getDetails();

		// Then
		assertThat(violations).isEmpty();
	}

	@Test
	void modulesAreFreeOfCycles() {
		// Given
		var rule = SlicesRuleDefinition.slices().matching("com.patbaumgartner.mytunes.(*)..").should().beFreeOfCycles();

		// When
		var violations = rule.evaluate(CLASSES).getFailureReport().getDetails();

		// Then
		assertThat(violations).isEmpty();
	}

	@Test
	void exposesTheModulesTheArchitectureDocumentsThem() {
		// When
		var names = CLASSES.stream()
			.map((javaClass) -> javaClass.getPackageName()
				.replace("com.patbaumgartner.mytunes", "")
				.replaceFirst("^\\.", "")
				.split("\\.")[0])
			.distinct()
			.toList();

		// Then
		assertThat(names).contains("stations", "player", "persistence", "themes", "ui", "platform");
	}

}
