package com.patbaumgartner.mytunes;

import com.patbaumgartner.jqh.test.JqhViolations;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the module boundaries declared in each {@code package-info}. The boundary that
 * matters most here is that only the platform module may touch the browser, which is what
 * keeps the domain testable on a JVM even though the application only ever runs inside
 * WebAssembly.
 */
class ModularityTests {

	@Test
	void modulesRespectTheirDeclaredBoundaries() {
		// Given
		ApplicationModules modules = ApplicationModules.of(MyTunesApplication.class);

		// When
		Violations violations = modules.detectViolations();

		// Then
		JqhViolations.record("modulith", violations.getMessages());
		assertThat(violations.getMessages()).isEmpty();
	}

	@Test
	void exposesTheModulesTheArchitectureDocumentsThem() {
		// Given
		ApplicationModules modules = ApplicationModules.of(MyTunesApplication.class);

		// When
		var names = modules.stream().map((module) -> module.getIdentifier().toString()).toList();

		// Then
		assertThat(names).contains("stations", "player", "persistence", "themes", "ui", "platform");
	}

}
