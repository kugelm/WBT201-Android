WBT201-Android
==============

Simplistic ("One Button") App for Android. This app will read out the stored tracks on a Wintec G-Rays 2 GPS logger (known as WBT201) via Bluetooth.
The app needs at least two Android permissions: Bluetooth (+ Bluetooth Admin) and for storing track data write access to the SD card.

Tracks will be stored in GPX format. If a "osmand" directory is detected, the app will store track data in the subdirectory tracks there in order to allow OSMAnd to show them directly from Layer's menu.

The app reuses source code from the GetWBT J2ME Java project. This is under GPL (c)2008. Many thanks to Dirkjan Krijnders.

This project has been developed using Google Android Studio 0.5.7.

Plans for future versions:
* read out and set configuration parameters on the logger
* alternative track directory (selectable) <-- done in latest version, could be improved
* alternative track file formats
* visualize tracks from app's menu (own viewer, other map apps....)
