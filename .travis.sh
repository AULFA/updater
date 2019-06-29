#!/bin/sh

(cat <<EOF

nexusUsername = notausername
nexusPassword = notapassword
EOF
) >> gradle.properties || exit 1

exec ./gradlew clean assemble test
