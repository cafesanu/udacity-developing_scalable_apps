package com.google.devrel.training.conference.form;

import java.util.Date;

public class SessionForm {
    /**
     * Name of the session
     */
    String name;
    
    /**
     * Highlights of the session
     */
    String highlights;
    
    /**
     * Speaker of the session
     */
    String speaker;
    
    /**
     * duration of the session represented in minutes
     */
    int duration;
    
    /**
     * Type of session: (eg lecture, keynote, workshop)
     */
    String type;

    /**
     * Date session takes place. Should be in MM-dd-yyyy format
     */
    Date date;

    /**
     * time session starts represented as HH:MM
     */
    String time;

    @SuppressWarnings("unused")
    private SessionForm() {}

    public String getName() {
        return name;
    }

    public String getHighlights() {
        return highlights;
    }

    public String getSpeaker() {
        return speaker;
    }

    public int getDuration() {
        return duration;
    }

    public String getType() {
        return type;
    }

    public Date getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }
}
