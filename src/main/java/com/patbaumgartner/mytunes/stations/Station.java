package com.patbaumgartner.mytunes.stations;

/**
 * A radio station myTunes can tune to.
 *
 * @param id stable identifier, also the value persisted in preferences
 * @param name display name shown in the station selector
 * @param genre short descriptor shown next to the station name
 * @param streamUrl a publicly reachable, CORS enabled stream
 * @param attribution who operates the stream, shown in the information panel
 */
public record Station(String id, String name, String genre, String streamUrl, String attribution) {
}
