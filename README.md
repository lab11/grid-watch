GridWatch
==========

GridWatch is a system for monitoring the state of the power grid using just
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
![android_screenshot_1](https://raw.githubusercontent.com/lab11/grid-watch/master/media/gridwatch_screenshot_1_sm.png)
](https://raw.githubusercontent.com/lab11/grid-watch/master/media/gridwatch_screenshot_1_sm.png)

[
![android_screenshot_2](https://raw.githubusercontent.com/lab11/grid-watch/master/media/gridwatch_screenshot_2_sm.png)
](https://raw.githubusercontent.com/lab11/grid-watch/master/media/gridwatch_screenshot_2_sm.png)

The `.apk` for Android is located in the `/release/android` folder.
