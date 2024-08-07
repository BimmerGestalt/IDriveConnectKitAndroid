IDriveConnectionKitAndroid
==========================

[![Build Status](https://img.shields.io/github/workflow/status/BimmerGestalt/IDriveConnectKitAndroid/build.svg)](https://github.com/BimmerGestalt/IDriveConnectKitAndroid/actions?query=workflow%3Abuild)
[![Coverage Status](https://coveralls.io/repos/github/BimmerGestalt/IDriveConnectKitAndroid/badge.svg?branch=main)](https://coveralls.io/github/BimmerGestalt/IDriveConnectKitAndroid?branch=main)
[![Jitpack](https://jitpack.io/v/io.bimmergestalt/IDriveConnectKitAndroid.svg)](https://jitpack.io/#io.bimmergestalt/IDriveConnectKitAndroid)
![MIT Licensed](https://img.shields.io/github/license/BimmerGestalt/IDriveConnectKitAndroid)

As a companion to [IDriveConnectionKit](https://github.com/BimmerGestalt/IDriveConnectKit), this library adds helper utilities for Android apps to connect to the IDrive system.

IDrive Connection Status
------------------------

The official BMW car apps provide a TCP tunnel to the car, and they send an announcement to any interested apps when this connection starts up.
The IDriveConnectionStatus module provides helpers to register and parse this announcement, to manually set the details if they are discovered during probing, and to pass around as part of Dependency Injection. 

Security Access
---------------

The first stage of the car connection involves a security challenge from the car, with the connecting app needing to provide the correct response.
The official BMW car apps can provide this answer, and the SecurityAccess module provides a method to fetch this answer.
As part of this, it discovers what BMW apps are installed, which may be useful for some functionality.

Additionally, the initial cert to log in to the car requires an special cert from the main BMW car app, in addition to the app-specific cert.
SecurityAccess, along with CertMangling, implements this cert combining process.

Car API Apps
------------

BMW Connected, unlike BMW Connected Classic before it and MyBMW afterward, implements an official 3rd-party integration named Car API.
Official 3rd-party apps register to be launched when the car connects, and provide their own cert and resources for Connected to send to the car.
The CarAPI module helps integrate into this system, registering to be launched by Connected and to load app resources from these other apps.

This module is now deprecated, since all the official 3rd-party car integrations have removed support and BMW Connected has shut down.