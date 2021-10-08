#!/bin/bash
set -x

[ -e 'external/MINI_Connected_Classic_v1.1.3_(usa_160214_448)_apkpure.com.apk' ] ||
wget --quiet -P external 'https://bimmergestalt.s3.amazonaws.com/aaidrive/external/MINI_Connected_Classic_v1.1.3_(usa_160214_448)_apkpure.com.apk'
[ -e 'external/iHeartRadio_for_Auto_v1.12.2_apkpure.com.apk' ] ||
wget --quiet -P external 'https://bimmergestalt.s3.amazonaws.com/aaidrive/external/iHeartRadio_for_Auto_v1.12.2_apkpure.com.apk'
