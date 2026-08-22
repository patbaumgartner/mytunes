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

	private static final Path EVIDENCE = Path.of("target/diagnostics/console");

	private static final Map<String, String> EXPECTED_TYPES = Map.of("/index.html", "text/html", "/styles.css",
			"text/css", "/mytunes.js", "application/javascript", "/mytunes.js.wasm", "application/wasm",
			"/backgrounds/dawn-lake.svg", "image/svg+xml", "/audio/mytunes-signal.mp3", "audio/mpeg");

	private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	private static String baseUrl() {
		return System.getProperty("mytunes.baseUrl").replaceAll("/$", "");
	}

	private static void record(String name, String content) throws IOException {
		Files.createDirectories(EVIDENCE);
		Files.writeString(EVIDENCE.resolve(name), content, StandardCharsets.UTF_8);
	}

	@Test
	void reportsReadyOnTheHealthEndpoint() throws IOException, InterruptedException {
		// When
		HttpResponse<String> response = get("/healthz");

		// Then
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("ok");
		// The file is extensionless, so nginx types it application/octet-stream; typing
		// it with add_header instead of default_type appends a second Content-Type and
		// leaves a malformed response that still passes a naive body assertion.
		assertThat(response.headers().allValues("content-type")).containsExactly("text/plain");
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
		record("docker-smoke.log", evidence.toString());
	}

	@Test
	void servesTheWasmModuleAsAnImmutableCacheableAsset() throws IOException, InterruptedException {
		// When
		HttpResponse<String> response = get("/mytunes.js.wasm");

		// Then a rebuilt image changes the tag, so the module itself may be cached hard
		assertThat(response.headers().firstValue("cache-control").orElse("")).contains("immutable");
	}

	@Test
	void servesTheWasmModulePrecompressedToClientsThatAcceptGzip() throws IOException, InterruptedException {
		// Given
		HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/mytunes.js.wasm"))
			.header("Accept-Encoding", "gzip")
			.timeout(Duration.ofSeconds(30))
			.build();

		// When
		HttpResponse<Void> response = this.client.send(request, HttpResponse.BodyHandlers.discarding());

		// Then the module travels compressed and still typed for streaming compilation
		assertThat(response.headers().firstValue("content-encoding").orElse("")).isEqualTo("gzip");
		assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("application/wasm");
		assertThat(response.headers().firstValue("vary").orElse("")).containsIgnoringCase("accept-encoding");
		// gzip -9 lands well under half the ~16 MB raw module; a regression to on-the-fly
		// level-1 gzip or to the raw file would push past this bound
		assertThat(response.headers().firstValueAsLong("content-length").orElse(Long.MAX_VALUE)).isLessThan(8_000_000L);
	}

	@Test
	void sendsSecurityHeadersOnEveryResponse() throws IOException, InterruptedException {
		// Given nginx does not merge add_header across blocks: a location that sets its
		// own
		// caching header silently discards the inherited security headers. That
		// regression is
		// invisible in the application, so it is asserted here on both a cached and an
		// uncached path, and on "/" because that is the URL every visitor actually opens.
		for (String path : List.of("/", "/index.html", "/mytunes.js.wasm")) {
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
		record("docker-image-contents.log", found.isEmpty() ? "no jar and no java executable found in " + image
				: String.join(System.lineSeparator(), found));
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
