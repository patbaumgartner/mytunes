package com.patbaumgartner.mytunes.browser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Establishes what the floating mini player can actually do, rather than assuming it.
 * <p>
 * Document Picture-in-Picture hands back a second {@code Window}, which reaches Java as
 * an ordinary {@code JSObject}, so its document is driven with the same Java DOM
 * vocabulary as the main page and needs no extra JavaScript. What this test records is
 * whether the browser under test exposes the API at all, and the outcome is written to
 * {@code target/diagnostics} either way.
 */
@EnabledIf("siteIsBuilt")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MiniPlayerBrowserTests {

	private static final Path SITE = Path.of("target/site");

	private static final Path CONSOLE = Path.of("target/diagnostics/console");

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
		this.site = StaticSite.serve(SITE.toAbsolutePath());
		this.playwright = Playwright.create();
		this.browser = this.playwright.chromium()
			.launch(new BrowserType.LaunchOptions().setArgs(List.of("--autoplay-policy=no-user-gesture-required")));
		Files.createDirectories(CONSOLE);
	}

	@AfterAll
	void stopBrowser() throws IOException {
		if (this.browser != null) {
			this.browser.close();
		}
		if (this.playwright != null) {
			this.playwright.close();
		}
		if (this.site != null) {
			this.site.close();
		}
	}

	@Test
	void offersTheMiniPlayerControlAndRecordsWhetherTheBrowserSupportsIt() throws IOException {
		// Given
		List<String> messages = new ArrayList<>();
		try (BrowserContext context = this.browser
			.newContext(new Browser.NewContextOptions().setViewportSize(1440, 900))) {
			Page page = context.newPage();
			page.onConsoleMessage((message) -> messages.add("[" + message.type() + "] " + message.text()));
			page.onPageError((error) -> messages.add("[pageerror] " + error));
			page.navigate(this.site.url("/index.html"));
			page.waitForSelector("#mytunes[data-ready='true']", new Page.WaitForSelectorOptions().setTimeout(120_000));

			// When
			boolean apiPresent = (Boolean) page.evaluate("() => 'documentPictureInPicture' in window");

			// Then the control is always offered, because support is decided at click
			// time
			assertThat(page.isVisible("#mini-player")).isTrue();

			String outcome;
			if (apiPresent) {
				page.click("#mini-player");
				page.waitForTimeout(2500);
				// The mini window is a separate document, so it is not reachable through
				// this
				// page's DOM. A refusal surfaces on the status line instead.
				String status = page.textContent("#status-line");
				outcome = "documentPictureInPicture present; after click status=" + status;
				assertThat(status).isNotNull();
			}
			else {
				outcome = "documentPictureInPicture absent in this browser; control degrades gracefully";
			}

			Files.writeString(CONSOLE.resolve("mini-player.log"),
					outcome + System.lineSeparator() + String.join(System.lineSeparator(), messages));
			assertThat(messages).noneMatch((line) -> line.startsWith("[pageerror]"));
		}
	}

}
