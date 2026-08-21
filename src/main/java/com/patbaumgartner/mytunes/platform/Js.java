package com.patbaumgartner.mytunes.platform;

import org.graalvm.webimage.api.JSBoolean;
import org.graalvm.webimage.api.JSNumber;
import org.graalvm.webimage.api.JSObject;
import org.graalvm.webimage.api.JSString;
import org.graalvm.webimage.api.JSValue;
import org.jspecify.annotations.Nullable;

/**
 * Plain-Java calling conventions for JavaScript objects.
 * <p>
 * Two details of the Web Image API are easy to get wrong and are encapsulated here.
 * {@code JSObject.call(thisArg, args)} is {@code Function.prototype.call}, so invoking a
 * method means reading the member and then calling it with an explicit receiver. And Java
 * boxed types are not JavaScript values: passing a {@code Double} to a DOM property
 * yields "non-finite", so every primitive must cross the boundary as an explicit
 * {@link JSValue}.
 * <p>
 * The lookups come in two shapes. {@link #object} and {@link #callObject} are total: the
 * caller has asserted the member must exist, so a missing one is a defect and fails
 * immediately naming the property, rather than surfacing as a null dereference somewhere
 * later. {@link #optional} is for genuine feature detection, where a browser may
 * legitimately not provide the member.
 */
public final class Js {

	public static Object call(JSObject target, String method, Object... args) {
		Object member = target.get(JSString.of(method));
		if (!(member instanceof JSObject function)) {
			throw new IllegalStateException("JavaScript object has no callable member '" + method + "'");
		}
		return function.call(target, args);
	}

	public static JSObject callObject(JSObject target, String method, Object... args) {
		Object result = call(target, method, args);
		if (!(result instanceof JSObject object)) {
			throw new IllegalStateException("Expected '" + method + "' to return a JavaScript object");
		}
		return object;
	}

	public static JSObject object(JSObject target, String property) {
		JSObject value = optional(target, property);
		if (value == null) {
			throw new IllegalStateException("JavaScript object has no member '" + property + "'");
		}
		return value;
	}

	public static @Nullable JSObject optional(JSObject target, String property) {
		Object value = target.get(JSString.of(property));
		return (value instanceof JSObject object) ? object : null;
	}

	public static void set(JSObject target, String property, String value) {
		target.set(JSString.of(property), JSString.of(value));
	}

	public static void set(JSObject target, String property, double value) {
		target.set(JSString.of(property), JSNumber.of(value));
	}

	public static void set(JSObject target, String property, boolean value) {
		target.set(JSString.of(property), JSBoolean.of(value));
	}

	public static void set(JSObject target, String property, JSObject value) {
		target.set(JSString.of(property), value);
	}

	public static Object get(JSObject target, String property) {
		return target.get(JSString.of(property));
	}

	public static @Nullable String getString(JSObject target, String property) {
		return asString(get(target, property));
	}

	public static double getDouble(JSObject target, String property, double fallback) {
		Object value = get(target, property);
		return (value instanceof JSNumber number) ? number.asDouble() : fallback;
	}

	public static boolean getBoolean(JSObject target, String property, boolean fallback) {
		Object value = get(target, property);
		return (value instanceof JSBoolean flag) ? flag.asBoolean() : fallback;
	}

	public static @Nullable String asString(Object value) {
		return (value instanceof JSString string) ? string.asString() : null;
	}

	private Js() {
	}

}
