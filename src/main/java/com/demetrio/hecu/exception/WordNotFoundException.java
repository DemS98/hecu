package com.demetrio.hecu.exception;

/**
 * Exception thrown when a word sent to the bot is not found. <br />
 * It is a {@link RuntimeException RuntimeException}.
 * @author Alessandro Chiariello (Demetrio)
 * @version 1.0
 * @see RuntimeException RuntimeException
 */
public class WordNotFoundException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * It construct a new {@link WordNotFoundException WordNotFoundException}
	 * with the following detail message: <i>Word not found</i>. <br />
	 * It is recommended to use {@link #WordNotFoundException(String)} to specify a more detailed
	 * message.
	 * @author Alessandro Chiariello (Demetrio)
	 * @see RuntimeException#RuntimeException(String) RuntimeException(String)
	 */
	public WordNotFoundException()
    {
        super("Word not found");
    }
	
	/**
	 * It construct a new {@link WordNotFoundException WordNotFoundException}
	 * with the detail message passed as parameter. <br />
	 * @param msg the detail message of the exception
	 * @author Alessandro Chiariello (Demetrio)
	 * @see RuntimeException#RuntimeException(String) RuntimeException(String)
	 */
    public WordNotFoundException(String msg)
    {
        super(msg);
    }
}