Grid Watch
==========

Grid watch is a system for monitoring the state of the power grid using just
smart phones. Phones running the GridWatch application monitor their status
for power change events (when the phone goes from charging to not charging).
This transition could signal a power outage, but is likely just the user
unplugging his/her phone. To determine which, the phone checks the accelerometer
for motion (signifying the user is picking up the phone), and the microphone
for evidence of a 50/60 Hz signal emanating from the power grid. If neither
signal is detected, a power outage event is transmitted to a server. These
events are aggregated to detect a power outage.


Applications
------------

[
![android_screenshot](https://github.com/lab11/grid-watch/raw/master/media/android_v0.1_screenshot_sm.png)
](https://github.com/lab11/grid-watch/raw/master/media/android_v0.1_screenshot.png)

The `.apk` for Android is located in the `/release/android` folder.
