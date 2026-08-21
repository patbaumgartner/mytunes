package com.patbaumgartner.mytunes.browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serves the built site for browser tests.
 * <p>
 * The application has no server of its own, so the tests need something to hand the
 * static shell, the generated loader and the Wasm module to the browser. Point the tests
 * at a different origin with {@code -Dmytunes.baseUrl=...} to exercise the Docker image
 * instead of this.
 * <p>
 * This is a deliberately minimal HTTP responder rather than the JDK's bundled server,
 * because the project's architecture rules forbid importing {@code com.sun} packages.
 */
final class StaticSite implements AutoCloseable {

	private final ServerSocket socket;

	private final String baseUrl;

	private final AtomicBoolean running = new AtomicBoolean(true);

	private StaticSite(ServerSocket socket, String baseUrl) {
		this.socket = socket;
		this.baseUrl = baseUrl;
	}

	static StaticSite serve(Path root) throws IOException {
		String configured = System.getProperty("mytunes.baseUrl");
		if (configured != null && !configured.isBlank()) {
			return new StaticSite(null, configured.replaceAll("/$", ""));
		}
		ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
		StaticSite site = new StaticSite(socket, "http://127.0.0.1:" + socket.getLocalPort());
		Thread accepting = new Thread(() -> site.acceptLoop(root), "mytunes-static-site");
		accepting.setDaemon(true);
		accepting.start();
		return site;
	}

	private void acceptLoop(Path root) {
		while (this.running.get()) {
			try (Socket client = this.socket.accept()) {
				respond(client, root);
			}
			catch (IOException ex) {
				if (this.running.get()) {
					throw new IllegalStateException("Static site failed while serving", ex);
				}
				return;
			}
		}
	}

	private void respond(Socket client, Path root) throws IOException {
		InputStream in = client.getInputStream();
		String requestLine = readLine(in);
		// The rest of the request must be drained. Replying while the client is still
		// sending
		// headers makes the kernel reset the connection, which shows up in the browser as
		// ERR_CONNECTION_RESET part way through the multi-megabyte Wasm module.
		String header;
		do {
			header = readLine(in);
		}
		while (!header.isEmpty());

		String[] parts = requestLine.trim().split(" ");
		OutputStream out = client.getOutputStream();
		if (parts.length < 2) {
			writeStatus(out, 400);
			return;
		}
		String requested = parts[1];
		int query = requested.indexOf('?');
		if (query >= 0) {
			requested = requested.substring(0, query);
		}
		Path file = root.resolve(requested.equals("/") ? "index.html" : requested.substring(1)).normalize();
		if (!file.startsWith(root) || !Files.isRegularFile(file)) {
			writeStatus(out, 404);
			return;
		}
		byte[] body = Files.readAllBytes(file);
		String headers = "HTTP/1.1 200 OK\r\nContent-Type: " + contentType(file) + "\r\nContent-Length: " + body.length
				+ "\r\nConnection: close\r\n\r\n";
		out.write(headers.getBytes(StandardCharsets.US_ASCII));
		out.write(body);
		out.flush();
	}

	private static String readLine(InputStream in) throws IOException {
		StringBuilder line = new StringBuilder();
		int character;
		while ((character = in.read()) != -1 && character != '\n') {
			if (character != '\r') {
				line.append((char) character);
			}
		}
		return line.toString();
	}

	private static void writeStatus(OutputStream out, int status) throws IOException {
		out.write(("HTTP/1.1 " + status + " \r\nContent-Length: 0\r\nConnection: close\r\n\r\n")
			.getBytes(StandardCharsets.US_ASCII));
		out.flush();
	}

	private static String contentType(Path file) {
		String name = file.getFileName().toString();
		// A browser refuses to stream-compile a module that is not served as
		// application/wasm.
		if (name.endsWith(".wasm")) {
			return "application/wasm";
		}
		if (name.endsWith(".js")) {
			return "application/javascript";
		}
		if (name.endsWith(".css")) {
			return "text/css";
		}
		if (name.endsWith(".svg")) {
			return "image/svg+xml";
		}
		if (name.endsWith(".mp3")) {
			return "audio/mpeg";
		}
		if (name.endsWith(".html")) {
			return "text/html";
		}
		return "application/octet-stream";
	}

	String url(String path) {
		return this.baseUrl + path;
	}

	@Override
	public void close() throws IOException {
		this.running.set(false);
		if (this.socket != null) {
			this.socket.close();
		}
	}

}
