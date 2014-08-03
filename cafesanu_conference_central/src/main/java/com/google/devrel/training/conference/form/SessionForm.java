package com.google.devrel.training.conference.form;

import java.util.Date;

public class SessionForm {

    /* **********************************************************************
     * CONSTANTS
     * **********************************************************************
     */

    /* **********************************************************************
     * ENUMS
     * **********************************************************************
     */

    /* **********************************************************************
     * INNER CLASSES
     * **********************************************************************
     */

    /* **********************************************************************
     * ATTRIBUTES
     * **********************************************************************
     */

    /**
     * Date session takes place. Should be in MM-dd-yyyy format
     */
    Date   date;

    /**
     * duration of the session represented in minutes
     */
    int    duration;

    /**
     * Highlights of the session
     */
    String highlights;

    /**
     * Name of the session
     */
    String name;

    /**
     * Speaker of the session
     */
    String speaker;

    /**
     * time session starts represented as HH:MM
     */
    String time;

    /**
     * Type of session: (eg lecture, keynote, workshop)
     */
    String type;
    

    /* **********************************************************************
     * CONSTRUCTORS
     * **********************************************************************
     */

    @SuppressWarnings("unused")
    private SessionForm() {
    }

    /**
     * Constructor for ProfileForm, solely for unit test.
     * 
     * @param displayName
     *            A String for displaying the user on this system.
     * @param notificationEmail
     *            An e-mail address for getting notifications from this system.
     */
    public SessionForm(Date date, int duration, String highlights, String name, String speaker, String time, String type) {
        this.date = date;
        this.duration = duration;
        this.highlights = highlights;
        this.name = name;
        this.speaker = speaker;
        this.time = time;
        this.type = type;
    }
    
    /* **********************************************************************
     * OVERRIDES
     * **********************************************************************
     */
    
    /* **********************************************************************
     * SETTERS AND GETTERS FOR ATTRIBUTES
     * **********************************************************************
     */

    public Date getDate() {
        return date;
    }

    public int getDuration() {
        return duration;
    }
    
    public String getName() {
        return name;
    }

    public String getHighlights() {
        return highlights;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getTime() {
        return time;
    }
    
    public String getType() {
        return type;
    }
    
    /* **********************************************************************
     * PRIVATE METHODS
     * **********************************************************************
     */
    
    /* **********************************************************************
     * PUBLIC METHODS
     * **********************************************************************
     */  
     
     
}
