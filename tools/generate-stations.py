#!/usr/bin/env python3
"""Proposes verified third-party channels for the myTunes station catalogue.

The catalogue in StationCatalogue.java is hand-curated, not fetched at runtime: myTunes runs
entirely in the browser, so every stream it points at must be HTTPS, credential-free and
CORS-permissive, and a directory lookup at runtime would add a third-party dependency the
project deliberately avoids. This script does the legwork at authoring time instead.

It does two things:

1. Re-verifies every third-party stream already in StationCatalogue.java the way a browser
   would use it: a GET carrying an Origin header must answer HTTP 200 with an audio content
   type and an Access-Control-Allow-Origin of "*" or an echo of the origin.
2. Queries the open, key-free Radio Browser directory (https://www.radio-browser.info/) for
   candidates per catalogue category, applies the same verification gate, and prints ready to
   paste `new Station(...)` lines for the survivors.

Nothing is written to the catalogue automatically. A human picks from the verified output,
checks the provider's terms, and commits the result — StationCatalogueTests then enforces the
invariants (no credential-bearing URLs, unique ids, generated channel leads each category).

Stdlib only, like the other tools here. Usage:

    tools/generate-stations.py            # verify the current catalogue and propose candidates
    tools/generate-stations.py --verify   # only re-verify the current catalogue
"""
import argparse
import json
import pathlib
import re
import socket
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request

CATALOGUE = (pathlib.Path(__file__).resolve().parents[1]
             / "src" / "main" / "java" / "com" / "patbaumgartner" / "mytunes"
             / "stations" / "StationCatalogue.java")
ORIGIN = "https://mytunes.invalid"
USER_AGENT = "myTunes-station-generator/1.0 (+https://github.com/patbaumgartner/myTunes)"
TIMEOUT = 8
CANDIDATES_PER_CATEGORY = 3

# Catalogue category -> Radio Browser tags to search, in order of preference.
CATEGORY_TAGS = {
    "Ambient": ["ambient", "drone"],
    "Chill": ["chillout", "lofi"],
    "Beats": ["downtempo", "trip-hop"],
    "Bass": ["drum and bass", "dubstep"],
    "Electronic": ["idm", "electronic"],
    "Trance": ["trance", "psytrance"],
    "Wave": ["synthwave", "new wave"],
    "Hacker": ["industrial", "demoscene"],
}

# Signed-CDN and API-key parameters; StationCatalogueTests bans these too.
CREDENTIAL_PARAMS = re.compile(r"(Policy|Signature|Key-Pair-Id|client_id)=", re.IGNORECASE)


def probe(url):
    """Fetches url the way a browser <audio> element would and returns (ok, reason)."""
    request = urllib.request.Request(url, headers={
        "Origin": ORIGIN,
        "User-Agent": USER_AGENT,
        "Accept": "*/*",
    })
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
            status = response.status
            content_type = (response.headers.get("Content-Type") or "").split(";")[0].strip()
            allow_origin = response.headers.get("Access-Control-Allow-Origin", "")
    except (urllib.error.URLError, ssl.SSLError, socket.timeout, ConnectionError, OSError) as error:
        return False, f"request failed: {error}"
    if status != 200:
        return False, f"HTTP {status}"
    if not content_type.startswith("audio/"):
        return False, f"content type {content_type or '(none)'}"
    if allow_origin not in ("*", ORIGIN):
        return False, f"Access-Control-Allow-Origin {allow_origin or '(absent)'}"
    return True, f"200 {content_type}, ACAO {allow_origin}"


def catalogue_streams():
    """Third-party stream URLs currently committed in StationCatalogue.java."""
    source = CATALOGUE.read_text()
    return sorted(set(re.findall(r'"(https://[^"]+)"', source)))


def verify_catalogue():
    print(f"Verifying third-party streams in {CATALOGUE.name} as a browser (Origin: {ORIGIN})\n")
    failures = 0
    for url in catalogue_streams():
        ok, reason = probe(url)
        print(f"  {'PASS' if ok else 'FAIL'}  {url}  ({reason})")
        if not ok:
            failures += 1
    print(f"\n{failures} of {len(catalogue_streams())} committed streams refuse browser playback."
          if failures else "\nAll committed third-party streams answer browsers with CORS.")
    return failures


def radio_browser_search(tag):
    """Queries Radio Browser for HTTPS MP3 stations carrying a tag, popular first."""
    query = urllib.parse.urlencode({
        "tag": tag,
        "codec": "MP3",
        "is_https": "true",
        "lastcheckok": "1",
        "hidebroken": "true",
        "order": "votes",
        "reverse": "true",
        "limit": "15",
    })
    url = f"https://all.api.radio-browser.info/json/stations/search?{query}"
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
            return json.load(response)
    except (urllib.error.URLError, ssl.SSLError, socket.timeout, ConnectionError, OSError, ValueError) as error:
        print(f"  Radio Browser query for tag '{tag}' failed: {error}", file=sys.stderr)
        return []


def station_id(name):
    return re.sub(r"-{2,}", "-", re.sub(r"[^a-z0-9]+", "-", name.lower())).strip("-")


def propose_candidates():
    known = set(catalogue_streams())
    print("\nProposing CORS-verified candidates from Radio Browser (https://www.radio-browser.info/)\n"
          "Review each provider's terms before committing an entry.\n")
    for category, tags in CATEGORY_TAGS.items():
        print(f"// {category}")
        found = 0
        seen = set()
        for tag in tags:
            if found >= CANDIDATES_PER_CATEGORY:
                break
            for station in radio_browser_search(tag):
                if found >= CANDIDATES_PER_CATEGORY:
                    break
                url = (station.get("url_resolved") or station.get("url") or "").strip()
                name = (station.get("name") or "").strip()
                if not url or not name or url in known or url in seen:
                    continue
                # A name that is all punctuation/non-Latin yields no usable id, and
                # keyword-stuffed directory names make unusable menu entries.
                if not station_id(name) or len(name) > 60:
                    continue
                if CREDENTIAL_PARAMS.search(url):
                    continue
                seen.add(url)
                ok, reason = probe(url)
                if not ok:
                    print(f"//   rejected {name}: {reason}")
                    continue
                genre = tag.title()
                print(f'new Station("{station_id(name)}", "{name}", "{genre}", "{category}",\n'
                      f'\t\t"{url}", "{name}"),  // {reason}, votes={station.get("votes", 0)}')
                found += 1
        if found == 0:
            print("//   no verified candidate survived the gate")
        print()


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--verify", action="store_true",
                        help="only re-verify the streams already committed in StationCatalogue.java")
    arguments = parser.parse_args()
    failures = verify_catalogue()
    if not arguments.verify:
        propose_candidates()
    return 1 if arguments.verify and failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
