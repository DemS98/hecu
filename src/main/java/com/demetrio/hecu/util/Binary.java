package com.demetrio.hecu.util;

import javax.sound.sampled.AudioInputStream;

/**
 * Bean class to contain data for the <b>/binary</b> request. <br />
 * A instance of this class is returned by {@link com.demetrio.hecu.Hecu#sayBinary(String) Hecu#sayBinary(String)}.
 * @author Alessandro Chiariello (Demetrio)
 * @version 1.0
 * @see com.demetrio.hecu.Hecu Hecu
 */
public class Binary
{
	/**
	 * The binary string
	 */
    private String value;
    
    /**
     * The audio stream, encoded in <a href="https://en.wikipedia.org/wiki/WAV">WAV</a> format, of the binary string. <br />
     * It's an audio of the binary string said with the voice of HECU (every number is said aggressively).
     */
    private AudioInputStream audio;
    
    /**
     * Default constructor: no initialization is made.
     * @author Alessandro Chiariello (Demetrio)
     */
    public Binary() {}
    
    /**
     * Construct a {@link Binary Binary} object with the given
     * values passed as parameters.
     * @param value the binary string
     * @param audio the binary audio stream
     * @author Alessandro Chiariello (Demetrio)
     * @see AudioInputStream AudioInputStream
     */
    public Binary(String value,AudioInputStream audio)
    {
        this.value = value;
        this.audio = audio;
    }

    /**
     * Get the binary audio stream.
     * @return the audio stream
     * @author Alessandro Chiariello (Demetrio)
     * @see AudioInputStream AudioInputStream
     */
    public AudioInputStream getAudio() {
        return audio;
    }

    /**
     * Get the binary string.
     * @return the binary string
     * @author Alessandro Chiariello (Demetrio)
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the binary string.
     * @param value the binary string
     * @author Alessandro Chiariello (Demetrio)
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Set the binary audio stream.
     * @param audio the binary audio stream
     * @author Alessandro Chiariello (Demetrio)
     * @see AudioInputStream AudioInputStream
     */
    public void setAudio(AudioInputStream audio) {
        this.audio = audio;
    }
}