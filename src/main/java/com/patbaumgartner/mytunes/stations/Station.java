package com.patbaumgartner.mytunes.stations;

/**
 * A radio station myTunes can tune to.
 *
 * @param id stable identifier, also the value persisted in preferences
 * @param name display name shown in the station selector
 * @param genre short descriptor shown next to the station name
 * @param category the menu group this channel belongs to
 * @param streamUrl a publicly reachable stream
 * @param attribution who operates the stream, shown in the information panel
 */
public record Station(String id, String name, String genre, String category, String streamUrl, String attribution) {

	/**
	 * A channel served from this repository rather than a third-party host. Self-hosted
	 * channels are finite generated files standing in for a continuous station, so
	 * playback repeats them.
	 */
	public boolean selfHosted() {
		return !this.streamUrl.startsWith("http");
	}

}
