package com.demetrio.hecu.util;

import java.util.Objects;

/**
 * Bean class to contain data for the users requests. <br />
 * This class is fundamental for the application logic as it represents a two-step request
 * that a user does to the bot; a one-step request is not wrapped in this class. <br />
 * Because {@link Request Request} objects need to be
 * compared, printed and used in a {@link}, this bean implements {@link #equals(Object)},
 * {@link #toString()} and {@link #hashCode()}
 * @author Alessandro Chiariello (Demetrio)
 * @version 1.0
 */
public class Request
{
	/**
	 * Enum that represents the type of the request.
	 * @author Alessandro Chiariello (Demetrio)
	 * @version 1.0
	 */
    public enum Type
    {
        SAY,
        BINARY,
        PHOTO
    }
    
    /**
     * Telegram Id of the user who made the request.
     */
    private Integer user;
    
    /**
     * Type of the request.
     * @see Type Type
     */
    private Type type;
    
    /**
     * Default constructor: no initialization is made.
     * @author Alessandro Chiariello (Demetrio)
     */
    public Request() {}
    
    /**
     * Construct a {@link Request Request} object with the given
     * values passed as parameters.
     * @param user the Telegram Id of the user who made the request
     * @param type the type of the request
     * @author Alessandro Chiariello (Demetrio)
     * @see Type Type
     */
    public Request(Integer user,Type type)
    {
        this.user = user;
        this.type = type;
    }

    /**
     * Get the type of the request.
     * @return the type of the request
     * @author Alessandro Chiariello (Demetrio)
     * @see Type Type
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the Telegram Id of the user who made the request
     * @return the Telegram Id of the user who made the request
     * @author Alessandro Chiariello (Demetrio)
     */
    public Integer getUser() {
        return user;
    }

    /**
     * Set the type of the request
     * @param type the request type
     * @author Alessandro Chiariello (Demetrio)
     * @see Type Type
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Set the Telegram Id of the user who made the request
     * @param user the Telegram Id of the user who made the request
     * @author Alessandro Chiariello (Demetrio)
     */
    public void setUser(Integer user) {
        this.user = user;
    }
    
    /**
     * Compare {@link Request this} with a object passed as parameter. <br />
     * This method uses {@link Objects#equals(Object) Objects#equals(Object)}.
     * @param obj the object to be compared with
     * @return {@code true} if {@code this} and {@code obj} are equal, {@code false} otherwise
     * @author Alessandro Chiariello (Demetrio)
     * @see Objects Objects
     */
    @Override
    public boolean equals(Object obj) {
        if (obj!=null && getClass()==obj.getClass())
        {
            Request other = (Request) obj;
            return Objects.equals(user, other.user) && Objects.equals(type,other.type);
        }
        return false;
    }
    
    /**
     * Get the hash code of the {@link Request Request} object.
     * This method uses {@link Objects#hash(Object...) Objects#hash(Object...)}.
     * @return the hash code of the object
     * @author Alessandro Chiariello (Demetrio)
     * @see Objects Objects
     */
    @Override
    public int hashCode() {
        return Objects.hash(user,type);
    }
    
    /**
     * Get the string representation of the {@link Request Request} object. <br />
     * The string format is: <i>Request{user = {@code user},type = {@code type}}</i>
     * @return the string representation of the object
     * @author Alessandro Chiariello (Demetrio)
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{user = " + user + ",type = " + type + "}";
    }
}