package com.patbaumgartner.mytunes.themes;

/**
 * A selectable background.
 *
 * @param id stable identifier, also the value persisted in preferences
 * @param name display name shown in the background picker
 * @param assetPath path to the artwork, relative to the served site root
 * @param accent colour used for active controls while this background is shown
 */
public record Background(String id, String name, String assetPath, String accent) {
}
