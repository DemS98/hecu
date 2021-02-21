# hecu
hecu is a simple Telegram bot written in Java.

Its main function is to reply to a sentence sent by an user with the voice of the HECU marine from Half-Life.
The HECU response is sent through vocal message.

Other two functions have been implemented only for fun purposes:
- binary: the bot will convert a sentence sent by a user in binary string and send it, with a vocal message, to the user
- photo: the bot will show a group of pictures with the topic specified by the user. The user can decide how many picture to display. The user can also display random pictures with *random*, followed by the width and height of the images.

The bot has been developed using:
- [Telegram Bot API](https://github.com/rubenlagus/TelegramBots) Java wrapper by **rubenglaus**
- [emoji-java](https://github.com/vdurmont/emoji-java) by **vdurmont**
- [Gson](https://github.com/google/gson) by **Google**
- [Google Custom Search API](https://developers.google.com/custom-search/v1/overview)
- [jave-2](https://github.com/a-schild/jave2) ffmpeg wrapper by **a-schild**
- [Lorem Picsum](https://picsum.photos) by **David Marby & Nijiko Yonskai**
- [Apache Tika](https://tika.apache.org/)

## Configuration
I will not provide Google API key and bot token so you have to create a new bot with **BotFather**,
create a [Custom Search Engine](https://cse.google.com/cse/all) and get a **Custom Search API key**.
Then create *src/main/resources/api.properties* with these properties

```
# Google Search API properties
google.search.scheme = https
google.search.host = www.googleapis.com
google.search.path = /customsearch/v1
google.search.query = key=[your_api_key]&cx=[your_search_engine_id]&q=:query:&searchType=image&start=:start:

# Picsum API properties
picsum.scheme = https
picsum.host = picsum.photos
picsum.path = /:width:/:height:
```

and *src/main/resources/bot.properties* with these properties

```
# Bot properties
bot.username = [your_bot_username]
bot.token = [your_bot_token]
```

Finally, you need to put all HECU wav sound files in *src/main/resources/words* folder. <br/>
I cannot publish the audio files in the repository because it is Valve property so you have to purchase
Half-Life in order to get them. I recommend purchasing it mostly because it's a great game and you're gonna have a lot of fun playing it.

## Installation

Run `mvn clean package` in the root directory (where the *pom.xml* is located) to build the jar. After that, simply run
`java -jar hecu-1.0.jar` on the built artifact.

## Instructions
After starting the bot (by clicking **START** or sending **/start**), you can see all commands by typing **/** 
or sending **/help**. All commands starts with **/**. <br/>
When in a group, a command must be sent appending **@HecuBot** to the original command, otherwise the bot will not recognize it. <br/>
Moreover, in a group, you must prepend **/** to reply at bot requests (for example, when the bot asks for
the sentence).

## Notes
- HECU vocabulary is limited so, if you send a sentence with a word that's not supported, the bot will print an error
- photo function has a limit of 100 queries per day; this limit is forced by Google so, to increase it, a monthly subscription has to be paid.

## Author
Alessandro Chiariello (Demetrio).
