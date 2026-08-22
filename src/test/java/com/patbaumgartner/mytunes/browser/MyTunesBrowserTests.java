package com.patbaumgartner.mytunes.browser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ViewportSize;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The authoritative tests for myTunes.
 * <p>
 * The application only ever executes as WebAssembly inside a browser, so JVM unit tests
 * can prove the player's logic but never that the product works. These tests drive the
 * built image in a real browser and write screenshots and console logs under
 * {@code target/diagnostics}.
 * <p>
 * They are excluded from the default Surefire run because they need a built Wasm image,
 * and they skip themselves rather than fail when that image is absent.
 */
@EnabledIf("siteIsBuilt")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyTunesBrowserTests {

	private static final Path SITE = Path.of("target/site");

	private static final Path EVIDENCE = Path.of("target/diagnostics/screenshots");

	private static final Path CONSOLE = Path.of("target/diagnostics/console");

	private static final Map<String, ViewportSize> BREAKPOINTS = Map.of("desktop-wide", new ViewportSize(1920, 1080),
			"desktop-standard", new ViewportSize(1440, 900), "mobile-portrait", new ViewportSize(390, 844),
			"mobile-small", new ViewportSize(360, 640));

	private StaticSite site;

	private Playwright playwright;

	private Browser browser;

	static boolean siteIsBuilt() {
		String configured = System.getProperty("mytunes.baseUrl");
		if (configured != null && !configured.isBlank()) {
			return true;
		}
		return Files.isRegularFile(SITE.resolve("mytunes.js.wasm"));
	}

	@BeforeAll
	void startBrowser() throws IOException {
		site = StaticSite.serve(SITE.toAbsolutePath());
		playwright = Playwright.create();
		browser = playwright.chromium()
			.launch(new BrowserType.LaunchOptions()
				// Autoplay is allowed only so that a headless run can observe real
				// playback; the application still starts audio from a real click,
				// exactly as a listener would.
				.setArgs(List.of("--autoplay-policy=no-user-gesture-required")));
		Files.createDirectories(EVIDENCE);
		Files.createDirectories(CONSOLE);
	}

	@AfterAll
	void stopBrowser() throws IOException {
		if (browser != null) {
			browser.close();
		}
		if (playwright != null) {
			playwright.close();
		}
		if (site != null) {
			site.close();
		}
	}

	@Test
	void springBootStartsInsideTheBrowserAndBuildsTheInterface() throws IOException {
		// Given
		List<String> messages = new ArrayList<>();

		// When
		try (BrowserContext context = newContext(1440, 900)) {
			Page page = open(context, messages);

			// Then
			assertThat(messages).anyMatch((line) -> line.contains("Spring Boot 4.1.1 started in the browser"));
			assertThat(messages).anyMatch((line) -> line.contains("interface ready"));
			assertThat(page.textContent(".station-select__name")).isNotBlank();
			Files.writeString(CONSOLE.resolve("mytunes-startup.log"), String.join("\n", messages));
		}
	}

	@Test
	void loadsNoScriptOtherThanTheGeneratedLoader() throws IOException {
		// Given, When
		try (BrowserContext context = newContext(1440, 900)) {
			Page page = open(context, new ArrayList<>());
			@SuppressWarnings("unchecked")
			List<String> scripts = (List<String>) page.evaluate("() => Array.from(document.scripts).map(s => s.src)");

			// Then
			assertThat(scripts).hasSize(1);
			assertThat(scripts.getFirst()).endsWith("/mytunes.js");
		}
	}

	@Test
	void playsRealAudioAfterAClickAndRespondsToVolumeAndPause() throws IOException {
		// Given
		try (BrowserContext context = newContext(1440, 900)) {
			Page page = open(context, new ArrayList<>());

			// When a listener presses play
			page.click("#play-toggle");
			page.waitForTimeout(4000);

			// Then audio is genuinely advancing, not merely requested
			assertThat((Boolean) page.evaluate("() => document.querySelector('audio').paused")).isFalse();
			assertThat((Double) page.evaluate("() => document.querySelector('audio').currentTime")).isGreaterThan(0.5);

			// Then the self-hosted default channel repeats: a finite generated file
			// stands
			// in for a continuous station
			assertThat((Boolean) page.evaluate("() => document.querySelector('audio').loop")).isTrue();

			// When the volume changes
			page.evaluate("() => { const s = document.getElementById('volume-slider');"
					+ " s.value = 25; s.dispatchEvent(new Event('input', { bubbles: true })); }");
			page.waitForTimeout(400);

			// Then
			assertThat((Double) page.evaluate("() => document.querySelector('audio').volume")).isEqualTo(0.25);

			// When paused
			page.click("#play-toggle");
			page.waitForTimeout(600);

			// Then
			assertThat((Boolean) page.evaluate("() => document.querySelector('audio').paused")).isTrue();
		}
	}

	@Test
	void remembersStationBackgroundAndVolumeAcrossAReload() throws IOException {
		// Given
		try (BrowserContext context = newContext(1440, 900)) {
			Page page = open(context, new ArrayList<>());

			// When
			page.click("#station-select");
			page.click(".station-menu__item[data-station='dronezone']");
			page.click("#background-toggle");
			page.waitForTimeout(600);
			String background = page.getAttribute("#mytunes", "data-background");

			page.reload();
			page.waitForSelector("#mytunes[data-ready='true']");
			page.waitForTimeout(600);

			// Then
			assertThat(page.textContent(".station-select__name")).isEqualTo("Drone Zone");
			assertThat(page.getAttribute("#mytunes", "data-background")).isEqualTo(background);
			assertThat((String) page.evaluate("() => localStorage.getItem('mytunes.schema')")).isEqualTo("1");
		}
	}

	@Test
	void switchingBackgroundChangesTheArtwork() throws IOException {
		// Given
		try (BrowserContext context = newContext(1440, 900)) {
			Page page = open(context, new ArrayList<>());
			String before = page.getAttribute("#mytunes", "data-background");

			// When
			page.click("#background-toggle");
			page.waitForTimeout(600);

			// Then
			assertThat(page.getAttribute("#mytunes", "data-background")).isNotEqualTo(before);
		}
	}

	@Test
	void keyboardMediaControlsDriveThePlayer() throws IOException {
		// Given
		try (BrowserContext context = newContext(1440, 900)) {
			Page page = open(context, new ArrayList<>());
			String before = page.textContent(".station-select__name");

			// When
			page.keyboard().press("ArrowRight");
			page.waitForTimeout(400);

			// Then
			assertThat(page.textContent(".station-select__name")).isNotEqualTo(before);
		}
	}

	@Test
	void exposesMediaSessionMetadataWhereTheBrowserSupportsIt() throws IOException {
		// Given
		try (BrowserContext context = newContext(1440, 900)) {
			Page page = open(context, new ArrayList<>());

			// When
			Object supported = page.evaluate("() => 'mediaSession' in navigator");

			// Then
			if (Boolean.TRUE.equals(supported)) {
				assertThat((String) page.evaluate("() => navigator.mediaSession.metadata?.title ?? ''")).isNotBlank();
			}
		}
	}

	@Test
	void recordsModuleSizeAndStartupTimingAsDiagnostics() throws IOException {
		// Given
		List<String> messages = new ArrayList<>();

		// When
		try (BrowserContext context = newContext(1440, 900)) {
			Page page = open(context, messages);

			String reported = messages.stream()
				.filter((line) -> line.contains("started in the browser in"))
				.findFirst()
				.orElse("(not reported)");

			StringBuilder diagnostics = new StringBuilder();
			diagnostics.append("springStartup=").append(reported).append(System.lineSeparator());
			recordAssetSize(diagnostics, "mytunes.js.wasm");
			recordAssetSize(diagnostics, "mytunes.js");
			Files.writeString(CONSOLE.resolve("wasm-diagnostics.log"), diagnostics.toString());

			// Then the module actually loaded and Spring reported its own startup
			assertThat(page.getAttribute("#mytunes", "data-ready")).isEqualTo("true");
			assertThat(reported).contains("Spring Boot");
		}
	}

	/**
	 * Sizes are read from the built site when it is present. Against a remote origin
	 * there is no local file, and the diagnostic records that rather than inventing a
	 * number.
	 */
	private static void recordAssetSize(StringBuilder diagnostics, String name) throws IOException {
		Path asset = SITE.resolve(name);
		diagnostics.append(name).append('=');
		diagnostics.append(Files.isRegularFile(asset) ? Files.size(asset) + " bytes" : "(not built locally)");
		diagnostics.append(System.lineSeparator());
	}

	@Test
	void controlsRemainReachableAtEveryBreakpoint() throws IOException {
		// Given
		List<String> report = new ArrayList<>();

		// When
		for (Map.Entry<String, ViewportSize> breakpoint : BREAKPOINTS.entrySet()) {
			ViewportSize size = breakpoint.getValue();
			List<String> messages = new ArrayList<>();
			try (BrowserContext context = newContext(size.width, size.height)) {
				Page page = open(context, messages);
				page.screenshot(new Page.ScreenshotOptions().setPath(EVIDENCE.resolve(breakpoint.getKey() + ".png")));

				// Then the controls a listener needs are reachable at every size
				assertThat(page.isVisible("#play-toggle")).isTrue();
				assertThat(page.isVisible("#station-select")).isTrue();
				assertThat(page.isVisible("#volume-slider")).isTrue();

				List<String> errors = messages.stream()
					.filter((line) -> line.startsWith("[error]"))
					.filter((line) -> !line.contains("SLF4J"))
					.toList();
				report.add(breakpoint.getKey() + " " + size.width + "x" + size.height + " consoleErrors="
						+ errors.size() + (errors.isEmpty() ? "" : " " + errors));
			}
		}
		Files.writeString(CONSOLE.resolve("mytunes-breakpoints.log"), String.join("\n", report));
		assertThat(report).hasSize(BREAKPOINTS.size());
	}

	private BrowserContext newContext(int width, int height) {
		return browser.newContext(new Browser.NewContextOptions().setViewportSize(width, height));
	}

	private Page open(BrowserContext context, List<String> messages) {
		Page page = context.newPage();
		page.onConsoleMessage((message) -> messages.add("[" + message.type() + "] " + message.text()));
		page.onPageError((error) -> messages.add("[pageerror] " + error));
		page.navigate(site.url("/index.html"));
		page.waitForSelector("#mytunes[data-ready='true']", new Page.WaitForSelectorOptions().setTimeout(120_000));
		return page;
	}

}
