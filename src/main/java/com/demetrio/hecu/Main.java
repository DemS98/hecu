package com.demetrio.hecu;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

/**
 * Class that contains {@link #main(String[]) main} method.
 * @author Alessandro Chiariello (Demetrio)
 * @version 1.0
 */
public class Main {
	/**
	 * main method to start the bot and load {@link Hecu Hecu} class.
	 * @param args not used
	 * @throws TelegramApiRequestException - if there are problems starting the bot, e.g. token invalid.
	 * @throws ClassNotFoundException - if the {@link Hecu Hecu} class cannot be found.
	 * @author Alessandro Chiariello (Demetrio)
	 * @see Hecu Hecu
	 */
    public static void main(String[] args) throws TelegramApiRequestException, ClassNotFoundException {
    	// initialize the API context
        ApiContextInitializer.init();
        
        // load eagerly Hecu class
        Class.forName(Hecu.class.getName());
        
        // starts the bot
        TelegramBotsApi api = new TelegramBotsApi();
        api.registerBot(new Bot());
    }
}