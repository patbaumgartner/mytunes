package com.patbaumgartner.mytunes.player;

/** What the player is currently doing, which the interface renders directly. */
public enum PlaybackStatus {

	/** Nothing has been requested yet. */
	IDLE,

	/** A stream was requested and the browser is still buffering. */
	LOADING,

	/** Audio is audible. */
	PLAYING,

	/** Playback was stopped by the user. */
	PAUSED,

	/** The stream could not be played. */
	FAILED

}
