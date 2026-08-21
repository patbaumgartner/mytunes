#!/usr/bin/env bash
# Repackages GraalVM's Web Image browser interop module as an ordinary Maven artifact.
#
# org.graalvm.webimage.api ships only as a JDK module inside Oracle GraalVM and has no Maven
# coordinates. Reaching it with javac's "add-modules" would force this project off the "release"
# flag, because "release" resolves against ct.sym and cannot see a non-standard module. Every tool
# that forks its own compiler then diverges: Spring Boot's AOT processing derives "release" from
# the same configuration and would fail.
#
# Installing it as a normal jar keeps one consistent compiler configuration across the whole build.
# Classpath entries are unaffected by "release".
set -euo pipefail

JAVA_HOME="${JAVA_HOME:-}"
if [[ -z "$JAVA_HOME" ]]; then
	echo "JAVA_HOME must point at Oracle GraalVM 25 (it provides the Wasm backend)." >&2
	exit 1
fi

JMOD="$JAVA_HOME/jmods/org.graalvm.webimage.api.jmod"
if [[ ! -f "$JMOD" ]]; then
	echo "Not found: $JMOD" >&2
	echo "This build requires Oracle GraalVM, which is the only JDK shipping the Web Image API." >&2
	exit 1
fi

VERSION="${1:-25.0.4}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

"$JAVA_HOME/bin/jmod" extract --dir "$WORK/extracted" "$JMOD"
# module-info is dropped so the result is a plain classpath jar rather than a named module.
rm -f "$WORK/extracted/classes/module-info.class"
"$JAVA_HOME/bin/jar" --create --file "$WORK/webimage-api.jar" -C "$WORK/extracted/classes" .

MVN="./mvnw"
[[ -x "$MVN" ]] || MVN="mvn"

"$MVN" -B -q org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \
	-Dfile="$WORK/webimage-api.jar" \
	-DgroupId=org.graalvm.webimage \
	-DartifactId=webimage-api \
	-Dversion="$VERSION" \
	-Dpackaging=jar

echo "Installed org.graalvm.webimage:webimage-api:$VERSION into the local repository."
