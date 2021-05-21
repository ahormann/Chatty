import json, math
import subprocess
from typing import Dict, Any, Union

import paho.mqtt.client as mqtt  # pip3 install paho-mqtt
mqtt_client = None

def get_keyboard_from_dict(dict: Dict):
    keyboard = []
    for key in dict.keys():
        keyboard.append('%s:%s' %(key, dict[key]))
    return keyboard



def on_message(client, userdata, msg: mqtt.MQTTMessage):

    command = msg.payload.decode('utf-8')
    answer_topic = "recieve_topic_1"
    print(answer_topic, command)
    answer_msg = None

    # Done
    if command == "/help":
        answer_msg = json.dumps({'text': "Whats up?", 'keyboard': get_keyboard_from_dict({'Nothing': 'optionA', 'This': 'optionB'})})

    # Done
    elif command.startswith('{\"status\":\"position\"') or command.startswith('{\"status\": \"position\"'):
        answer_msg = "It's a location."

        ### Do something with your position here

    # Done
    elif command.startswith('{\"status\":\"favorite\"') or command.startswith('{\"status\": \"favorite\"'):
        answer_msg = "It's a favorite."
        pos = json.loads(command)
        entry = "%s; %s; %s; %s; %s; %s" % (pos["latitude"],pos["longitude"], pos["altitude"], pos["accuracy"], pos["speed"], pos["comment"])

        ### Do something with your favorite-position here

    else:
        answer_msg = 'Error'

    print("input: %s, \n output: %s"%(command, answer_msg))
    mqtt_client.publish(answer_topic, answer_msg)


def on_connect(client, userdata, flags, rc):
    client.subscribe("send_topic_1")


def setup_mqtt(ip, port):
    global mqtt_client
    mqtt_client = mqtt.Client()
    mqtt_client.on_connect = on_connect
    mqtt_client.on_message = on_message

    mqtt_client.connect(ip, port)
    mqtt_client.loop_forever()


if __name__ == '__main__':
    # SETUP MQTT
    setup_mqtt("broker.hivemq.com", 1883)
