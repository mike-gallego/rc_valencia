import pigpio
import speech_recognition as sr
import time
import os
import sys
import pickle
import pygame
import json
import pyrebase
from gtts import gTTS
from time import ctime


"""
Issue 1: Need to work on making the user variable global, so that when cait says goodbye, She will say the user's name




Future Ideas: Have cait remember categories and values in it by reading and opening files. Professor Suggested Pickle

"""

"""
Firebase Information
"""

config = {"apiKey": "AIzaSyBc7qqxaJiXJ9FlZlDrEqdPoVyA1JM5vlY","authDomain": "learningserver-1abd7.firebaseapp.com","databaseURL": "https://learningserver-1abd7.firebaseio.com","projectId": "learningserver-1abd7","storageBucket": "learningserver-1abd7.appspot.com","messagingSenderId": "658436185485"}
firebase = pyrebase.initialize_app(config)
db = firebase.database()




"""___________________Variables___________________"""

#objects
gpio = pigpio.pi()
db = firebase.database()
#SIGNALS
MAX_SIGNAL = 2000
MIN_SIGNAL = 1000
IDLE_SIGNAL = 1500
OFF_SIGNAL = 0
android_signal = 0



#PINS
LEFT_MOTOR_PIN = 23
RIGHT_MOTOR_PIN = 17


"""____________________FUNCTIONS____________________"""

#ESSENTIALS
def start():
	idle()
	time.sleep(2)
	speak("who is controlling me today?")
	user = recordAudio()
	speak("Hi {}, ready when you are!".format(user))
	start_loop()

def start_loop():
	while 1:
		
		speeds_now = db.child("speeds").get()
		android_signal = speeds_now.val()[-1]

		gpio.set_servo_pulsewidth(LEFT_MOTOR_PIN, float(android_signal))
		gpio.set_servo_pulsewidth(RIGHT_MOTOR_PIN, float(android_signal))	

		if float(android_signal) == 0:
			data = ""
			data = recordAudio()
			cait_commands(data)


def stop():
	disarm()
	gpio.stop()
	sys.exit()





#ROVER_SETUP

def idle():
	gpio.set_servo_pulsewidth(LEFT_MOTOR_PIN, IDLE_SIGNAL)
	gpio.set_servo_pulsewidth(RIGHT_MOTOR_PIN, IDLE_SIGNAL)
	
def disarm():
	gpio.set_servo_pulsewidth(LEFT_MOTOR_PIN, OFF_SIGNAL)
	gpio.set_servo_pulsewidth(RIGHT_MOTOR_PIN, OFF_SIGNAL)

#ROVER_COMMANDS

def voice_command_move(speed, seconds):
	speed = float(speed)
	seconds = float(seconds)
	gpio.set_servo_pulsewidth(LEFT_MOTOR_PIN, speed)
	gpio.set_servo_pulsewidth(RIGHT_MOTOR_PIN, speed)
	time.sleep(seconds)
	idle()
	

#AI_SETUP
	
def speak(audioString):
	print(audioString)
	if audioString != "":
		tts = gTTS(text=audioString, lang="en")
		tts.save("audio.mp3")
		os.system("mpg321 audio.mp3")
	
def recordAudio():
	r = sr.Recognizer()
	with sr.Microphone() as source:
		print("Say something!")
		audio = r.listen(source)
	
	data = ""
	try:
		data = r.recognize_google(audio)
		print("You said: " + data)
	except sr.UnknownValueError:
		print("Google Speech Recognition could not understand audio")
	except sr.RequestError as e:
		print("Could not request results from Google Speech Recognitionservice; {0}".format(e))
	return data	
	
def store_data(list_of_data):
	
	for x in list_of_data:
		if x == "a" and list_of_data[list_of_data.index(x) - 1] == "is":
			value_index = list_of_data.index(x) - 2
			value = list_of_data[value_index]
			category_index = list_of_data.index(x) + 1
			category = list_of_data[category_index]
	
	with open("cait_vocabulary.json", "r") as f:
		s = f.read()
		if s == "":
			s = "{\"key\": [\"value\"]}"
			temp_vocabulary = json.loads(s)
		else:
			temp_vocabulary = json.loads(s)
	with open("cait_vocabulary.json", "w") as f:
		try:
			temp_vocabulary[category].append(value)
		except KeyError:
			temp_vocabulary[category] = [value]
		json.dump(temp_vocabulary, f, indent = 2)
	
def retrieve_data(category):
	with open("cait_vocabulary.json", "r") as f:
		s = f.read()
		temp_vocabulary = json.loads(s)
		if category in temp_vocabulary:
			speak("In my {}'s list are {}".format(category, temp_vocabulary[category]))
		else:
			speak("list of {} is not in my vocabulary".format(category))
	
#AI_COMMANDS

def go(data):
	if data[2] == "at":


		for x in data:
			if x == "activate" and data[data.index(x) + 2] == "at":
				speed_index = data.index(x) + 1
				speed = data[speed_index]
				seconds_index = data.index(x) + 3
				seconds = data[seconds_index]
		speak("Go!")
		voice_command_move(speed, seconds)
		data = ""
		
	elif data[1] < 1000 or data[1] > 2000 or data[1] == 1500:
		speak("My max speed backwards is 1000 servo pulse widths and my max speed forward is 2000 servo pulse widths. My idle is 1500")
		data = ""
	else:
		speak("To make me go, make sure you talk in the format of. go. speed. for. seconds.")
		data = ""
		
def play_tune(file_name):
	
	pygame.mixer.init()
	pygame.mixer.music.load(file_name)
	pygame.mixer.music.play()
	print(file_name)
	while pygame.mixer.music.get_busy() == True:
		continue
	pygame.quit()
	

def cait_commands(data):
	if "tell me about yourself" in data:
		speak("Ok! Well, to start with my name is cait, which stands "
		"for cognitive artificial intelligence technology. I was created by the idea that someday A I will "
		"develop intelligence the way humans do")
	elif "goodbye" in data:
		
		speak("goodbye, I hope to accompany you again")
		stop()
	elif "is a" in data:
		data = data.lower()
		data = data.split(" ")
		speak("Oh really? I didn't know that. Let me store it in my vocabulary")
		store_data(data) # saves and loads a file using the with loop (not working yet)
	elif "tell" in data and "your" in data:
		data = data.lower()
		data = data.split(" ")
		retrieve_category_index = data.index("your") + 1
		retrieve_category = data[retrieve_category_index]
		print(retrieve_category)
		retrieve_data(retrieve_category)
	elif "activate" in data and data[0:1] == 'a':
		# If the user doesn't say the right format, which is go {speed} for {seconds} seconds}
		# then Cait will instruct how to format it
		speak("Okay! activating in three. two. one.")
		data = data.lower()
		data = data.split(" ")
		go(data)
	elif "want" in data and "joke" in data:
		#Here she will collect jokes in her memory
		print("nothing")
	elif "tell" in data and "joke" in data:
		#Here she will tell a random index of the joke list that she knows
		print("nothing")
		
	elif "play" and "song" in data:
		speak("Sure! do you like hip hop?")
		play_tune("still_dre.mp3")

"""____________________EXECUTION____________________"""
start()
