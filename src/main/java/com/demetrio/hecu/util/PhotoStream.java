package com.demetrio.hecu.util;

import java.io.InputStream;

/**
 * Bean class for containing the result of <i>photo</i> request.<br/>
 * A {@link java.util.List List} of instances of this class is returned by
 * {@link com.demetrio.hecu.Hecu#getPhotos(String, int, Runnable) getPhotos(String, int, Runnable)} and
 * {@link com.demetrio.hecu.Hecu#getRandom(int, int, int, Runnable) getRandom(int, int, int, Runnable)} methods.
 * @author Alessandro Chiariello (Demetrio)
 * @version 1.0
 * @see com.demetrio.hecu.Hecu Hecu
 */
public class PhotoStream {
	// the photo InputStream
	private InputStream input;

	// the photo name
	private String name;

	/**
	 * Get the image {@link InputStream InputStream}.
	 * @return the image {@link InputStream InputStream}
	 * @author Alessandro Chiariello (Demetrio)
	 */
	public InputStream getInput() {
		return input;
	}

	/**
	 * Set the image {@link InputStream InputStream}.
	 * @param input the image {@link InputStream InputStream}
	 * @author Alessandro Chiariello (Demetrio)
	 */
	public void setInput(InputStream input) {
		this.input = input;
	}

	/**
	 * Get the name of the image
	 * @return the name of the image
	 * @author Alessandro Chiariello (Demetrio)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the image
	 * @param name the name of the image
	 * @author Alessandro Chiariello (Demetrio)
	 */
	public void setName(String name) {
		this.name = name;
	}
}
