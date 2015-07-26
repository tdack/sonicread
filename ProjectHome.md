# SonicRead #

**SonicRead** is a java program that helps you to read the acoustic data sent by a Polar S410, S510 or a S520 heart rate monitor. SonicRead spits out raw data to a file in hsr format. You can use [SportsTracker](http://www.saring.de/sportstracker/index.html) (version 3.6.1 or higher) to view the hsr data and store it in your sporting activities database. [![](http://mac.softpedia.com/base_img/softpedia_free_award_f.gif)](http://mac.softpedia.com/progClean/SonicRead-Clean-65940.html)

## News ##
  * 2009/01/03 Version 2.0 - Released GUI version of SonicRead
  * 2009/08/29 Version 2.1 - More robust SonicLink algorithm

## Credits ##
Thanks to [Tomás Oliveira e Silva](http://www.ieeta.pt/~tos) for his time spent on decoding the Soniclink waveform and the structure of the data. See his [website](http://www.ieeta.pt/~tos/software/polar_s410.html) for the original code.

## Screenshots ##
http://sites.google.com/site/sonicread/Home/Screenshot-SonicRead.png?attredirects=0

## Requirements ##
[Java SE Runtime Environment (JRE) 6 or greater](http://java.sun.com/javase/downloads/index.jsp)
This program should run on any platform supported by Java, but I only tested it on Linux (Ubuntu 8.10).
Of course, you will need a microphone to read the acoustic SonicLink data.

## Installation ##
Use to following link to start SonicRead using java webstart:
[![](http://sites.google.com/site/sonicread/_/rsrc/1230819339226/Home/webstart.png)](http://sites.google.com/site/sonicread/Home/sonicread.jnlp?attredirects=0)

<a href='Hidden comment: 
Or see the [http://code.google.com/p/sonicread/downloads/list downloads] section for a jar.
'></a>

## How to ##

Follow the below stated steps to extract the data from your Polar heart rate monitor..

  * Make sure you are in a quiet environment to prevent glitches in the audio signal.
  * Enable the microphone signal in the Volume Control program of your Operating System.
  * Press the Start button in SonicRead.
  * Hold your watch near the microphone.
  * Select "Connect" in the menu of your Polar.
  * SonicRead should indicate that it started processing SonicLink data, and will update the progress bar.
  * When done, press the Save button and save the hsr file to the location you wish
  * Use [SportsTracker](http://www.saring.de/sportstracker/index.html) to view the data.

## Troubleshooting ##

If SonicRead doesn't recognize the signal, check the microphone volume:
> The dB meter bar should be around 20-30% full when picking up background noise. If you make noise, like for example snapping your finger, the progress bar indicator should rise to 80-90%, and will possible indicate that the audio signal clipped.
> You can play with the distance between your watch and the microphone.

Contact me if you need support.


---


[Remco den Breeje](mailto:stacium@gmail.com)


