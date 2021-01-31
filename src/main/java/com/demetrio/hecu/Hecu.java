package com.demetrio.hecu;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;

import com.demetrio.hecu.exception.WordNotFoundException;
import com.demetrio.hecu.util.Binary;
import com.demetrio.hecu.util.PhotoStream;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Singleton class that provides the bot functionalities.
 * It has a private constructor and exposes these methods:
 * <ul>
 *     <li>{@link Hecu#getInstance() getInstance()} for getting the class instance</li>
 *     <li>{@link Hecu#say(String...) say(String...)} for <i>say</i> functionality</li>
 *     <li>{@link Hecu#sayBinary(String) sayBinary(String)} for <i>binary</i> functionality</li>
 *     <li>{@link Hecu#getPhotos(String, int, Runnable) getPhotos(String, int, Runnable)} for <i>photo</i> functionality</li>
 *     <li>{@link Hecu#getRandom(int, int, int, Runnable) getRandom(int, int, int, Runnable)} for random <i>photo</i> functionality</li>
 *     <li>{@link Hecu#getWords() getWords()} for <i>list</i> functionality</li>
 * </ul>
 * @author Alessandro Chiariello (Demetrio)
 * @version 1.0
 */
public class Hecu {

    // logger
    private static final Logger LOGGER = Logger.getLogger(Hecu.class.getName());

    // instance
    private static final Hecu INSTANCE = new Hecu();

    // word directory path
    private static final String WORD_DIR_PATH = "/words";

    // api properties file path
    private static final String API_PROPS_PATH = "/api.properties";

    // User Agent string
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20100101 Firefox/79.0";

    // Google Custom Search API start point limit
    private static final int MAX_START = 90;

    // binary String length limit
    private static final int BINARY_LENGTH_LIMIT = 2500;

    // Apache Tika configuration
    private static final TikaConfig TIKA_CONFIG = TikaConfig.getDefaultConfig();

    // words audio Map
    private final Map<String, byte[]> words;

    // api properties
    private final Properties props;

    /**
     * Enum representing a symbol:
     * <ul>
     *     <li>comma (',')</li>
     *     <li>period ('.')</li>
     * </ul>
     * @author Alessandro Chiariello (Demetrio)
     * @version 1.0
     */
    private enum Symbol {
        COMMA, 
        PERIOD
    }

    /**
     * Constructor called only once, eagerly. <br/>
     * It stores in a {@link java.util.Map Map} the audio files, where:
     * <ul>
     *     <li>key is the word</li>
     *     <li>value is a byte array representing the audio file</li>
     * </ul>
     * It also loads the API properties, relative to Google Custom Search API and Picsum
     * @author Alessandro Chiariello (Demetrio)
     */
    private Hecu() {
        words = new HashMap<>();
        props = new Properties();
        // get and finally close the InputStream of api.properties
        try (InputStream input = Hecu.class.getResourceAsStream(API_PROPS_PATH)) 
        {
            // load the API props
            props.load(input);

            // get the audio files directory as URI
            URI wordsURI = Hecu.class.getResource(WORD_DIR_PATH).toURI();

            // if the URI scheme is relative to a jar file (so the software is packaged), it creates a new FileSystem pointing
            // to the directory; otherwise it doesn't do anything
            // The filesystem is not used but it must be created so I can read the directory in the jar
            try(FileSystem fileSystem = (wordsURI.getScheme().equals("jar") ? FileSystems.newFileSystem(wordsURI, Collections.emptyMap()) : null))
            {
                // Walks the audio files directory
                // This is the only case I know where there's need to close a Stream
                // That's because it is relative to a opened system resource (the directory)
            	try (Stream<Path> paths = Files.walk(Paths.get(wordsURI)))
                {
                    // Filters out the directories (in this case, only the parent directory)
                    // Each file is processed
                	paths.filter(Files::isRegularFile).forEach(e -> {
                		String filename = e.getFileName().toString();
                		// get the question mark position in the audio filename
                        // If a word audio name has a question mark, it is in caps lock (heavy! -> HEAVY)
                		int mark = filename.indexOf('!');

                		// if there's a question mark in the filename, the word name is converted to UPPERCASE
                        // (without question mark). In each case, the file extension is removed.
                        // e.g. ass.wav -> ass, ass!.wav -> ASS
                        String name = mark != -1 ? filename.substring(0, mark).toUpperCase()
                                : filename.substring(0, filename.indexOf('.'));
						try
						{
						    // put in the words Map a pair: k = word, v = audio bytes
                            // It uses the Files class to read all the bytes from the Path
                            // representing the audio file.
							words.put(name, Files.readAllBytes(e));
						}
						// if a word was not read correctly
						catch (IOException e1)
						{
							LOGGER.log(Level.SEVERE, "Error loading word audio file in memory", e1);
						}
                	});
                }
            }
        }
        // if props were not loaded correctly or the URI syntax is wrong
        catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Error loading properties or URI syntax not correct", e);
        }
    }

    /**
     * Get the Hecu singleton instance
     * @return the Hecu instance
     * @author Alessandro Chiariello (Demetrio)
     */
    public static Hecu getInstance() {
        return INSTANCE;
    }

    /**
     * Get the audio stream from a variable array of words. <br/>
     * The method extract from the audio {@link java.util.Map Map} each word and
     * concatenate them in a single {@link AudioInputStream AudioInputStream}, representing the sentence. <br/>
     * It throws a {@link WordNotFoundException WordNotFoundException} if one word was not found.
     * @param words a variable array of words, passed as multiple parameters
     * @return the audio stream of the sentence, {@code null} if {@code words} is {@code null} or empty
     * @throws IOException - if there's an error reading an audio from the map
     * @throws UnsupportedAudioFileException - if an audio is not supported
     * @author Alessandro Chiariello (Demetrio)
     */
    public AudioInputStream say(String... words) throws IOException, UnsupportedAudioFileException {
        // if words is null or empty
        if (words != null && words.length > 0) {
            AudioInputStream audio;
            String word;
            byte[] file;
            // if the first word ends with a comma
            if (words[0].endsWith(",")) {
                // get the word String without the comma
                word = words[0].substring(0, words[0].length() - 1);

                // get the audio byte array from the word
                file = getWordFile(word);

                // if there isn't a file for the word, throws WordNotFoundException
                if (file == null)
                    throw new WordNotFoundException("Word \"" + word + "\" not found");

                // concatenates the word audio and the comma audio
                // It reads the byte array as a ByteArrayInputStream, so it will be treated as a normal InputStream
                // The comma byte array will be also be treated as an InputStream (in the appendSymbol() method)
                audio = appendSymbol(AudioSystem.getAudioInputStream(new ByteArrayInputStream(file)), Symbol.COMMA);

            // if it ends with period
            } else if (words[0].endsWith(".")) {

                // do the same of above
                word = words[0].substring(0, words[0].length() - 1);
                file = getWordFile(word);
                if (file == null)
                    throw new WordNotFoundException("Word \"" + word + "\" not found");
                audio = appendSymbol(AudioSystem.getAudioInputStream(new ByteArrayInputStream(file)), Symbol.PERIOD);
            // otherwise
            } else {
                // get the word normally and do the same described previously
                word = words[0];
                file = getWordFile(word);
                if (file == null)
                    throw new WordNotFoundException("Word \"" + word + "\" not found");
                audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(file));
            }

            // processes every word and do the concatenation
            for (int i = 1; i < words.length; i++) {
                AudioInputStream audio2;
                // with the same method above, get the other word
                if (words[i].endsWith(",")) {
                    word = words[i].substring(0, words[i].length() - 1);
                    file = getWordFile(word);
                    if (file == null)
                        throw new WordNotFoundException("Word \"" + word + "\" not found");
                    audio2 = appendSymbol(AudioSystem.getAudioInputStream(new ByteArrayInputStream(file)), Symbol.COMMA);
                } else if (words[i].endsWith(".")) {
                    word = words[i].substring(0, words[i].length() - 1);
                    file = getWordFile(word);
                    if (file == null)
                        throw new WordNotFoundException("Word \"" + word + "\" not found");
                    audio2 = appendSymbol(AudioSystem.getAudioInputStream(new ByteArrayInputStream(file)), Symbol.PERIOD);
                } else {
                    word = words[i];
                    file = getWordFile(word);
                    if (file == null)
                        throw new WordNotFoundException("Word \"" + word + "\" not found");
                    audio2 = AudioSystem.getAudioInputStream(new ByteArrayInputStream(file));
                }
                // concatenate the previous word with the new word
                // we can use a SequenceInputStream because wav is uncompressed
                audio = new AudioInputStream(new SequenceInputStream(audio, audio2), audio.getFormat(),
                        audio.getFrameLength() + audio2.getFrameLength());
            }
            // get the sentence
            return audio;
        }
        return null;
    }

    /**
     * Get a {@link Binary Binary} object containing the binary audio and string, constructed from a
     * quote passed as parameter.
     * @param quote the quote
     * @return a Binary object constructed from the quote
     * @throws IOException - if there's an error reading an audio from the map
     * @throws UnsupportedAudioFileException - if an audio is not supported
     * @author Alessandro Chiariello (Demetrio)
     */
    public Binary sayBinary(String quote) throws IOException, UnsupportedAudioFileException {
        // convert the quote to binary
        String binary = toBinary(quote);
        // to prevent StackOverflowError
        if (binary.length() <= BINARY_LENGTH_LIMIT) {
            // get the first binary digit as audio
            AudioInputStream audio = binary.charAt(0) == '0' ? AudioSystem.getAudioInputStream(new ByteArrayInputStream(words.get("ZERO")))
                    : AudioSystem.getAudioInputStream(new ByteArrayInputStream(words.get("ONE")));
            // processes the other digits
            for (int i = 1; i < binary.length(); i++) {
                // if it's a space, ignore this character
                if (Character.isWhitespace(binary.charAt(i)))
                    continue;

                // get the digit audio
                AudioInputStream audio2 = binary.charAt(i) == '0' ? AudioSystem.getAudioInputStream(new ByteArrayInputStream(words.get("ZERO")))
                        : AudioSystem.getAudioInputStream(new ByteArrayInputStream(words.get("ONE")));

                // concatenate the previous digit with the new digit
                audio = new AudioInputStream(new SequenceInputStream(audio, audio2), audio.getFormat(),
                        audio.getFrameLength() + audio2.getFrameLength());
            }
            // return the Binary object with binary string and audio
            return new Binary(binary, audio);
        }
        return null;
    }

    /**
     * Get a {@link List List} of images based on the query search string.<br/>
     * The number of images is determined by {@code limit}.<br/>
     * The images are searched through <i>Google Custom Search API</i>, that returns a JSON with
     * the links to the images. Then an {@link URLConnection URLConnection} to each link is opened and
     * the respective {@link InputStream InputStream} is saved, with a name, in a {@link PhotoStream PhotoStream}
     * object.<br/>
     * If an image has an unsupported extension (like svg), it is discarded.<br/>
     * The method execute constantly the {@link Runnable Runnable} passed as parameter, that execute a
     * {@link org.telegram.telegrambots.meta.api.methods.send.SendChatAction SendChatAction} of type
     * <i>UPLOAD_PHOTO</i>, so that the user is informed that the bot is working on the <i>photo</i> request.
     * @param query the query of the image, passed to <i>Google Custom Search API</i>
     * @param limit the number of images to get
     * @param sendPhoto the @link org.telegram.telegrambots.meta.api.methods.send.SendChatAction SendChatAction} of
     *                  type <i>UPLOAD_PHOTO</i> to constantly run
     * @return a {@link List List} of {@link PhotoStream} containing the {@link InputStream InputStream} and name of
     *      the images
     * @throws URISyntaxException if <i>Google Custom Search API</i> URI is malformed
     * @see PhotoStream PhotoStream
     * @see org.telegram.telegrambots.meta.api.methods.send.SendChatAction SendChatAction
     * @author Alessandro Chiariello (Demetrio)
     */
    public List<PhotoStream> getPhotos(String query,int limit, Runnable sendPhoto) throws URISyntaxException {
    	Random random = new Random();
    	List<PhotoStream> photos = new ArrayList<>();
    	// search start position
    	int start = random.nextInt(MAX_START)+1;

    	// while I didn't get all the photos
        while (photos.size()<limit)
        {
            // run the execute SendChatAction of type UPLOAD_PHOTO
        	sendPhoto.run();

        	// create a new REST API Client
        	Client client = ClientBuilder.newClient();

        	// construct the URI to Google Custom Search API
            // properties are taken from api.properties file
            // google.search.query prop is formatted automatically
            // and query and start part are replaced
			URI uri = new URI(props.getProperty("google.search.scheme"), null, 
					props.getProperty("google.search.host"), -1, 
					props.getProperty("google.search.path"), props.getProperty("google.search.query")
					.replace(":query:",query).replace(":start:",start + ""), null);
			WebTarget target = client.target(uri);

			// through Gson, the JSON result is converted in a JsonObject and the items are get
	    	JsonArray items = new Gson().fromJson(target.request(MediaType.APPLICATION_JSON).get(String.class),JsonObject.class).get("items").getAsJsonArray();

	    	// while some item has not been processed and not all photos has been get
	    	while(items.size()>0 && photos.size()<limit)
	        {
	    		sendPhoto.run();
	    		try
	    		{
	    		    // create an URL from the link of a random item that is removed
	    			URL image = new URL(items.remove(random.nextInt(items.size())).getAsJsonObject().get("link").getAsString());

	    			// open a connection to the image URL
		            URLConnection conn = image.openConnection();

		            // set the User-Agent so the server where the image is stored does not complain
		            conn.setRequestProperty("User-Agent", USER_AGENT);

		            // if the URL is not an HTTP URL, cannot verify the status code so proceed
                    // if the URL is an HTTP URL, verify that the response code is OK (200) and then proceed
		            if (!(conn instanceof HttpURLConnection) || ((HttpURLConnection)conn).getResponseCode()==200)
		            {
                        sendPhoto.run();

                        // wraps the resource InputStream in a BufferedInputStream so it can be passed
                        // to Tika#detect() method for Mime type detection.
                        // BufferedInputStream support reset method so Tika can reset it to the initial
                        // position
		            	BufferedInputStream input = new BufferedInputStream(conn.getInputStream());
						try
						{
						    // detect the Mime type string and get the respective MimeType object from the
                            // Mime repository
							MimeType mime = TIKA_CONFIG.getMimeRepository().forName(new Tika().detect(input));

							// if the Type of the Mime type is an image
							if (mime.getType().getType().equals("image"))
			            	{
			            	    // get the Mime type Subtype
								String subType = mime.getType().getSubtype();

								// Telegram support only jpeg, png and webp for images
                                // Check if the Subtype is supported
								if (subType.equals("jpeg") || subType.equals("png") || subType.equals("webp"))
								{
				            		sendPhoto.run();
			            			PhotoStream photo = new PhotoStream();
			            			// set the image name
				            		photo.setName(query + photos.size() + mime.getExtension());
				            		// set the InputStream
				            		photo.setInput(input);
				            		// add the PhotoStream to the List
				            		photos.add(photo);
								}
			            	}
						}
						// if MimeType was not found in the repository
						catch (MimeTypeException e)
						{
							LOGGER.log(Level.SEVERE, "Error finding MimeType from Mime repository", e);
						}
		            }
	    		}
	    		// if there's some error opening the URL connection or getting its InputStream
	    		catch (IOException e)
	    		{
	    			LOGGER.log(Level.SEVERE, "Error opening URL connection or getting URL InputStream", e);
	    		}
	        }
	    	// go to the next starting point. Resetted if on the last starting point
        	start = (start+10) % (MAX_START+1);
        }
        return photos;
    }

    /**
     * Get a {@link List List} of random images with specified {@code width} and {@code height}.<br/>
     * The number of images is determined by {@code limit}.<br/>
     * The images are get by making a request URL to picsum API, that returns a random image.
     * If an image has an unsupported extension (like svg), it is discarded.<br/>
     * The method execute constantly the {@link Runnable Runnable} passed as parameter, that execute a
     * {@link org.telegram.telegrambots.meta.api.methods.send.SendChatAction SendChatAction} of type
     * <i>UPLOAD_PHOTO</i>, so that the user is informed that the bot is working on the <i>photo random</i> request.
     * @param width the width of the random images
     * @param height the height of the random images
     * @param limit the number of images to get
     * @param sendPhoto he @link org.telegram.telegrambots.meta.api.methods.send.SendChatAction SendChatAction} of
     *          type <i>UPLOAD_PHOTO</i> to constantly run
     * @return a {@link List List} of {@link PhotoStream} containing the {@link InputStream InputStream} and name of the images
     * @throws URISyntaxException if the URI is malformed
     * @throws MalformedURLException if the URL derived by the URI is malformed
     * @see PhotoStream PhotoStream
     * @see org.telegram.telegrambots.meta.api.methods.send.SendChatAction SendChatAction
     * @author Alessandro Chiariello (Demetrio)
     */
    public List<PhotoStream> getRandom(int width, int height, int limit, Runnable sendPhoto) throws URISyntaxException, MalformedURLException
    {
        List<PhotoStream> photos = new ArrayList<>();

        // while I didn't get all the photos
        while (photos.size() < limit)
        {
            sendPhoto.run();

            // construct an URI to the picsum API
            // properties are taken from api.properties file
            URI uri = new URI(props.getProperty("picsum.scheme"), null, props.getProperty("picsum.host"),
                    -1, props.getProperty("picsum.path").replace(":width:",width + "")
                    .replace(":height:",height + ""), null, null);

            // convert the URI to URL
            URL image = uri.toURL();
            try
            {
                // open the connection to the image URL
                URLConnection conn = image.openConnection();

                // set the User-Agent so the server where the image is present does not complain
                conn.setRequestProperty("User-Agent", USER_AGENT);

                // if it alright (like in getPhotos() method)
                if (!(conn instanceof HttpURLConnection) || ((HttpURLConnection)conn).getResponseCode()==200)
                {
                    // do the same procedure as the getPhotos() method
                    // ------------------------------------------------
                    BufferedInputStream input = new BufferedInputStream(conn.getInputStream());
                    try
                    {
                        MimeType mime = TIKA_CONFIG.getMimeRepository().forName(new Tika().detect(input));
                        if (mime.getType().getType().equals("image"))
                        {
                            String subType = mime.getType().getSubtype();
                            if (subType.equals("jpeg") || subType.equals("png") || subType.equals("webp"))
                            {
                                sendPhoto.run();
                                PhotoStream photo = new PhotoStream();
                                photo.setName(photos.size() + mime.getExtension());
                                photo.setInput(input);
                                photos.add(photo);
                            }
                        }
                    } catch (MimeTypeException e)
                    {
                        LOGGER.log(Level.SEVERE, "Error finding MimeType from Mime repository", e);
                    }
                    // ----------------------------------------------------
                }
            }
            catch (IOException e)
            {
                LOGGER.log(Level.SEVERE, "Error opening URL connection or getting URL InputStream", e);
            }
        }
        return photos;
    }

    /**
     * Get HECU words as {@link Set Set}.
     * @return the HECU words
     * @author Alessandro Chiariello (Demetrio)
     */
    public Set<String> getWords() {
        return words.keySet();
    }

    // convert a String to textual binary representation
    private String toBinary(String str) {
        // get string bytes with UTF-8 encoding
        byte[] buf = str.getBytes(StandardCharsets.UTF_8);

        StringBuilder result = new StringBuilder();
        for (byte b : buf) {
            // convert the byte to String binary representation
            String binary = Integer.toBinaryString(b);

            // pad the binary number with leading 0s
            result.append(("00000000" + binary).substring(binary.length()));

            // separate each 8-bit binary number with a space
            result.append(' ');
        }

        // return the result trimming trailing whitespace
        return result.toString().trim();
    }

    // append a comma or period to an audio stream
    private AudioInputStream appendSymbol(AudioInputStream audio, Symbol symbol) throws IOException, UnsupportedAudioFileException {
        AudioInputStream audioSymbol = symbol == Symbol.COMMA
                ? AudioSystem.getAudioInputStream(new ByteArrayInputStream(this.words.get("_comma")))
                : AudioSystem.getAudioInputStream(new ByteArrayInputStream(this.words.get("_period")));
        return new AudioInputStream(new SequenceInputStream(audio, audioSymbol), audio.getFormat(),
                audio.getFrameLength() + audioSymbol.getFrameLength());
    }

    // get an audio byte array from a word
    // null if there's no matching audio for the word
    // it always tries to find the word, even if written incorrectly
    private byte[] getWordFile(String word)
    {
    	byte[] file;
    	// if the word audio is uppercase
    	if (isUpperCase(word)) {
    	    // try to get it
    		file = this.words.get(word);
    		// if it's not in the Map
            if (file == null)
                // try to find its lowercase counterpart
                file = this.words.get(word.toLowerCase());
        }
    	// if it's lowercase or written incorrectly (e.g. HeaVy)
    	else
        {
    	    // try to get the lowercase word
            file = this.words.get(word.toLowerCase());
            // if it's not in the Map
            if (file == null)
                // try to find its uppercase counterpart
                file = this.words.get(word.toUpperCase());
        }
    	return file;
    }

    // check if a word is uppercase
    private boolean isUpperCase(String str) {
        // for each character
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);

            // if the char is a letter and it's not uppercase, the string is not uppercase
            if (Character.isLetter(ch) && !Character.isUpperCase(ch))
                return false;
        }
        // if every letter char is uppercase
        return true;
    }
}