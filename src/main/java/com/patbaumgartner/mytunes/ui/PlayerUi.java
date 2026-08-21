package com.patbaumgartner.mytunes.ui;

import com.patbaumgartner.mytunes.persistence.Preferences;
import com.patbaumgartner.mytunes.player.PlayerState;
import com.patbaumgartner.mytunes.platform.AudioElement;
import com.patbaumgartner.mytunes.platform.Dom;
import com.patbaumgartner.mytunes.platform.Js;
import com.patbaumgartner.mytunes.platform.MediaSessionBridge;
import com.patbaumgartner.mytunes.platform.PictureInPictureBridge;
import com.patbaumgartner.mytunes.stations.Station;
import com.patbaumgartner.mytunes.stations.StationCatalogue;
import com.patbaumgartner.mytunes.themes.Background;
import com.patbaumgartner.mytunes.themes.BackgroundCatalogue;
import org.graalvm.webimage.api.JSObject;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Connects the player state machine to the browser: builds the view, translates user
 * gestures into state changes, and renders state back into the DOM and the audio element.
 */
@Service
public class PlayerUi {

	private final Dom dom;

	private final PlayerState player;

	private final StationCatalogue stations;

	private final BackgroundCatalogue backgrounds;

	private final AudioElement audio;

	private final MediaSessionBridge mediaSession;

	private final PictureInPictureBridge pictureInPicture;

	private @Nullable PlayerView view;

	private PlayerView view() {
		PlayerView current = this.view;
		if (current == null) {
			throw new IllegalStateException("PlayerUi.start() must run before the interface is rendered");
		}
		return current;
	}

	private String renderedStationId = "";

	private String renderedBackgroundId = "";

	public PlayerUi(Dom dom, PlayerState player, StationCatalogue stations, BackgroundCatalogue backgrounds,
			AudioElement audio, MediaSessionBridge mediaSession, PictureInPictureBridge pictureInPicture) {
		this.dom = dom;
		this.player = player;
		this.stations = stations;
		this.backgrounds = backgrounds;
		this.audio = audio;
		this.mediaSession = mediaSession;
		this.pictureInPicture = pictureInPicture;
	}

	public void start() {
		this.view = new PlayerView(this.dom);
		buildStationMenu();
		buildInfoPanel();
		wireControls();
		wireAudio();
		wireMediaSession();
		this.player.onChange((state) -> render());
		this.player.restore(new Preferences(this.stations.first().id(), this.backgrounds.first().id(), 0.7, false));
		render();
		this.dom.attribute(this.dom.byId("mytunes"), "data-ready", "true");
	}

	private void buildStationMenu() {
		for (Station station : this.stations.all()) {
			JSObject item = view().listItem(station.id(), station.name(), station.genre());
			this.dom.on(item, "click", (event) -> {
				this.player.selectStation(station.id());
				closeStationMenu();
			});
			this.dom.append(view().stationMenu, item);
		}
	}

	private void buildInfoPanel() {
		JSObject heading = this.dom.create("h2");
		this.dom.text(heading, "myTunes");
		JSObject body = this.dom.create("p");
		this.dom.text(body, "A DevTunes FM inspired radio player. The entire application, including this "
				+ "interface, is Java compiled to WebAssembly and runs in your browser. There is no server.");
		JSObject streams = this.dom.create("p");
		this.dom.text(streams, "Streams are provided by SomaFM, listener supported and independent of myTunes. "
				+ "Availability may change without notice.");
		this.dom.append(view().infoPanel, heading);
		this.dom.append(view().infoPanel, body);
		this.dom.append(view().infoPanel, streams);
	}

	private void wireControls() {
		this.dom.on(view().playButton, "click", (event) -> this.player.togglePlay());
		this.dom.on(view().muteButton, "click", (event) -> this.player.toggleMute());
		this.dom.on(view().stationButton, "click", (event) -> toggleHidden(view().stationMenu, view().stationButton));
		this.dom.on(view().backgroundButton, "click", (event) -> this.player
			.selectBackground(this.backgrounds.next(this.player.preferences().backgroundId()).id()));
		this.dom.on(view().miniPlayerButton, "click", (event) -> openMiniPlayer());
		this.dom.on(view().infoButton, "click", (event) -> toggleHidden(view().infoPanel, view().infoButton));
		this.dom.on(view().volumeSlider, "input", (event) -> this.player.volume(readSliderValue() / 100.0));

		this.dom.on(this.dom.document(), "keydown", (event) -> {
			String key = Js.getString(event, "key");
			if (key == null) {
				return;
			}
			switch (key) {
				case " ", "k" -> this.player.togglePlay();
				case "m" -> this.player.toggleMute();
				case "ArrowRight" -> this.player.nextStation();
				case "ArrowLeft" -> this.player.previousStation();
				case "ArrowUp" -> this.player.volume(this.player.preferences().volume() + 0.05);
				case "ArrowDown" -> this.player.volume(this.player.preferences().volume() - 0.05);
				case "Escape" -> closeStationMenu();
				default -> {
				}
			}
		});
	}

	private void wireAudio() {
		this.audio.on("playing", (event) -> this.player.confirmPlaying());
		this.audio.on("waiting", (event) -> this.player.nowPlaying(this.player.station().name()));
		this.audio.on("error", (event) -> this.player.fail("Stream unavailable"));
		this.audio.on("stalled", (event) -> this.player.fail("Stream stalled"));
	}

