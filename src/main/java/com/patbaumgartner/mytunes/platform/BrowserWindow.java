package com.patbaumgartner.mytunes.platform;

import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSObject;

/**
 * The one and only place in myTunes where JavaScript text is written by a human.
 * <p>
 * WebAssembly has no direct access to the DOM, so every Java-to-WASM toolchain needs a
 * JavaScript interop layer. GraalVM Web Image supplies {@link JSObject}, whose
 * {@code get}, {@code set} and {@code call} are enough to express every browser operation
 * this application performs in plain Java, but it offers no way to obtain the
 * <em>first</em> reference. The interop API contains no global accessor, and
 * {@code @JS.Import} imports a JavaScript <em>class</em>, not an instance, so it cannot
 * return {@code document}. This was verified against Oracle GraalVM 25.0.4, GraalVM CE
 * 25.2.4 and the Early Access build {@code jdk-25i3-25.0.4.1-ea.02}.
 * <p>
 * The two declarations below are therefore the minimum possible JavaScript surface: each
 * is a single expression that names a browser root. They are foreign-function
 * declarations, closer to a JNI signature than to application code. No application logic,
 * DOM bridge or audio adapter is written in JavaScript; everything else in this codebase
 * is Java. {@code NoHandwrittenJavaScriptTests} enforces that no {@code .js},
 * {@code .mjs}, {@code .cjs} or {@code .ts} file exists in the repository and that no
 * other class declares {@code @JS}.
 */
public final class BrowserWindow {

	@JS("return window;")
	static native JSObject window();

	@JS("return document;")
	static native JSObject document();

	private BrowserWindow() {
	}

}
