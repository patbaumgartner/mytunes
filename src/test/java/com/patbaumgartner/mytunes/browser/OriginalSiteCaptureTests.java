package com.patbaumgartner.mytunes.browser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Captures the DevTunes FM reference screenshots that myTunes is compared against, at the
 * same four breakpoints the myTunes evidence uses.
 * <p>
 * This reaches the public internet, so it is opt-in: run it with
 * {@code -Dmytunes.captureOriginal=true}. Without that flag it skips, which keeps the
 * ordinary build offline and deterministic. The captured images are a point-in-time
 * record; the live site changes independently of this repository, so re-running this
 * replaces the reference rather than reproducing it byte for byte.
 */
@EnabledIfSystemProperty(named = "mytunes.captureOriginal", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OriginalSiteCaptureTests {

	private static final String ORIGINAL = "https://radio.madza.dev/";

	private static final Path EVIDENCE = Path.of("docs/parity/original");

	private static final Path CONSOLE = Path.of("docs/parity/console");

	private record Breakpoint(String name, int width, int height) {
	}

	private static final List<Breakpoint> BREAKPOINTS = List.of(new Breakpoint("desktop-wide", 1920, 1080),
			new Breakpoint("desktop-standard", 1440, 900), new Breakpoint("mobile-portrait", 390, 844),
			new Breakpoint("mobile-small", 360, 640));

	private Playwright playwright;

	private Browser browser;

	@BeforeAll
	void startBrowser() throws IOException {
		this.playwright = Playwright.create();
		this.browser = this.playwright.chromium().launch();
		Files.createDirectories(EVIDENCE);
		Files.createDirectories(CONSOLE);
	}

	@AfterAll
	void stopBrowser() {
		if (this.browser != null) {
			this.browser.close();
		}
		if (this.playwright != null) {
			this.playwright.close();
		}
	}

	@Test
	void capturesTheReferenceAtEveryBreakpoint() throws IOException {
		// Given
		List<String> requests = new ArrayList<>();
		List<String> messages = new ArrayList<>();

		// When
		for (Breakpoint breakpoint : BREAKPOINTS) {
			try (BrowserContext context = this.browser
				.newContext(new Browser.NewContextOptions().setViewportSize(breakpoint.width(), breakpoint.height()))) {
				Page page = context.newPage();
				page.onConsoleMessage((message) -> messages
					.add("[" + breakpoint.name() + "][" + message.type() + "] " + message.text()));
				page.onRequest((request) -> requests.add(breakpoint.name() + " " + request.method() + " "
						+ request.resourceType() + " " + request.url()));
				page.navigate(ORIGINAL, new Page.NavigateOptions().setTimeout(90_000));
				page.waitForLoadState(LoadState.NETWORKIDLE);
				page.waitForTimeout(4000);
				page.screenshot(new Page.ScreenshotOptions().setPath(EVIDENCE.resolve(breakpoint.name() + ".png")));
			}
		}

		// Then
		Files.writeString(CONSOLE.resolve("original-network.log"), String.join(System.lineSeparator(), requests));
		Files.writeString(CONSOLE.resolve("original-console.log"), String.join(System.lineSeparator(), messages));
		for (Breakpoint breakpoint : BREAKPOINTS) {
			assertThat(EVIDENCE.resolve(breakpoint.name() + ".png")).exists();
		}
	}

}
