package com.patbaumgartner.mytunes.wasm;

import java.util.Set;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Web Image substitutes every {@code StackWalker} method with an unconditional
 * {@code UnsupportedOperationException}, and Spring Boot's {@code SpringApplication}
 * constructor calls {@code deduceMainApplicationClass()}, which walks the stack to find
 * the class declaring {@code main}. Without this substitution the constructor throws
 * before any application context can be created, so no Spring Boot application can start
 * in the browser.
 * <p>
 * The replacement neither guesses nor hard-codes a class. It reads the
 * {@code primarySources} the caller already passed to the constructor, which the bytecode
 * assigns before {@code deduceMainApplicationClass()} is invoked, so it yields exactly
 * the class the stack walk would have found.
 */
@TargetClass(className = "org.springframework.boot.SpringApplication")
final class TargetSpringApplication {

	@Alias
	private Set<Class<?>> primarySources;

	@Substitute
	private Class<?> deduceMainApplicationClass() {
		Set<Class<?>> sources = this.primarySources;
		if (sources == null || sources.isEmpty()) {
			return null;
		}
		return sources.iterator().next();
	}

	private TargetSpringApplication() {
	}

}
