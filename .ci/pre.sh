#!/bin/bash

exec &> >(tee -a ".ci/pre.log")

#------------------------------------------------------------------------
# Utility methods
#

fatal()
{
  echo "fatal: $1" 1>&2
  echo
  echo "dumping log: " 1>&2
  echo
  cat .ci/pre.log
  exit 1
}

info()
{
  echo "info: $1" 1>&2
}

#------------------------------------------------------------------------
# Check environment
#

if [ -z "${MAVEN_CENTRAL_USERNAME}" ]
then
  fatal "MAVEN_CENTRAL_USERNAME is not defined"
fi
if [ -z "${MAVEN_CENTRAL_PASSWORD}" ]
then
  fatal "MAVEN_CENTRAL_PASSWORD is not defined"
fi
if [ -z "${MAVEN_CENTRAL_STAGING_PROFILE_ID}" ]
then
  fatal "MAVEN_CENTRAL_STAGING_PROFILE_ID is not defined"
fi
if [ -z "${MAVEN_CENTRAL_SIGNING_KEY_ID}" ]
then
  fatal "MAVEN_CENTRAL_SIGNING_KEY_ID is not defined"
fi
if [ -z "${LFA_GITHUB_ACCESS_TOKEN}" ]
then
  fatal "LFA_GITHUB_ACCESS_TOKEN is not defined"
fi
if [ -z "${LFA_BUILDS_SSH_KEY}" ]
then
  fatal "LFA_BUILDS_SSH_KEY not set"
fi
if [ -z "${LFA_KEYSTORE_PASSWORD}" ]
then
  fatal "LFA_KEYSTORE_PASSWORD not set"
fi

#------------------------------------------------------------------------
# Clone credentials repos
#

info "cloning credentials"

git clone \
  --depth 1 \
  "https://${LFA_GITHUB_ACCESS_TOKEN}@github.com/AULFA/credentials" \
  ".ci/credentials" || fatal "could not clone credentials"

#------------------------------------------------------------------------
# Configure SSH

mkdir -p "${HOME}/.ssh" || exit 1
echo "${LFA_BUILDS_SSH_KEY}" | base64 -d > "${HOME}/.ssh/id_ed25519" || exit 1
chmod 700 "${HOME}/.ssh" || exit 1
chmod 600 "${HOME}/.ssh/id_ed25519" || exit 1

(cat <<EOF
[builds.lfa.one]:1022 ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIH/vroEIxH46lW/xg+CmCDwO7FHN24oP+ad4T/OtB/D2
EOF
) >> "$HOME/.ssh/known_hosts" || exit 1

#------------------------------------------------------------------------
# Configure Nexus and keystore

scp -B -P 1022 travis-ci@builds.lfa.one:updater-credentials.xml one.lfa.updater.app/src/main/assets/bundled_credentials.xml || exit 1
scp -B -P 1022 travis-ci@builds.lfa.one:lfa-keystore.jks . || exit 1

(cat <<EOF
au.org.libraryforall.keyAlias=main
au.org.libraryforall.keyPassword=${LFA_KEYSTORE_PASSWORD}
au.org.libraryforall.storePassword=${LFA_KEYSTORE_PASSWORD}
EOF
) >> gradle.properties || exit 1

#------------------------------------------------------------------------
# Import the PGP key for signing Central releases, and try to sign a test
# file to check that the key hasn't expired.
#

info "importing GPG key"
gpg --import ".ci/credentials/android/lfa.asc" || fatal "could not import GPG key"

info "signing test file"
echo "Test" > hello.txt || fatal "could not create test file"
gpg --sign -a hello.txt || fatal "could not produce test signature"

#------------------------------------------------------------------------
# Download Brooklime if necessary.
#

BROOKLIME_URL="https://repo1.maven.org/maven2/com/io7m/brooklime/com.io7m.brooklime.cmdline/0.1.1/com.io7m.brooklime.cmdline-0.1.1-main.jar"
BROOKLIME_SHA256_EXPECTED="efce745f90741aa18c9751c5707580abc123088a8de6d6081fff58f8533f9d8e"

wget -O "brooklime.jar.tmp" "${BROOKLIME_URL}" || fatal "could not download brooklime"
mv "brooklime.jar.tmp" "brooklime.jar" || fatal "could not rename brooklime"

BROOKLIME_SHA256_RECEIVED=$(openssl sha256 "brooklime.jar" | awk '{print $NF}') || fatal "could not checksum brooklime.jar"

if [ "${BROOKLIME_SHA256_EXPECTED}" != "${BROOKLIME_SHA256_RECEIVED}" ]
then
  fatal "brooklime.jar checksum does not match.
  Expected: ${BROOKLIME_SHA256_EXPECTED}
  Received: ${BROOKLIME_SHA256_RECEIVED}"
fi

