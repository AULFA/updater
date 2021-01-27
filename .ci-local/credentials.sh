#!/bin/bash

#------------------------------------------------------------------------
# A script to copy the necessary credentials into the right places in the
# build.
#

#------------------------------------------------------------------------
# Utility methods
#

fatal()
{
  echo "credentials.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "credentials.sh: info: $1" 1>&2
}

copy()
{
  FROM="$1"
  TO="$2"
  cp -v "${FROM}" "${TO}" || fatal "could not copy ${FROM} -> ${TO}"
}

copy .ci/credentials/lfa-keystore.jks lfa-keystore.jks
copy .ci/credentials/updater-credentials.xml one.lfa.updater.app/src/main/assets/bundled_credentials.xml

mkdir -p "$HOME/.gradle" ||
  fatal "could not create gradle configuration directory"
cat .ci/credentials/lfa-keystore.properties >> "$HOME/.gradle/gradle.properties" ||
  fatal "could not write credentials"
