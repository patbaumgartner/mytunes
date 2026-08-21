package com.patbaumgartner.mytunes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enforces the project's hardest constraint: no hand-authored JavaScript application
 * logic.
 * <p>
 * Only two JavaScript expressions are permitted in the whole repository, and both live in
 * {@code BrowserWindow}, because GraalVM Web Image offers no way to obtain a browser
 * global from Java. These tests fail if that surface grows, or if a {@code .js},
 * {@code .mjs}, {@code .cjs} or TypeScript source file ever appears.
 */
class NoHandwrittenJavaScriptTests {

	private static final Path ROOT = Path.of("").toAbsolutePath();

	private static final List<String> FORBIDDEN_SUFFIXES = List.of(".js", ".mjs", ".cjs", ".ts", ".tsx", ".jsx");

	private static final List<String> GENERATED_OR_EXTERNAL = List.of("/target/", "/node_modules/", "/.git/", "/.mvn/");

	@Test
	void theRepositoryContainsNoHandWrittenJavaScriptOrTypeScriptSource() throws IOException {
		// Given, When
		List<Path> offenders = sourceFiles()
			.filter((path) -> FORBIDDEN_SUFFIXES.stream().anyMatch(path.toString()::endsWith))
			.toList();

		// Then
		assertThat(offenders)
			.as("Application behaviour must be Java compiled to WebAssembly, never hand-written JavaScript")
			.isEmpty();
	}

	@Test
	void onlyBrowserWindowDeclaresJavaScriptSnippets() throws IOException {
		// Given, When
		List<Path> offenders = sourceFiles().filter((path) -> path.toString().endsWith(".java"))
			.filter(NoHandwrittenJavaScriptTests::declaresJsSnippet)
			.filter((path) -> !path.endsWith("BrowserWindow.java"))
			.toList();

		// Then
		assertThat(offenders)
			.as("Browser globals are reached only through BrowserWindow; everything else uses JSObject")
			.isEmpty();
	}

	@Test
	void theBrowserInteropSurfaceStaysAtTwoExpressions() throws IOException {
		// Given
		Path browserWindow = ROOT.resolve("src/main/java/com/patbaumgartner/mytunes/platform/BrowserWindow.java");

		// When
		long snippets = Files.readAllLines(browserWindow, StandardCharsets.UTF_8)
			.stream()
			.filter((line) -> line.trim().startsWith("@JS("))
			.count();

		// Then
		assertThat(snippets).as("Adding a third JavaScript expression is an architectural change, not a detail")
			.isEqualTo(2);
	}

	@Test
	void theBootstrapPageCarriesNoInlineScript() throws IOException {
		// Given
		Path index = ROOT.resolve("src/main/web/index.html");

		// When
		String html = Files.readString(index, StandardCharsets.UTF_8);

		// Then only the generated loader may be referenced, and never inline
		assertThat(html).contains("<script src=\"mytunes.js\"></script>");
		assertThat(html.replace("<script src=\"mytunes.js\"></script>", "")).doesNotContain("<script");
		assertThat(html).doesNotContain("onclick").doesNotContain("onload");
	}

	private static boolean declaresJsSnippet(Path path) {
		try {
			return Files.readAllLines(path, StandardCharsets.UTF_8)
				.stream()
				.anyMatch((line) -> line.trim().startsWith("@JS("));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not read " + path, ex);
		}
	}

	private static Stream<Path> sourceFiles() throws IOException {
		return Files.walk(ROOT).filter(Files::isRegularFile).filter((path) -> {
			String normalised = path.toString().replace('\\', '/');
			return GENERATED_OR_EXTERNAL.stream().noneMatch(normalised::contains);
		});
	}

}
