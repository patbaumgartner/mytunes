package com.patbaumgartner.mytunes.ui;

import com.patbaumgartner.mytunes.platform.Dom;
import com.patbaumgartner.mytunes.platform.Js;
import org.graalvm.webimage.api.JSObject;

/**
 * Builds the myTunes interface as a DOM tree, from Java.
 * <p>
 * The markup is created here rather than written into {@code index.html} so that
 * behaviour and structure stay in one language. {@code index.html} only bootstraps the
 * WebAssembly module. Structure is separated from behaviour: this class produces elements
 * and holds references to them, while {@link PlayerUi} decides what they do and what they
 * show.
 */
final class PlayerView {

	private final Dom dom;

	final JSObject backdropBase;

	final JSObject backdropNext;

	final JSObject stationButton;

	final JSObject stationName;

	final JSObject stationGenre;

	final JSObject stationMenu;

	final JSObject backgroundButton;

	final JSObject miniPlayerButton;

	final JSObject infoButton;

	final JSObject infoPanel;

	final JSObject playButton;

	final JSObject nowPlaying;

	final JSObject statusLine;

	final JSObject muteButton;

	final JSObject volumeSlider;

	PlayerView(Dom dom) {
		this.dom = dom;
		JSObject root = dom.byId("mytunes");

		this.backdropBase = dom.createWithClass("div", "backdrop");
		this.backdropNext = dom.createWithClass("div", "backdrop backdrop--next");
		dom.append(root, this.backdropBase);
		dom.append(root, this.backdropNext);
		dom.append(root, dom.createWithClass("div", "scrim"));

		JSObject header = dom.createWithClass("header", "bar bar--top");
		this.stationButton = dom.createWithClass("button", "station-select");
		dom.attribute(this.stationButton, "id", "station-select");
		dom.attribute(this.stationButton, "aria-haspopup", "listbox");
		dom.attribute(this.stationButton, "aria-expanded", "false");
		JSObject label = dom.createWithClass("span", "station-select__label");
		this.stationName = dom.createWithClass("span", "station-select__name");
		this.stationGenre = dom.createWithClass("span", "station-select__genre");
		dom.append(label, this.stationName);
		dom.append(label, this.stationGenre);
		dom.append(this.stationButton, icon(dom, "headphones"));
		dom.append(this.stationButton, label);
		dom.append(this.stationButton, icon(dom, "chevron"));

		JSObject actions = dom.createWithClass("div", "bar__actions");
		this.backgroundButton = iconButton(dom, "background-toggle", "image", "Change background");
		this.miniPlayerButton = iconButton(dom, "mini-player", "pip", "Open mini player");
		this.infoButton = iconButton(dom, "info-toggle", "info", "About myTunes");
		dom.append(actions, this.backgroundButton);
		dom.append(actions, this.miniPlayerButton);
		dom.append(actions, this.infoButton);

		dom.append(header, this.stationButton);
		dom.append(header, actions);
		dom.append(root, header);

		this.stationMenu = dom.createWithClass("ul", "station-menu");
		dom.attribute(this.stationMenu, "id", "station-menu");
		dom.attribute(this.stationMenu, "role", "listbox");
		dom.attribute(this.stationMenu, "hidden", "hidden");
		dom.append(root, this.stationMenu);

		this.infoPanel = dom.createWithClass("section", "info-panel");
		dom.attribute(this.infoPanel, "id", "info-panel");
		dom.attribute(this.infoPanel, "hidden", "hidden");
		dom.append(root, this.infoPanel);

		JSObject footer = dom.createWithClass("footer", "bar bar--bottom");
		JSObject left = dom.createWithClass("div", "transport");
		this.playButton = iconButton(dom, "play-toggle", "play", "Play");
		JSObject titles = dom.createWithClass("div", "transport__titles");
		this.nowPlaying = dom.createWithClass("span", "transport__track");
		dom.attribute(this.nowPlaying, "id", "now-playing");
		this.statusLine = dom.createWithClass("span", "transport__status");
		dom.attribute(this.statusLine, "id", "status-line");
		dom.append(titles, this.nowPlaying);
		dom.append(titles, this.statusLine);
		dom.append(left, this.playButton);
		dom.append(left, titles);

		JSObject right = dom.createWithClass("div", "volume");
		this.muteButton = iconButton(dom, "mute-toggle", "volume", "Mute");
		this.volumeSlider = dom.create("input");
		dom.attribute(this.volumeSlider, "id", "volume-slider");
		dom.attribute(this.volumeSlider, "type", "range");
		dom.attribute(this.volumeSlider, "min", "0");
		dom.attribute(this.volumeSlider, "max", "100");
		dom.attribute(this.volumeSlider, "aria-label", "Volume");
		Js.set(this.volumeSlider, "className", "volume__slider");
		dom.append(right, this.muteButton);
		dom.append(right, this.volumeSlider);

		dom.append(footer, left);
		dom.append(footer, right);
		dom.append(root, footer);
	}

	JSObject listItem(String stationId, String name, String genre) {
		JSObject item = this.dom.createWithClass("li", "station-menu__item");
		this.dom.attribute(item, "role", "option");
		this.dom.attribute(item, "data-station", stationId);
		JSObject primary = this.dom.createWithClass("span", "station-menu__name");
		this.dom.text(primary, name);
		JSObject secondary = this.dom.createWithClass("span", "station-menu__genre");
		this.dom.text(secondary, genre);
		this.dom.append(item, primary);
		this.dom.append(item, secondary);
		return item;
	}

	private static JSObject iconButton(Dom dom, String id, String iconName, String accessibleName) {
		JSObject button = dom.createWithClass("button", "icon-button");
		dom.attribute(button, "id", id);
		dom.attribute(button, "type", "button");
		dom.attribute(button, "aria-label", accessibleName);
		dom.append(button, icon(dom, iconName));
		return button;
	}

	private static JSObject icon(Dom dom, String name) {
		JSObject span = dom.createWithClass("span", "icon icon--" + name);
		dom.attribute(span, "aria-hidden", "true");
		return span;
	}

}
