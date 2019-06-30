updater
===

[![Build Status](https://img.shields.io/travis/AULFA/updater.svg?style=flat-square)](https://travis-ci.org/AULFA/updater)

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
