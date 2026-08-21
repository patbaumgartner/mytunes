package com.patbaumgartner.mytunes.browser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for the container that serves myTunes.
 * <p>
 * These assert the two things a browser needs from the host and the one thing the
 * architecture claims about it: that every asset is served with a type the browser will
 * accept, that the WebAssembly module in particular arrives as {@code application/wasm}
 * so it can be stream compiled, and that the image contains no JVM and no application
 * jar.
 * <p>
 * Opt-in, because they need a built image and a running container:
 * {@code -Dmytunes.baseUrl=http://127.0.0.1:8099 -Dmytunes.dockerImage=mytunes:latest}.
 */
@EnabledIfSystemProperty(named = "mytunes.baseUrl", matches = ".+")
class DockerSmokeTests {

	private static final Path EVIDENCE = Path.of("docs/parity/console");

	private static final Map<String, String> EXPECTED_TYPES = Map.of("/index.html", "text/html", "/styles.css",
			"text/css", "/mytunes.js", "application/javascript", "/mytunes.js.wasm", "application/wasm",
			"/backgrounds/dawn-lake.svg", "image/svg+xml", "/audio/mytunes-signal.mp3", "audio/mpeg");

	private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	private static String baseUrl() {
		return System.getProperty("mytunes.baseUrl").replaceAll("/$", "");
	}

	@Test
	void reportsReadyOnTheHealthEndpoint() throws IOException, InterruptedException {
		// When
		HttpResponse<String> response = get("/healthz");

		// Then
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("ok");
	}

	@Test
	void servesEveryAssetWithATypeTheBrowserAccepts() throws IOException, InterruptedException {
		// Given
		StringBuilder evidence = new StringBuilder();

		// When, Then
		for (Map.Entry<String, String> asset : EXPECTED_TYPES.entrySet()) {
			HttpResponse<String> response = get(asset.getKey());
			String contentType = response.headers().firstValue("content-type").orElse("(none)");
			evidence.append(asset.getKey())
				.append(" -> ")
				.append(response.statusCode())
				.append(' ')
				.append(contentType)
				.append(System.lineSeparator());

			assertThat(response.statusCode()).as("status for %s", asset.getKey()).isEqualTo(200);
			assertThat(contentType).as("content type for %s", asset.getKey()).startsWith(asset.getValue());
		}
		Files.writeString(EVIDENCE.resolve("docker-smoke.log"), evidence.toString(), StandardCharsets.UTF_8);
	}

	@Test
	void servesTheWasmModuleAsAnImmutableCacheableAsset() throws IOException, InterruptedException {
		// When
		HttpResponse<String> response = get("/mytunes.js.wasm");

		// Then a rebuilt image changes the tag, so the module itself may be cached hard
		assertThat(response.headers().firstValue("cache-control").orElse("")).contains("immutable");
	}

	@Test
	void sendsSecurityHeadersOnEveryResponse() throws IOException, InterruptedException {
		// Given nginx does not merge add_header across blocks: a location that sets its
		// own
		// caching header silently discards the inherited security headers. That
		// regression is
		// invisible in the application, so it is asserted here on both a cached and an
		// uncached path.
		for (String path : List.of("/index.html", "/mytunes.js.wasm")) {
			// When
			HttpResponse<String> response = get(path);

			// Then
			assertThat(response.headers().firstValue("content-security-policy").orElse(""))
				.as("Content-Security-Policy for %s", path)
				// Instantiating a Wasm module counts as eval, so without this the app
				// cannot start.
				.contains("'wasm-unsafe-eval'")
				.contains("frame-ancestors 'none'");
			assertThat(response.headers().firstValue("x-content-type-options").orElse(""))
				.as("X-Content-Type-Options for %s", path)
				.isEqualTo("nosniff");
			assertThat(response.headers().firstValue("referrer-policy").orElse("")).as("Referrer-Policy for %s", path)
				.isEqualTo("no-referrer");
		}
	}

	@Test
	@EnabledIfSystemProperty(named = "mytunes.dockerImage", matches = ".+")
	void theImageContainsNoJvmAndNoApplicationJar() throws IOException, InterruptedException {
		// Given the central architectural claim: the application runs in the browser, not
		// here
		String image = System.getProperty("mytunes.dockerImage");

		// When
		Process process = new ProcessBuilder("docker", "run", "--rm", "--entrypoint", "sh", image, "-c",
				"find / -name '*.jar' -o -name java -type f 2>/dev/null")
			.redirectErrorStream(true)
			.start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		process.waitFor();

		// Then
		List<String> found = output.lines().filter((line) -> !line.isBlank()).toList();
		Files.writeString(EVIDENCE.resolve("docker-image-contents.log"), found.isEmpty()
				? "no jar and no java executable found in " + image : String.join(System.lineSeparator(), found),
				StandardCharsets.UTF_8);
		assertThat(found).as("A JVM or jar in the runtime image would contradict the architecture").isEmpty();
	}

	private HttpResponse<String> get(String path) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + path))
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();
		return this.client.send(request, HttpResponse.BodyHandlers.ofString());
	}

}
