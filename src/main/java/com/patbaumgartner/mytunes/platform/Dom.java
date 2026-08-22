package com.patbaumgartner.mytunes.platform;

import java.util.function.Consumer;

import org.graalvm.webimage.api.JSObject;
import org.springframework.stereotype.Component;

/**
 * DOM access expressed in Java. Every method delegates to {@link Js}, so no JavaScript is
 * written here; this is the vocabulary the {@code ui} module builds the interface from.
 */
@Component
public final class Dom {

	private final JSObject document;

	public Dom() {
		this.document = BrowserWindow.document();
	}

	/**
	 * Looks up an element the caller knows the bootstrap page declares. A missing element
	 * means the page and the application have diverged, which is a defect rather than a
	 * state to model.
	 */
	public JSObject byId(String id) {
		return Js.callObject(this.document, "getElementById", id);
	}

	public JSObject create(String tagName) {
		return Js.callObject(this.document, "createElement", tagName);
	}

	public JSObject createWithClass(String tagName, String className) {
		JSObject element = create(tagName);
		Js.set(element, "className", className);
		return element;
	}

	public void append(JSObject parent, JSObject child) {
		Js.call(parent, "appendChild", child);
	}

	public void text(JSObject element, String value) {
		Js.set(element, "textContent", value);
	}

	public void attribute(JSObject element, String name, String value) {
		Js.call(element, "setAttribute", name, value);
	}

	public void addClass(JSObject element, String className) {
		Js.call(Js.object(element, "classList"), "add", className);
	}

	public void removeClass(JSObject element, String className) {
		Js.call(Js.object(element, "classList"), "remove", className);
	}

	public void toggleClass(JSObject element, String className, boolean present) {
		if (present) {
			addClass(element, className);
		}
		else {
			removeClass(element, className);
		}
	}

	public void style(JSObject element, String property, String value) {
		Js.set(Js.object(element, "style"), property, value);
	}

	/**
	 * Registers a Java listener for a DOM event. The listener must accept one argument,
	 * because the browser always passes the event object and a zero-argument functional
	 * interface fails with "No matching signature for single abstract method".
	 */
	public void on(JSObject element, String event, Consumer<JSObject> listener) {
		Js.call(element, "addEventListener", event, listener);
	}

	public JSObject body() {
		return Js.object(this.document, "body");
	}

	public JSObject document() {
		return this.document;
	}

}
