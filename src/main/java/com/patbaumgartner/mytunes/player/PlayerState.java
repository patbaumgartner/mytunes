package com.patbaumgartner.mytunes.player;

import java.util.List;
import java.util.function.Consumer;

import com.patbaumgartner.mytunes.persistence.Preferences;
import com.patbaumgartner.mytunes.persistence.PreferencesStore;
import com.patbaumgartner.mytunes.stations.Station;
import com.patbaumgartner.mytunes.stations.StationCatalogue;

/**
 * The player state machine.
 * <p>
 * This class holds no reference to the DOM or to any browser API, which is what lets it
 * be unit tested on a plain JVM. It records what the player should be doing and notifies
 * listeners; translating that into an audio element and pixels is the job of the
 * {@code ui} module.
 */
public class PlayerState {

	private final StationCatalogue stations;

	private final PreferencesStore preferencesStore;

	private final List<Consumer<PlayerState>> listeners = new java.util.ArrayList<>();

	private Preferences preferences;

	private PlaybackStatus status = PlaybackStatus.IDLE;

	private String nowPlaying = "";

	private String errorMessage = "";

	public PlayerState(StationCatalogue stations, PreferencesStore preferencesStore) {
		this.stations = stations;
		this.preferencesStore = preferencesStore;
		this.preferences = new Preferences(stations.first().id(), "dawn-lake", 0.7, false);
	}

	public void restore(Preferences defaults) {
		this.preferences = this.preferencesStore.load(defaults);
		notifyListeners();
	}

	public void onChange(Consumer<PlayerState> listener) {
		this.listeners.add(listener);
	}

	public Station station() {
		return this.stations.byIdOrFirst(this.preferences.stationId());
	}

	public Preferences preferences() {
		return this.preferences;
	}

	public PlaybackStatus status() {
		return this.status;
	}

	public String nowPlaying() {
		return this.nowPlaying;
	}

	public String errorMessage() {
		return this.errorMessage;
	}

	public boolean playRequested() {
		return this.status == PlaybackStatus.LOADING || this.status == PlaybackStatus.PLAYING;
	}

	public void selectStation(String id) {
		this.preferences = this.preferences.withStation(this.stations.byIdOrFirst(id).id());
		this.nowPlaying = "";
		this.errorMessage = "";
		this.status = playRequested() ? PlaybackStatus.LOADING : PlaybackStatus.IDLE;
		persistAndNotify();
	}

	public void nextStation() {
		selectStation(this.stations.next(this.preferences.stationId()).id());
	}

	public void previousStation() {
		selectStation(this.stations.previous(this.preferences.stationId()).id());
	}

	public void selectBackground(String id) {
		this.preferences = this.preferences.withBackground(id);
		persistAndNotify();
	}

	public void volume(double value) {
		this.preferences = this.preferences.withVolume(value);
		if (value > 0.0 && this.preferences.muted()) {
			this.preferences = this.preferences.withMuted(false);
		}
		persistAndNotify();
	}

	public void toggleMute() {
		this.preferences = this.preferences.withMuted(!this.preferences.muted());
		persistAndNotify();
	}

	public void requestPlay() {
		this.errorMessage = "";
		this.status = PlaybackStatus.LOADING;
		notifyListeners();
	}

	public void requestPause() {
		this.status = PlaybackStatus.PAUSED;
		notifyListeners();
	}

	public void togglePlay() {
		if (playRequested()) {
			requestPause();
		}
		else {
			requestPlay();
		}
	}

	public void confirmPlaying() {
		this.status = PlaybackStatus.PLAYING;
		this.errorMessage = "";
		notifyListeners();
	}

	public void fail(String message) {
		this.status = PlaybackStatus.FAILED;
		this.errorMessage = (message == null || message.isBlank()) ? "Stream unavailable" : message;
		notifyListeners();
	}

	public void nowPlaying(String title) {
		this.nowPlaying = (title == null) ? "" : title;
		notifyListeners();
	}

	private void persistAndNotify() {
		this.preferencesStore.save(this.preferences);
		notifyListeners();
	}

	private void notifyListeners() {
		this.listeners.forEach((listener) -> listener.accept(this));
	}

}
