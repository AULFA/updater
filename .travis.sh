#!/bin/sh

(cat <<EOF
nexusUsername = notausername
nexusPassword = notapassword
EOF
) > gradle.properties.tmp || exit 1

mv gradle.properties.tmp gradle.properties || exit 1

exec ./gradlew clean assemble test