	private void wireMediaSession() {
		if (!this.mediaSession.supported()) {
			return;
		}
		this.mediaSession.action("play", (event) -> this.player.requestPlay());
		this.mediaSession.action("pause", (event) -> this.player.requestPause());
		this.mediaSession.action("nexttrack", (event) -> this.player.nextStation());
		this.mediaSession.action("previoustrack", (event) -> this.player.previousStation());
	}

	/**
	 * Opens the floating mini player. The Picture-in-Picture document is a second browser
	 * window, so its body is built with the same Java DOM vocabulary as the main page and
	 * its controls drive the same player state.
	 */
	private void openMiniPlayer() {
		if (!this.pictureInPicture.supported()) {
			this.player.fail("This browser has no Document Picture-in-Picture support");
			return;
		}
		this.pictureInPicture.open(360, 120, (body) -> {
			Dom pip = this.pictureInPicture.dom();
			JSObject shell = pip.createWithClass("div", "mini");
			JSObject title = pip.createWithClass("span", "mini__title");
			pip.attribute(title, "id", "mini-title");
			pip.text(title, this.player.station().name());
			JSObject toggle = pip.createWithClass("button", "mini__toggle");
			pip.attribute(toggle, "id", "mini-toggle");
			pip.attribute(toggle, "type", "button");
			pip.text(toggle, this.player.playRequested() ? "Pause" : "Play");
			pip.on(toggle, "click", (event) -> {
				this.player.togglePlay();
				pip.text(toggle, this.player.playRequested() ? "Pause" : "Play");
			});
			pip.append(shell, title);
			pip.append(shell, toggle);
			pip.append(body, shell);
			this.player.onChange((state) -> pip.text(title, state.station().name()));
		}, this.player::fail);
	}

	private void render() {
		Preferences preferences = this.player.preferences();
		Station station = this.player.station();
		Background background = this.backgrounds.byIdOrFirst(preferences.backgroundId());

		this.dom.text(view().stationName, station.name());
		this.dom.text(view().stationGenre, station.genre());
		this.dom.text(view().nowPlaying,
				this.player.nowPlaying().isBlank() ? station.name() : this.player.nowPlaying());
		this.dom.text(view().statusLine, statusText());

		renderBackground(background);
		renderStation(station);
		renderTransport();

		this.audio.volume(preferences.muted() ? 0.0 : preferences.volume());
		this.audio.muted(preferences.muted());
		Js.set(view().volumeSlider, "value", Math.round(preferences.volume() * 100.0));
		this.dom.toggleClass(view().muteButton, "is-muted", preferences.muted());
	}

	private void renderBackground(Background background) {
		if (background.id().equals(this.renderedBackgroundId)) {
			return;
		}
		this.renderedBackgroundId = background.id();
		this.dom.style(view().backdropNext, "backgroundImage", "url(" + background.assetPath() + ")");
		this.dom.addClass(view().backdropNext, "backdrop--visible");
		this.dom.style(view().backdropBase, "backgroundImage", "url(" + background.assetPath() + ")");
		this.dom.style(this.dom.byId("mytunes"), "accentColor", background.accent());
		this.dom.attribute(this.dom.byId("mytunes"), "data-background", background.id());
	}

	private void renderStation(Station station) {
		if (station.id().equals(this.renderedStationId)) {
			return;
		}
		this.renderedStationId = station.id();
		this.audio.source(station.streamUrl());
		this.audio.load();
		this.mediaSession.metadata(station.name(), station.genre(), "myTunes",
				this.backgrounds.byIdOrFirst(this.player.preferences().backgroundId()).assetPath());
		if (this.player.playRequested()) {
			startPlayback();
		}
	}

	private void renderTransport() {
		boolean playing = this.player.playRequested();
		this.dom.toggleClass(view().playButton, "is-playing", playing);
		this.dom.attribute(view().playButton, "aria-label", playing ? "Pause" : "Play");
		this.mediaSession.playbackState(playing ? "playing" : "paused");
		if (playing && this.audio.paused()) {
			startPlayback();
		}
		else if (!playing && !this.audio.paused()) {
			this.audio.pause();
		}
	}

	private void startPlayback() {
		this.audio.play(this.player::fail);
	}

	private String statusText() {
		return switch (this.player.status()) {
			case IDLE -> this.player.station().genre();
			case LOADING -> "Connecting\u2026";
			case PLAYING -> this.player.station().genre();
			case PAUSED -> "Paused";
			case FAILED -> this.player.errorMessage();
		};
	}

	private double readSliderValue() {
		String raw = Js.getString(view().volumeSlider, "value");
		try {
			return (raw == null) ? 0.0 : Double.parseDouble(raw);
		}
		catch (NumberFormatException ex) {
			return 0.0;
		}
	}

	private void toggleHidden(JSObject panel, JSObject trigger) {
		boolean hidden = Js.get(panel, "hidden") instanceof org.graalvm.webimage.api.JSBoolean flag && flag.asBoolean();
		Js.set(panel, "hidden", !hidden);
		this.dom.attribute(trigger, "aria-expanded", hidden ? "true" : "false");
	}

	private void closeStationMenu() {
		Js.set(view().stationMenu, "hidden", true);
		this.dom.attribute(view().stationButton, "aria-expanded", "false");
	}

}
