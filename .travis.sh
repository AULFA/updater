#!/bin/sh

if [ -z "${LFA_BUILDS_SSH_KEY}" ]
then
  echo "LFA_BUILDS_SSH_KEY not set"
  exit 1
fi

mkdir -p "${HOME}/.ssh" || exit 1
echo "${LFA_BUILDS_SSH_KEY}" | base64 -d > "${HOME}/.ssh/id_ed25519" || exit 1

(cat <<EOF

nexusUsername = notausername
nexusPassword = notapassword
EOF
) >> gradle.properties || exit 1

./gradlew clean assemble test || exit 1

scp -P 1022 ./au.org.libraryforall.updater.app/build/outputs/apk/debug/*.apk travis-ci@builds.lfa.one:/sites/builds.lfa.one/apk/ || exit 1
