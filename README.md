# MQTT-Chat
This is a repository for an android chat application that uses MQTT as transfer protocol to send messages between users. \
Original Repository:  https://github.com/jawnpaul/Chatty

# Motivation
I wanted an android-app that allows to send and recieve messages to/from a MQTT-Server and to share my location with my own server.
Using this one can for example send simple commands to their smart home.
Additionally one can start the location-sharing to send the current location to the server every 30 seconds or mark favorites.

# Disclaimer
This is a hobby-project and i am not responsible for how you use it or for damage caused by this!\
The app is pre-configured to use a public server. You can use this for testing, but
if you care about your privacy DO NOT SEND PRIVATE DATA WHILE THE APP IS CONFIGURED TO USE A PUBLIC SERVER everyone can read there!\
This holds also for your location-data!\
The data is transferred without encryption. The app is intended for usage with a private server in the same network/VPN.\
You are of course welcome to add encryption yourself.

# Setup
First clone the Repository and setup an environment to edit and compile a gradle-application.\
Tested with Android Studio and Android 10 (API 29).\

Then go to 'Chatty_MQTT_Chat/app/src/main/java/ng/org/knowit/mqttchat'.\
In ChatActivity.java replace 'tcp://broker.hivemq.com:1883' with the ip and port of your server.\
In ChatActivity.java add all commands you will frequently send to commandDict.\
The keyword is the string that will be shown to the user, the value is the string that will be sent to the server.\
In MainActivity.java add the topics you will frequently listen and send to to topicSendingList and topicRecievingList.\

After installation grant the location-permission to the app manually, i could not figure out how to ask the user with a popup.
(Of course only if you need the location-feature).

# Example usage in Backend:
See example_server.py.