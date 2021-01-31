package com.demetrio.hecu;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.demetrio.hecu.exception.WordNotFoundException;
import com.demetrio.hecu.util.Binary;
import com.demetrio.hecu.util.PhotoStream;
import com.demetrio.hecu.util.Request;
import com.demetrio.hecu.util.Request.Type;
import com.vdurmont.emoji.EmojiParser;

import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Class that realizes the Telegram bot. It is a {@link TelegramLongPollingBot TelegramLongPollingBot}. <br/>
 * It has a {@link Bot#Bot() Default Constructor} that initializes the objects needed by the bot. <br/>
 * It overrides three methods:
 * <ol>
 *     <li>{@link Bot#getBotUsername() getBotUsername()} for getting the bot username. It is taken from
 *          <i>/resources/bot.properties</i> file by a {@link Properties Properties} object.</li>
 *     <li>{@link Bot#getBotToken()} getBotToken()} for getting the bot token. It is taken with the same
 *          procedure of {@link Bot#getBotUsername() getBotUsername()}.</li>
 *     <li>{@link Bot#onUpdateReceived(Update) onUpdateReceived(Update)} for responding to updates, which in this
 *          case are text messages sent by an user. This method incorporates the bot logic.</li>
 * </ol>
 * @author Alessandro Chiariello (Demetrio)
 * @version 1.0
 * @see org.telegram.telegrambots.bots.TelegramLongPollingBot TelegramLongPollingBot
 */
public class Bot extends TelegramLongPollingBot {

    // the message templates properties file path
	private static final String MESSAGE_PROPS_PATH = "/message_template.properties";

	// the bot properties file path
	private static final String BOT_PROPS_PATH = "/bot.properties";

	// a logger
    private static final Logger LOGGER = Logger.getLogger(Bot.class.getName());

    /* the limit of the photo group relative to a photo request.
     * Telegram can send a limit of 10 photos per group so that's the reason the limit is set to 10.
     * If an user requests more than 10 photos, the bot will send an error message.
     * This limit will be changed if Telegram will change it */
    private static final int PHOTO_GROUP_LIMIT = 10;

    /* the default size (both width and height) of a photo or group of photos requested with a
     * random photo request */
    private static final int RANDOM_PHOTO_DEFAULT_SIZE = 800;

    // max photo requests (not random) in a day. Limit enforced by Google Custom Search API
    private static final int MAX_PHOTO_REQUESTS = 100;

    /* the Map of the users requests in private or group chats.
     * The key is the chat identifier, the value is a Set of user requests. */
    private final Map<Long, Set<Request>> requests;

    // the message template properties
    private final Properties messageProps;

    // the bot properties
    private final Properties botProps;

    /* the number of photo requests on this day.
     * It is atomic because the bot is concurrent; this variable has to be consistent so that
     * the photo request limit (not random) is not exceeded because of consistency errors.
     * If one or more day have passed, it's set to 0 */
    private final AtomicInteger photoRequests;

    /* the current date. Initialized on construction as now, it's set to the current day
     * if one or more day have passed (in conjunction with photoRequests). */
    private LocalDate today;

    /**
     * Constructor that initializes the objects needed by the bot.
     * @author Alessandro Chiariello (Demetrio)
     */
    public Bot() 
    {
        LOGGER.info("HecuBot started");
        // initialize the requests variable with a ConcurrentHashMap, needed for consistency
        requests = new ConcurrentHashMap<>();
        // initialize with current date (yyyy-MM-dd)
        today = LocalDate.now();
        // initialize the photoRequests counter
        photoRequests = new AtomicInteger(0);

        /* read the message template and the bot properties from the respective files.
         * If an error occurs, it is logged. */
        messageProps = new Properties();
        botProps = new Properties();
        try (InputStream input1 = Bot.class.getResourceAsStream(MESSAGE_PROPS_PATH);
        	 InputStream input2 = Bot.class.getResourceAsStream(BOT_PROPS_PATH))
        {
            messageProps.load(input1);
            botProps.load(input2);
        } 
        catch (IOException e) 
        {
            LOGGER.log(Level.SEVERE, "Error in loading properties", e);
        }
    }

    /**
     * Returns the bot username, taken from the bot properties.
     * @return the bot username
     * @author Alessandro Chiariello (Demetrio)
     */
    @Override
    public String getBotUsername() 
    {
        return botProps.getProperty("bot.username");
    }

    /**
     * Method for responding to updates, in this case the text messages sent by the user. <br/>
     * It incorporates the bot logic.
     * @param update the update, that is the text message sent by the user
     * @author Alessandro Chiariello (Demetrio)
     */
    @Override
    public void onUpdateReceived(Update update) 
    {
        // if it's a text message
        if (update.hasMessage() && update.getMessage().hasText()) 
        {
            Message message = update.getMessage();
            // ---------------------- Request messages -------------------------------
            // The bot checks if a request in a group has the bot reference (@HecuBot)
            // and if the user has not already sent a request to the bot

            // bot starting
            if (((!message.isGroupMessage() && message.getText().equals("/start"))
                    || message.getText().equals("/start@HecuBot"))
            		&& isUserNotInRequest(message.getFrom().getId(), message.getChatId()))
            {
                // if the bot was not started in this chat
                if (requests.get(message.getChatId()) == null)
                {
                    // add this chat for accepting bot requests
                    requests.put(message.getChatId(), new HashSet<>());
                    LOGGER.log(Level.INFO, "Bot activated in chat {0,number,#}\nBot activation recap: {1}",
                            new Object[]{ message.getChatId(), requests.keySet() });

                    // bot typing
                    SendChatAction typing = new SendChatAction(message.getChatId(),ActionType.TYPING.toString());
                    try 
                    {
                        execute(typing);

                        // sends a started confirm message
                        SendMessage sendMessage = new SendMessage(message.getChatId(),messageProps.getProperty("hecu.hi"))
                                .setReplyToMessageId(message.getMessageId());
                        execute(sendMessage);
                    } 
                    catch (TelegramApiException e) 
                    {
                        LOGGER.log(Level.SEVERE, "Error in sending \"hi\" message", e);
                    }
                }
            }
            // bot stopping
            else if (((!message.isGroupMessage() && message.getText().equals("/stop"))
                    || message.getText().equals("/stop@HecuBot"))
                    && isUserNotInRequest(message.getFrom().getId(), message.getChatId()))
            {
                // if the bot was started in this chat
                if (requests.get(message.getChatId()) != null)
                {
                    // remove the chat from the requests Map
                    requests.remove(message.getChatId());

                    LOGGER.log(Level.INFO, "Bot removed from chat {0,number,#}\nBot activation recap: {1}",
                            new Object[]{ message.getChatId(), requests.keySet() });
                    SendChatAction typing = new SendChatAction(message.getChatId(),ActionType.TYPING.toString());
                    try 
                    {
                        execute(typing);

                        // send a stopped confirm message
                        SendMessage sendMessage = new SendMessage(message.getChatId(),messageProps.getProperty("hecu.bye"))
                            .setReplyToMessageId(message.getMessageId());
                        execute(sendMessage);
                    } 
                    catch (TelegramApiException e) 
                    {
                        LOGGER.log(Level.SEVERE, "Error in sending \"bye\" message", e);
                    }
                }
            }

            // list request
            else if (((!message.isGroupMessage() && message.getText().equals("/list"))
                    || message.getText().equals("/list@HecuBot"))
                    && isUserNotInRequest(message.getFrom().getId(), message.getChatId()))
            {
            	if (requests.get(message.getChatId()) != null)
            	{
            		SendChatAction typing = new SendChatAction(message.getChatId(),ActionType.TYPING.toString());
	                try 
	                {
	                    execute(typing);

	                    // send the words list, sorted through Stream, to the user.
                        // EmojiParser used for parsing emoji string code (like :us:) to Unicode
	                    SendMessage sendMessage = new SendMessage(message.getChatId(),
	                    		EmojiParser.parseToUnicode(messageProps.getProperty("hecu.list") + "\n" + 
	                    				getWordList(Hecu.getInstance().getWords().stream()
	                                    		.sorted(String::compareToIgnoreCase).collect(Collectors.toList()))))
	                                .setReplyToMessageId(message.getMessageId());
	                    execute(sendMessage);
	                } 
	                catch (TelegramApiException e) 
	                {
                        LOGGER.log(Level.SEVERE, "Error in sending \"list\" message", e);
	                }
            	}
            }
            // say request
            else if (((!message.isGroupMessage() && message.getText().equals("/say"))
                    || message.getText().equals("/say@HecuBot"))
                    && isUserNotInRequest(message.getFrom().getId(), message.getChatId()))
            {
                Set<Request> set = requests.get(message.getChatId());
                // if the bot was started in this chat
                if (set != null) 
                {
                	Integer userId = message.getFrom().getId();
                    Request request = new Request(userId, Type.SAY);

                    // it's a two-step request so it is added to the request Set
                    set.add(request);
                    LOGGER.log(Level.INFO, "New \"say\" request in chat {0,number,#}\nRequest recap: {1}",
                            new Object[]{ message.getChatId(), set });
                    SendChatAction typing = new SendChatAction(message.getChatId(), ActionType.TYPING.toString());
                    try 
                    {
                        execute(typing);

                        // send the say request ask message
                        SendMessage sendMessage = new SendMessage(message.getChatId(),
                                EmojiParser.parseToUnicode(messageProps.getProperty("hecu.say")))
                                    .setReplyToMessageId(message.getMessageId());
                        execute(sendMessage);
                    } 
                    catch (TelegramApiException e) 
                    {
                        LOGGER.log(Level.SEVERE, "Error in sending \"say\" message", e);
                    }
                }
            }
            // help request (no need of bot starting)
            else if (((!message.isGroupMessage() && message.getText().equals("/help"))
                    || message.getText().equals("/help@HecuBot"))
                    && isUserNotInRequest(message.getFrom().getId(), message.getChatId()))
            {
                SendChatAction typing = new SendChatAction(message.getChatId(), ActionType.TYPING.toString());
                try 
                {
                    execute(typing);
                    SendMessage sendMessage = new SendMessage(message.getChatId(), messageProps.getProperty("hecu.help").replaceFirst("\\$",PHOTO_GROUP_LIMIT+""))
                            .setReplyToMessageId(message.getMessageId());
                    execute(sendMessage);
                } 
                catch (TelegramApiException e) 
                {
                    LOGGER.log(Level.SEVERE, "Error in sending \"help\" message", e);
                }
            }
            // binary request
            else if (((!message.isGroupMessage() && message.getText().equals("/binary"))
                    || message.getText().equals("/binary@HecuBot"))
                    && isUserNotInRequest(message.getFrom().getId(), message.getChatId()))
            {
                Set<Request> set = requests.get(message.getChatId());
                if (set != null) 
                {
                	Integer userId = message.getFrom().getId();
                    Request request = new Request(userId, Type.BINARY);

                    // added because of the two-step request thing
                    set.add(request);
                    LOGGER.log(Level.INFO, "New \"binary\" request in chat {0,number,#}\nRequest recap: {1}",
                            new Object[]{ message.getChatId(), set });
                    SendChatAction typing = new SendChatAction(message.getChatId(), ActionType.TYPING.toString());
                    try 
                    {
                        execute(typing);

                        // send the binary request ask message
                        SendMessage sendMessage = new SendMessage(message.getChatId(),
                                EmojiParser.parseToUnicode(messageProps.getProperty("hecu.binary")))
                                        .setReplyToMessageId(message.getMessageId());
                        execute(sendMessage);
                    } 
                    catch (TelegramApiException e) 
                    {
                        LOGGER.log(Level.SEVERE, "Error in sending \"binary\" message", e);
                    }
                }
            }
            // photo request
            else if (((!message.isGroupMessage() && message.getText().equals("/photo"))
                    || message.getText().equals("/photo@HecuBot"))
                    && isUserNotInRequest(message.getFrom().getId(), message.getChatId()))
            {
                Set<Request> set = requests.get(message.getChatId());
                if (set != null) 
                {
                	Integer userId = message.getFrom().getId();
                    Request request = new Request(userId, Type.PHOTO);

                    // added because of the two-step request thing
                    set.add(request);
                    LOGGER.log(Level.INFO, "New \"photo\" request in chat {0,number,#}\nRequest recap: {1}",
                            new Object[]{ message.getChatId(), set });
                    SendChatAction typing = new SendChatAction(message.getChatId(), ActionType.TYPING.toString());
                    try 
                    {
                        execute(typing);
                        SendMessage sendMessage = new SendMessage(message.getChatId(),
                                EmojiParser.parseToUnicode(messageProps.getProperty("hecu.photo").replace("$",PHOTO_GROUP_LIMIT+"")))
                        		.setParseMode(ParseMode.HTML).setReplyToMessageId(message.getMessageId());
                        execute(sendMessage);
                    } 
                    catch (TelegramApiException e) 
                    {
                        LOGGER.log(Level.SEVERE, "Error in sending \"photo\" message", e);
                    }
                }
            }
            // ---------------------- Request messages -------------------------------

            // ---------------------- Two-step messages ------------------------------
            // User object request and response by bot
            // The bot checks if the user has done the first step (request) and return to him/her
            // a result based on the user message (I call this object request)
            else 
            {
                Set<Request> set = requests.get(message.getChatId());
                // bot consumes say request
                // If the user is in a say request, the request is removed from the Set and the loop
                // is entered
                if (set.removeIf(e -> e.getUser().equals(message.getFrom().getId()) && e.getType() == Type.SAY))
                {
                    LOGGER.log(Level.INFO, "\"say\" request consumed in chat {0,number,#}\nRequest recap: {1}",
                            new Object[]{ message.getChatId(), set });

                    // split the sentence on whitespace so that the bot can get the audio of the words
                    // if present, the initial / is removed
                    String[] sentence = (message.getText().charAt(0)=='/' ? message.getText().substring(1) : message.getText())
                            .split("\\s+");
                    SendChatAction recording = new SendChatAction(message.getChatId(),
                            ActionType.RECORDAUDIO.toString());
                    try 
                    {
                        execute(recording);
                        try 
                        {
                            // get the sentence audio
                            AudioInputStream audio = Hecu.getInstance().say(sentence);

                            // if the user has typed something
                            if (audio != null) 
                            {
                                // In order to encode the wav in ogg through ffmpeg Java wrapper,
                                // it's necessary to create two temp files to pass to ffmpeg

                                // save the audio in a temporary wav file
                            	Path inputTemp = createTempWavFile(audio);

                            	// create a temporary ogg file with generated filename
                            	Path outputTemp = Files.createTempFile(genFilename(),".ogg");

                                // set the ffmpeg attributes for ogg conversion
                            	AudioAttributes attr = new AudioAttributes()
                            			.setCodec("libopus");
                            	EncodingAttributes encAttr = new EncodingAttributes()
                            			.setInputFormat("wav")
                            			.setOutputFormat("ogg")
                            			.setAudioAttributes(attr);

                            	// encode the wav in ogg (from temp wav file to temp ogg file)
                            	new Encoder().encode(new MultimediaObject(inputTemp.toFile()), outputTemp.toFile(), encAttr);

                            	// send the audio as voice
                                SendVoice voice = new SendVoice().setChatId(message.getChatId())
                                        .setReplyToMessageId(message.getMessageId()).setVoice(outputTemp.toFile());
                                execute(voice);

                                // delete the temporary files
                                if (!Files.deleteIfExists(inputTemp) || !Files.deleteIfExists(outputTemp))
                                    LOGGER.warning("\"say\" request: One of both temporary files not deleted\nThey will be deleted on shutdown");
                            }
                        }
                        // if some word was not found
                        catch (WordNotFoundException e) 
                        {
                            SendChatAction typing = new SendChatAction(message.getChatId(),
                                    ActionType.TYPING.toString());
                            execute(typing);
                            SendMessage sendMessage = new SendMessage(message.getChatId(),
                                    e.getMessage()).setReplyToMessageId(message.getMessageId());
                            execute(sendMessage);
                        }
                    } 
                    catch (TelegramApiException | IOException | EncoderException | UnsupportedAudioFileException e)
                    {
                        LOGGER.log(Level.SEVERE, "Error in processing \"say\" request", e);
                    }
                }
                // bot consumes binary request
                else if (set.removeIf(e -> e.getUser().equals(message.getFrom().getId()) && e.getType() == Type.BINARY))
                {
                    LOGGER.log(Level.INFO, "\"binary\" request consumed in chat {0,number,#}\nRequest recap: {1}",
                            new Object[]{ message.getChatId(), set });

                    SendChatAction recording = new SendChatAction(message.getChatId(),
                            ActionType.RECORDAUDIO.toString());
                    try 
                    {
                        execute(recording);

                        // get the binary object containing the binary audio and String
                        Binary binary = Hecu.getInstance().sayBinary(message.getText().charAt(0)=='/' ? message.getText().substring(1)
                                : message.getText());
                        if (binary != null) {
                            // same encoding procedure of say response
                            Path inputTemp = createTempWavFile(binary.getAudio());
                            Path outputTemp = Files.createTempFile(genFilename(), ".ogg");
                            AudioAttributes attr = new AudioAttributes()
                                    .setCodec("libopus");
                            EncodingAttributes encAttr = new EncodingAttributes()
                                    .setInputFormat("wav")
                                    .setOutputFormat("ogg")
                                    .setAudioAttributes(attr);
                            new Encoder().encode(new MultimediaObject(inputTemp.toFile()), outputTemp.toFile(), encAttr);
                            SendVoice voice = new SendVoice().setChatId(message.getChatId())
                                    .setReplyToMessageId(message.getMessageId()).setVoice(outputTemp.toFile());
                            execute(voice);
                            if (!Files.deleteIfExists(inputTemp) || !Files.deleteIfExists(outputTemp))
                                LOGGER.warning("\"say\" request: One of both temporary files not deleted\nThey will be deleted on shutdown");
                            SendChatAction typing = new SendChatAction(message.getChatId(),
                                    ActionType.TYPING.toString());
                            execute(typing);
                            SendMessage sendMessage = new SendMessage(message.getChatId(), binary.getValue())
                                    .setReplyToMessageId(message.getMessageId());
                            execute(sendMessage);
                        }
                        // if binary string is too large for audio conversion
                        else {
                            SendChatAction typing = new SendChatAction(message.getChatId(),
                                    ActionType.TYPING.toString());
                            execute(typing);
                            SendMessage sendMessage = new SendMessage(message.getChatId(),
                                    "String too large for binary request").setReplyToMessageId(message.getMessageId());
                            execute(sendMessage);
                        }
                    } 
                    catch (TelegramApiException | IOException | UnsupportedAudioFileException | EncoderException e)
                    {
                        LOGGER.log(Level.SEVERE, "Error in processing \"binary\" request", e);
                    }
                }
                // bot consumes photo request
                else if (set.removeIf(e -> e.getUser().equals(message.getFrom().getId()) && e.getType() == Type.PHOTO))
                {
                    LOGGER.log(Level.INFO, "\"photo\" request consumed in chat {0,number,#}\nRequest recap: {1}",
                            new Object[]{ message.getChatId(), set });

                	String query = message.getText().charAt(0)=='/' ? message.getText().substring(1) : message.getText();

                	// the query limit point
            		int index = query.lastIndexOf("//");

            		// the number of photos requested
            		int photoNumber;

            		try
                    {
                        // if photo number not present in message, get the half of the photo group limit
                        if (index==-1)
                            photoNumber = PHOTO_GROUP_LIMIT/2;
                        else
                        {
                            // parse photo number and query
                            photoNumber = Integer.parseInt(query.substring(index+2));
                            query = query.substring(0,index);
                        }

                        // if photo number in range [1,n] (now n = 10)
                        if (photoNumber>=1 && photoNumber<=PHOTO_GROUP_LIMIT)
                        {
                            SendChatAction sending = new SendChatAction(message.getChatId(),ActionType.UPLOADPHOTO.toString());

                            // save upload photo action as Runnable
                            // In this way, we can execute it anywhere so that the user is reassured that the bot is
                            // working on the response
                            Runnable sendPhoto = () -> {
                                try
                                {
                                    execute(sending);
                                } catch (TelegramApiException e)
                                {
                                    LOGGER.log(Level.SEVERE, "Error in processing \"photo\" request", e);
                                }
                            };
                            List<PhotoStream> streams = null;

                            // if the query specify a random photo request
                            if (query.toLowerCase().startsWith("random"))
                            {
                                int width, height;

                                // width start
                                int start = query.indexOf('-');

                                // if width is specified (we go to the first width character eg: -23 -> 2)
                                if (start++ != -1)
                                {
                                    // height start
                                    int end = query.lastIndexOf('-');

                                    // if height is specified
                                    if (start < end)
                                    {
                                        // parse width and height
                                        width = Integer.parseInt(query.substring(start, end));
                                        height = Integer.parseInt(query.substring(end + 1));
                                    }
                                    else
                                    {
                                        // parse width and set height as width
                                        width = Integer.parseInt(query.substring(start));
                                        height = width;
                                    }
                                }
                                else
                                    // use default random photo size
                                    width = height = RANDOM_PHOTO_DEFAULT_SIZE;

                                // get random photos. sendPhoto is called repeatedly in getRandom()
                                streams = Hecu.getInstance().getRandom(width, height, photoNumber, sendPhoto);
                            }
                            else
                            {
                                // if one day as passed since the last photo (not random) request
                                if (ChronoUnit.DAYS.between(today, LocalDate.now()) > 0)
                                {
                                    LOGGER.log(Level.INFO,"New day! Photo request counter with value {0,number,#} set to 0", photoRequests.get());
                                    photoRequests.set(0);
                                    // get the actual date
                                    today = LocalDate.now();
                                }

                                // if photo request limit is not exceeded
                                if (photoRequests.get() < MAX_PHOTO_REQUESTS)
                                {
                                    // get the photo based on the user specified query
                                    // sendPhoto is called repeatedly on getPhotos()
                                    streams = Hecu.getInstance().getPhotos(query, photoNumber, sendPhoto);
                                    photoRequests.incrementAndGet();
                                }
                            }

                            // if photo request limit was not exceeded
                            if (streams != null)
                            {
                                // if more than one photo was requested
                                if (photoNumber > 1)
                                {
                                    execute(sending);
                                    @SuppressWarnings("rawtypes")
                                    List<InputMedia> group = new ArrayList<>();
                                    // add retrieved photos to a InputMediaGroup
                                    streams.forEach(e -> group.add(new InputMediaPhoto().setMedia(e.getInput(), e.getName())));
                                    SendMediaGroup mediaGroup = new SendMediaGroup(message.getChatId(),
                                            group).setReplyToMessageId(message.getMessageId());
                                    execute(mediaGroup);
                                }
                                else
                                {
                                    SendPhoto photo = new SendPhoto().setChatId(message.getChatId())
                                            .setPhoto(streams.get(0).getName(),streams.get(0).getInput())
                                            .setReplyToMessageId(message.getMessageId());
                                    execute(photo);
                                }

                                // close the streams
                                streams.forEach(e -> {
                                    try
                                    {
                                        e.getInput().close();
                                    } catch (IOException e1)
                                    {
                                        LOGGER.log(Level.SEVERE,"Error in closing streams in \"photo\" request", e1);
                                    }
                                });
                            }
                            // send photo exceeded error response
                            else
                            {
                                SendChatAction typing = new SendChatAction(message.getChatId(),
                                        ActionType.TYPING.toString());
                                execute(typing);
                                SendMessage sendMessage = new SendMessage(message.getChatId(),messageProps.getProperty("hecu.error.photo.exceed"))
                                        .setReplyToMessageId(message.getMessageId());
                                execute(sendMessage);
                            }
                        }
                        // if photo group limit is exceeded
                        else
                        {
                            SendChatAction typing = new SendChatAction(message.getChatId(),
                                    ActionType.TYPING.toString());
                            execute(typing);
                            SendMessage sendMessage = new SendMessage(message.getChatId(),messageProps.getProperty("hecu.error.photo.limit").replaceFirst("\\$",PHOTO_GROUP_LIMIT+""))
                                    .setReplyToMessageId(message.getMessageId());
                            execute(sendMessage);
                        }
                    }
                    // if number in query cannot be parsed
                    catch (NumberFormatException e)
                    {
                        try
                        {
                            SendChatAction typing = new SendChatAction(message.getChatId(),
                                    ActionType.TYPING.toString());
                            execute(typing);
                            SendMessage sendMessage = new SendMessage(message.getChatId(),messageProps.getProperty("hecu.error.photo.malformed").replace("$",PHOTO_GROUP_LIMIT+""))
                                    .setParseMode(ParseMode.HTML).setReplyToMessageId(message.getMessageId());
                            execute(sendMessage);
                        }
                        catch (TelegramApiException e1)
                        {
                            LOGGER.log(Level.SEVERE,"Error in processing query error response to \"photo\" request", e1);
                        }
                    }
                    catch (TelegramApiException | MalformedURLException | URISyntaxException e)
                    {
                        LOGGER.log(Level.SEVERE, "Error in processing \"photo\" request", e);
                    }
                }
            }
        }
    }

    /**
     * Return the bot token, taken from the bot properties.
     * @return the bot token
     * @author Alessandro Chiariello (Demetrio)
     */
    @Override
    public String getBotToken()
    {
        return botProps.getProperty("bot.token");
    }

    // generates a 10 char filename
    private String genFilename()
    {
    	Random random = new Random();
    	String charset = "abcdefghilmnopqrstuvzABCDEFGHILMNOPQRSTUVZ0123456789";
    	StringBuilder sb = new StringBuilder();
    	for(int i=0;i<10;i++)
    		sb.append(charset.charAt(random.nextInt(charset.length())));
    	return sb.toString();
    }

    // create a temp wav file from the given audio stream
    // Name is generated through genFilename()
    private Path createTempWavFile(AudioInputStream audio) throws IOException 
    {
    	Path temp = Files.createTempFile(genFilename(), ".wav");
        AudioSystem.write(audio, javax.sound.sampled.AudioFileFormat.Type.WAVE, temp.toFile());
        return temp;
    }

    // check if user is not in a request
    private boolean isUserNotInRequest(Integer userId, Long chatId)
    {
        Set<Request> set = requests.get(chatId);
        
        if (set==null)
        	return true;
        
        return set.stream().noneMatch(e -> e.getUser().equals(userId));
    }

    // get the HECU word list
    private String getWordList(List<String> words)
    {
    	StringBuilder sb = new StringBuilder();
    	int n = words.size();
    	int rows = (int) Math.ceil(n / 5.0);
    	
    	for(int r=0, k=0;r < rows; r++, k+=5)
    	{
    		for(int i=k; i < k + 5 && i < n; i++)
    		{
    			String word = words.get(i);
    			if (word.equals("_comma"))
    				word = ",";
    			else
    			if (word.equals("_period"))
    				word = ".";
    			sb.append(word);
    			if (i+1 < k+5 && i + 1 < n)
    				sb.append("    ");
    		}
    		sb.append('\n');
    	}
		
		return sb.toString().trim();
    }
}