updater
===

![updater](./src/site/resources/updater.jpg?raw=true)

The LFA Updater application downloads and installs updates to applications published in
[repositories](#repositories).

## Features

* Install and update applications from self-hosted repositories
* Minimal repository hosting requirements: Consumes static XML and APK files
* High coverage test suite
* Apache 2.0 License

## Repositories

A _repository_ is an XML file conforming to the published [updater xml schema](au.org.libraryforall.updater.repository.xml.v1_0/src/main/resources/au/org/libraryforall/updater/repository/xml/v1_0/schema-1.0.xsd).
The format is designed to be trivial to stream-parse and compact enough to be used over
low-bandwidth networks.

An example repository file:

```
<?xml version="1.0" encoding="UTF-8"?>
<r:repository id="378142a2-23ff-44c9-964f-b621fe45ed0c" self="https://builds.lfa.one/apk/releases.xml" title="LFA Releases" updated="2019-06-30T10:08:11.696843" xmlns:r="urn:au.org.libraryforall.updater.repository.xml:1.0">
  <r:package id="au.org.libraryforall.reader" name="LFA" sha256="b91ff56662958cb212b54082e3de64315dfc728481703b7961546ad8ce591ab0" source="lfa-1.3.7-243-release.apk" versionCode="243" versionName="1.3.7"/>
  <r:package id="au.org.libraryforall.reader" name="LFA" sha256="c383763b04400a21cde5141a4fcdd081cab4783697419d21b4c279919b4c19f7" source="lfa-1.3.7-245-release.apk" versionCode="245" versionName="1.3.7"/>
  <r:package id="au.org.libraryforall.reader" name="LFA" sha256="6bcf80ab0cad8ce698f72a72983f4e7c31a0d4a3fafa24ac09913bf9d7ea5dc6" source="lfa-1.3.7-252-release.apk" versionCode="252" versionName="1.3.7"/>
  <r:package id="au.org.libraryforall.launcher.app" name="LFA Launcher" sha256="0db912f5b0144ef6746fad5d6d231c0edf1e158c2a5cca592d1fc7dfa2c840ef" source="au.org.libraryforall.launcher.app-0.0.3-148-release.apk" versionCode="148" versionName="0.0.3"/>
  <r:package id="au.org.libraryforall.reader.offline" name="LFA Offline" sha256="9be997ddd0affa68736fcc110635ace5033cc0feac3f1d910aa36f5d875b53ac" source="lfa-offline-1.3.7-240-release.apk" versionCode="240" versionName="1.3.7"/>
  <r:package id="au.org.libraryforall.reader.offline" name="LFA Offline" sha256="654d3d1186a3bc006939c6b76869a5da396e40e418464299cc405f7744d77835" source="lfa-offline-1.3.7-247-release.apk" versionCode="247" versionName="1.3.7"/>
  <r:package id="au.org.libraryforall.reader.offline" name="LFA Offline" sha256="144869c316eb0e32eb980546952d82e3bbc53c57c977eb6e9857bdd1a6509e48" source="lfa-offline-1.3.7-280-release.apk" versionCode="280" versionName="1.3.7"/>
  <r:package id="au.org.libraryforall.updater.app" name="LFA Updater" sha256="ce073b979d6941b43cef04b50d1e6dfedffda2565e121b0a9b333b4c03e3a6f2" source="lfa-updater-0.0.3-514-release.apk" versionCode="514" versionName="0.0.3"/>
  <r:package id="au.org.libraryforall.updater.app" name="LFA Updater" sha256="b49d1e4d8c649dcbfd7793044434098d257caceb3316fd6ee555602c161ccd65" source="lfa-updater-0.0.3-516-release.apk" versionCode="516" versionName="0.0.3"/>
</r:repository>
```

Currently, the Updater app supports these methods to package, read and display repositories:
- LFA SD Card: normally used to deliver content together with the Spark Kits sent on the field, or to provide our apps and collections to third-party devices.
- LFA Hotspot: installed on GroundCloud hardware through USB sticks either on the field or while setting up kits to send to the field.
- Testing online repository: used by us (the tech team) or under our supervision to test the latest versions of our apps and collections. This can only be viewed by activating the “testing repositories” from the app’s settings. It can be found at this URL: Index of /repository/testing/

While every type of repository is displayed in the same way, they have different structures and delivery methods under the hood. You can find a tutorial on how to create an Updater repository here: [LFA Updater Repositories - A to Z](<https://stc365.sharepoint.com/:w:/r/sites/Spaces02/Site028/Site007/Documents/Documentation/A-Z Tech Guides/MS Word/LFA Updater Repositories - A to Z.docx?d=w6d894542043143278d5a17c67c48dc05&csf=1&web=1&e=5NYN5c>)

## Setup
1. Clone the repo:
```bash
git clone git@github.com:AULFA/updater.git
```
2. Get the submodules:
```bash
cd updater
git submodule update --init
```
3. Clone the application-secrets directory into the `.ci` directory, naming it `credentials`:
```bash
cd .ci
git clone git@github.com:AULFA/application-secrets.git credentials
```
4. Run the `credentials.sh` script to set up the required credential files for the app:
```bash
cd ..
.ci-local/credentials.sh
```

Done! You should be able to build the project in any of the available variants now.

## How-tos

### Increment the version number
The version of the Updater app is composed by two parts: version name and version code.

#### Version code
The version code of a specific variant of the app is provided by the `version.properties` file of the variant:
```bash
#
#Thu May 19 19:52:30 AEST 2022
versionCode=1487

```
This file is updated automatically every time you (or Android Studio) rebuild the app, so you shouldn't need to update it manually.
If you need to change it, it's better if you rebuild the app and commit the new content of this file. You can change it manually too, just make sure that the new version code is higher than the previous one.

#### Version name
The version name of the app is defined under the `gradle.properties` file of the project:
```bash
VERSION_NAME=0.0.7-SNAPSHOT
```
You can update this by simply incrementing the version in this variable.

### Create a new variant of the Updater
You might need to create a new variant of the app to provide a different translation or list of repositories for a specific program. For example, the Ukraine program has a separate variant of the Updater with a different list of repositories.
If you want to create a new variant of the Updater, you can follow these steps:
1. Duplicate the whole `<project root>/one.lfa.updater.app` directory. Rename it to `one.lfa.updater.<name of the program>`.
2. Update the `settings.gradle` file of the project to include the new package.
3. Locate the `AndroidManifest.xml` file of your variant. Change the package name to be the same as the package directory name (`one.lfa.updater.<name of the program>`).
4. To change the app title, create a `res/values/strings.xml` file for your variant. Set the item `<string name="main_title">YOUR APP TITLE HERE</string>` according to your needs.
5. To change the list of repositories, create or modify the `res/xml/bundled_repositories.xml` file for your variant. The main version of the Updater comes with three predefined repositories (SD Card, GroundCloud, Testing online repository). If you want to add a new repository, bear in mind that its `uuid` value has to be unique.
6. Update the `.ci-local/credentials.sh` file to copy the `bundled_credentials.xml` file to your build variant:
```bash
copy .ci/credentials/updater-credentials.xml one.lfa.updater.<your variant>/src/main/assets/bundled_credentials.xml
```
7. Re-run `.ci-local/credentials.sh`.
8. Update the `.ci-local/deploy-ssh.conf` file of the project to include the new variant.

You should now be able to run, test and release the new variant.

## Release
Once you're done with your changes, you may want to release a new version of the app. To do this, follow these steps:
1. Commit your changes to a new branch and push them to the GitHub repository.
2. Create a pull request detailing the changes you made (features, fixes, new versions...). The PR should point to the `develop` branch.
3. Depending on the changes made and the availability of the rest of the team, ask for a code review.
4. Once everything is ready, squash and merge the pull request.
5. A GitHub Action will start. This will build all the app variants and, if successful, push the generated APK files automatically to the testing repository.
6. You will find the latest version of each variant in the testing repository https://distribution.lfa.one/repository/testing/

