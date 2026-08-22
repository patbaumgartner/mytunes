package com.patbaumgartner.mytunes.persistence;

/**
 * The user preferences myTunes restores after a reload.
 *
 * @param stationId the selected station
 * @param backgroundId the selected background
 * @param volume playback volume between 0.0 and 1.0
 * @param muted whether output is muted
 */
public record Preferences(String stationId, String backgroundId, double volume, boolean muted) {

	public Preferences {
		// Math.clamp propagates NaN, which is not a volume; silence is the safe floor.
		volume = Double.isNaN(volume) ? 0.0 : Math.clamp(volume, 0.0, 1.0);
	}

	public Preferences withStation(String id) {
		return new Preferences(id, this.backgroundId, this.volume, this.muted);
	}

	public Preferences withBackground(String id) {
		return new Preferences(this.stationId, id, this.volume, this.muted);
	}

	public Preferences withVolume(double value) {
		return new Preferences(this.stationId, this.backgroundId, value, this.muted);
	}

	public Preferences withMuted(boolean value) {
		return new Preferences(this.stationId, this.backgroundId, this.volume, value);
	}

}
