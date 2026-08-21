package com.patbaumgartner.mytunes.wasm;

import java.io.Console;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * {@code java.io.Console} holds its tty state in a static final field initialised from
 * the native method {@code ttyStatus()}, which Web Image cannot link, so merely
 * initialising the class fails with a {@code LinkageError}. Spring Boot reaches it
 * through {@code LoggingSystemProperties.getConsole()} while preparing logging
 * properties.
 * <p>
 * Returning {@code null} is the contractually correct answer rather than a suppression: a
 * browser has no controlling terminal, {@code System.console()} is specified to return
 * {@code null} in exactly that case, and Spring Boot's caller already null-checks the
 * result.
 */
@TargetClass(System.class)
final class TargetSystem {

	@Substitute
	public static Console console() {
		return null;
	}

	private TargetSystem() {
	}

}
