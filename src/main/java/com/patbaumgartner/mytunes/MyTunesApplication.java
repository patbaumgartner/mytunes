package com.patbaumgartner.mytunes;

import com.patbaumgartner.mytunes.persistence.PreferencesStore;
import com.patbaumgartner.mytunes.platform.AudioElement;
import com.patbaumgartner.mytunes.platform.BrowserConsole;
import com.patbaumgartner.mytunes.platform.Dom;
import com.patbaumgartner.mytunes.platform.LocalStorageKeyValueStore;
import com.patbaumgartner.mytunes.platform.MediaSessionBridge;
import com.patbaumgartner.mytunes.platform.PictureInPictureBridge;
import com.patbaumgartner.mytunes.player.PlayerState;
import com.patbaumgartner.mytunes.stations.StationCatalogue;
import com.patbaumgartner.mytunes.themes.BackgroundCatalogue;
import com.patbaumgartner.mytunes.ui.PlayerUi;

/**
 * myTunes, entirely client side.
 * <p>
 * This {@code main} runs inside the browser: GraalVM Web Image compiles it to WebAssembly
 * and the generated loader invokes it once the module is instantiated. The object graph
 * is wired by hand below, and the interface is then built from Java against the live DOM.
 * No application runs on a server at any point.
 * <p>
 * The wiring is deliberately plain constructor calls rather than a dependency injection
 * container: the graph is nine objects, fixed at compile time, and a closed-world,
 * single-threaded Wasm module gains nothing from reflection-driven assembly that it would
 * pay for in module size.
 */
public final class MyTunesApplication {

	public static void main(String[] args) {
		long started = System.currentTimeMillis();

		Dom dom = new Dom();
		PreferencesStore preferencesStore = new PreferencesStore(new LocalStorageKeyValueStore());
		StationCatalogue stations = new StationCatalogue();
		BackgroundCatalogue backgrounds = new BackgroundCatalogue();
		PlayerState player = new PlayerState(stations, preferencesStore);
		PlayerUi ui = new PlayerUi(dom, player, stations, backgrounds, new AudioElement(dom), new MediaSessionBridge(),
				new PictureInPictureBridge(dom));

		BrowserConsole.log("[mytunes] Java started in the browser in " + (System.currentTimeMillis() - started)
				+ "ms, wired by hand without a framework");

		ui.start();
		BrowserConsole.log("[mytunes] interface ready");
	}

	private MyTunesApplication() {
	}

}
