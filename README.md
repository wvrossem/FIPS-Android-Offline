FIPS-Android-Offline
====================

Project Overview
----------------

This repository is part of the project "An Extensible Framework for Indoor Positioning on Mobile Devices", which is the master thesis that I did in 2011-2012 at the Vrije Universiteit Brussel to achieve my "Master in Applied Computer Science". My promotor for this master thesis was [Prof. Dr. Beat Signer](http://www.beatsigner.com/). The thesis document can be found [here](https://www.dropbox.com/s/j0xehv5qodxh3id/Van%20Rossem%20-%202012%20-%20A%20FrameWork%20for%20Indoor%20Positioning%20on%20Mobile%20Devices.pdf).

The entire project is divided into several repositories:

* [FIPS-Datastore](https://github.com/wvrossem/FIPS-Datastore)
* [FIPS-Server](https://github.com/wvrossem/FIPS-Server)
* [FIPS-Tool](https://github.com/wvrossem/FIPS-Tool)
* [FIPS-Android-Offline](https://github.com/wvrossem/FIPS-Android-Offline)
* [FIPS-Android-Online](https://github.com/wvrossem/FIPS-Android-Online)

Android Offline Application Usage
---------------------------------

This is an Android application to collect fingerprints for a WLAN indoor positioning system. The easiest way to use this project is to open it as an Eclipse project and running it from there. The application is dependent on the [FIPS-Datastore](https://github.com/wvrossem/FIPS-Datastore) for the data entities so the easiest way to resolve the dependencies is to link the projects in eclipse (or alternatively put the src files and libraries in the Android project).

The application works as follows: select a grid that denotes your location on the map and a WLAN scan will be initiated. The results from this scan are then saved to the folder `ips_samples` on your device. A menu to configure some settings is also provided.

##### The main interface of the application

![Main interface](https://dl.dropbox.com/s/h2une8tjd9zgsze/android1.png "Main interface")

##### WiFi scan in progress

![WiFi scan](https://dl.dropbox.com/s/lipi3w82y0agrjz/android2.png "WiFi scan")

##### The settings menu

![Settings menu](https://dl.dropbox.com/s/brzmpqrh2p6hrok/android3.png "Settings menu")

License
-------

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.



