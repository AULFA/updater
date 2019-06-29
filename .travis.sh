#!/bin/sh

if [ -z "${LFA_BUILDS_USER}" ]
then
  echo "error: LFA_BUILDS_USER is not defined" 1>&2
  exit 1
fi

if [ -z "${LFA_BUILDS_PASSWORD}" ]
then
  echo "error: LFA_BUILDS_PASSWORD is not defined" 1>&2
  exit 1
fi

(cat <<EOF

EOF
) > gradle.properties.tmp || exit 1

mv gradle.properties.tmp gradle.properties || exit 1

exec ./gradlew clean assemble test
