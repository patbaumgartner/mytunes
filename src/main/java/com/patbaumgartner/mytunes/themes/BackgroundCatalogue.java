package com.patbaumgartner.mytunes.themes;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * The background catalogue.
 * <p>
 * DevTunes FM serves wallpapers sourced from wallhaven, whose redistribution terms are
 * not established, so none of them are reused here. These backgrounds are original
 * flat-design SVG artwork authored for myTunes and are covered by this repository's own
 * licence, which removes the attribution and redistribution question entirely rather than
 * deferring it.
 */
@Service
public class BackgroundCatalogue {

	private static final List<Background> BACKGROUNDS = List.of(
			new Background("dawn-lake", "Dawn Lake", "backgrounds/dawn-lake.svg", "#f6c177"),
			new Background("dusk-ridge", "Dusk Ridge", "backgrounds/dusk-ridge.svg", "#eb6f92"),
			new Background("night-pines", "Night Pines", "backgrounds/night-pines.svg", "#9ccfd8"),
			new Background("deep-current", "Deep Current", "backgrounds/deep-current.svg", "#31748f"),
			new Background("ember-dunes", "Ember Dunes", "backgrounds/ember-dunes.svg", "#f6ad55"));

	public List<Background> all() {
		return BACKGROUNDS;
	}

	public Background first() {
		return BACKGROUNDS.getFirst();
	}

	public Optional<Background> byId(String id) {
		return BACKGROUNDS.stream().filter((background) -> background.id().equals(id)).findFirst();
	}

	public Background byIdOrFirst(String id) {
		return byId(id).orElseGet(this::first);
	}

	public Background next(String id) {
		int index = BACKGROUNDS.indexOf(byIdOrFirst(id));
		return BACKGROUNDS.get(Math.floorMod(index + 1, BACKGROUNDS.size()));
	}

}
