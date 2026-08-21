package com.patbaumgartner.mytunes.stations;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * The station catalogue.
 * <p>
 * DevTunes FM streams audio from SoundCloud CDN URLs carrying signed CloudFront
 * {@code Policy}, {@code Signature} and {@code Key-Pair-Id} parameters, minted by its own
 * private endpoint. Those are time limited credentials belonging to someone else's
 * session, so myTunes does not reuse them.
 * <p>
 * These stations are SomaFM's listener supported public streams. They answer curl with
 * {@code Access-Control-Allow-Origin: *} and {@code Content-Type: audio/mpeg}, but were
 * observed returning HTTP 403 to browser requests on every mount tested, which is
 * SomaFM's prerogative and is recorded in the README and the parity checklist rather than
 * worked around. The first entry is therefore served from this repository, so real
 * playback can always be demonstrated offline with no third-party dependency and no
 * licensing question.
 */
@Service
public class StationCatalogue {

	private static final List<Station> STATIONS = List.of(
			new Station("mytunes-signal", "myTunes Signal", "Ambient / Generated", "audio/mytunes-signal.mp3",
					"Original loop generated for myTunes by tools/generate-audio.py"),
			new Station("groovesalad", "Groove Salad", "Ambient / Downtempo",
					"https://ice1.somafm.com/groovesalad-128-mp3", "SomaFM"),
			new Station("dronezone", "Drone Zone", "Atmospheric", "https://ice1.somafm.com/dronezone-128-mp3",
					"SomaFM"),
			new Station("lush", "Lush", "Vocal / Dreampop", "https://ice1.somafm.com/lush-128-mp3", "SomaFM"),
			new Station("spacestation", "Space Station", "Electronic", "https://ice1.somafm.com/spacestation-128-mp3",
					"SomaFM"),
			new Station("deepspaceone", "Deep Space One", "Deep Ambient",
					"https://ice1.somafm.com/deepspaceone-128-mp3", "SomaFM"),
			new Station("defcon", "DEF CON Radio", "Hacker / Electro", "https://ice1.somafm.com/defcon-128-mp3",
					"SomaFM"));

	public List<Station> all() {
		return STATIONS;
	}

	public Station first() {
		return STATIONS.getFirst();
	}

	public Optional<Station> byId(String id) {
		return STATIONS.stream().filter((station) -> station.id().equals(id)).findFirst();
	}

	public Station byIdOrFirst(String id) {
		return byId(id).orElseGet(this::first);
	}

	public Station next(String id) {
		return relative(id, 1);
	}

	public Station previous(String id) {
		return relative(id, -1);
	}

	private Station relative(String id, int offset) {
		int size = STATIONS.size();
		int index = STATIONS.indexOf(byIdOrFirst(id));
		return STATIONS.get(Math.floorMod(index + offset, size));
	}

}
