package com.google.devrel.training.conference.domain;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.base.Preconditions;
import com.google.devrel.training.conference.form.SessionForm;
import com.google.devrel.training.conference.utils.Time24HoursValidator;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.Key;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Cache
public class Session {

    /* **********************************************************************
     * CONSTANTS
     * **********************************************************************
     */
    
    /* **********************************************************************
     * ENUMS
     * **********************************************************************
     */
     
    /* **********************************************************************
     * ATTRIBUTES
     * **********************************************************************
     */

    /**
     * Holds Conference key as the parent.
     */
    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Conference> conferenceKey;

    /**
     * Date of session taking place
     */
    @Index
    private Date            date;
    
    /**
     * Duration in minutes of the session
     */
    private int             duration;

    /**
     * The highlights of the session
     */
    private String          highlights;
    
    /**
     * The id for the datastore key.
     *
     * Use automatic id assignment for entities of Session class.
     */
    @Id
    private long            id;

    /**
     * The session name
     */
    @Index
    private String          name;

    /**
     * Name of the speaker
     */
    @Index
    private String          speaker;

    /**
     * Time the session starts if format HH:MM
     */
    @Index
    private String          time;
    
    /**
     * The session type
     */
    @Index
    private String          type;

    
    /* **********************************************************************
     * CONSTRUCTORS
     * **********************************************************************
     */
    
    /**
     * Making the default constructor private.
     */
    @SuppressWarnings("unused")
    private Session() {
    }

    /**
     * All attributes except for highlights are required.
     * 
     * @param sessionId
     *            the id of this entity
     * @param conferenceKey
     *            the id of the parent conference
     * @param sessionForm
     *            the properties of the new entity
     */
    public Session(final long sessionId, final Key<Conference> conferenceKey, final SessionForm sessionForm){
        // Check preconditions before creating entity
        this.checkPreconditions(sessionForm);

        this.id = sessionId;
        this.conferenceKey = conferenceKey;
        this.updateWithSessionForm(sessionForm);
    }
    
    /* **********************************************************************
     * OVERRIDES
     * **********************************************************************
     */

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Id: " + this.id + "\n").append("Name: ").append(this.name).append("\n");
        if (this.highlights != null) {
            stringBuilder.append("Highlights: ").append(this.highlights).append("\n");
        }
        stringBuilder.append("Speaker: ").append(this.speaker).append("\n").append("Duration: ").append(this.duration).append("\n").append("Type: ")
                        .append(this.type).append("\n").append("Date: ").append(this.date.toString()).append("\n").append("Time: ").append(this.time)
                        .append("\n");
        return stringBuilder.toString();
    }
    
    /* **********************************************************************
     * SETTERS AND GETTERS FOR ATTRIBUTES
     * **********************************************************************
     */

    /**
     * Getter for conferenceKey.
     * 
     * @return conferenceKey.
     */
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Key<Conference> getConferenceKey() {
        return conferenceKey;
    }

    /**
     * Returns a defensive copy of date if not null.
     * 
     * @return a defensive copy of date if not null.
     */
    public Date getDate() {
        return date == null ? null : new Date(date.getTime());
    }

    /**
     * Getter for duration.
     * 
     * @return duration.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Getter for highlights.
     * 
     * @return highlights.
     */
    public String getHighlights() {
        return highlights;
    }
    
    /**
     * Getter for id.
     * 
     * @return id.
     */
    public long getId() {
        return id;
    }

    /**
     * Getter for name.
     * 
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for speaker.
     * 
     * @return speaker.
     */
    public String getSpeaker() {
        return speaker;
    }

    /**
     * Getter for time.
     * 
     * @return time.
     */
    public String getTime() {
        return time;
    }

    /**
     * Getter for type.
     * 
     * @return type.
     */
    public String getType() {
        return type;
    }
    
    /* **********************************************************************
     * PRIVATE METHODS
     * **********************************************************************
     */

    /**
     * /**
     * check user's input is valid, otherwise, throw exception
     * 
     * @param sessionForm
     *            User's input to be verified
     */
    private void checkPreconditions(SessionForm sessionForm) {
        String sessionName = sessionForm.getName();
        String speaker = sessionForm.getSpeaker();
        int duration = sessionForm.getDuration();
        String type = sessionForm.getType();
        Date date = sessionForm.getDate();
        String time = sessionForm.getTime();
        boolean timeisValid = Time24HoursValidator.validate(time);

        Preconditions.checkNotNull(sessionName, "The name is required");
        Preconditions.checkArgument(!sessionName.isEmpty(), "The name is required");
        Preconditions.checkNotNull(speaker, "Speaker is required");
        Preconditions.checkArgument(!speaker.isEmpty(), "Speaker is required");
        Preconditions.checkArgument(duration >= 15, "Session must be at least 15 minutes");
        Preconditions.checkNotNull(type, "Session type is required");
        Preconditions.checkArgument(!type.isEmpty(), "Session type is required");
        Preconditions.checkNotNull(date, "Date is required");
        Preconditions.checkNotNull(time, "Session time is required");
        Preconditions.checkArgument(!time.isEmpty(), "Session time is required");
        Preconditions.checkArgument(timeisValid, "Time must be in military hour in format hh:mm");

    }
    
    /**
     * Updates attributes with user's input
     * @param sessionForm
     *            user's input with session's properties
     */
    private void updateWithSessionForm(SessionForm sessionForm) {
        this.name = sessionForm.getName();
        this.speaker = sessionForm.getSpeaker();
        this.duration = sessionForm.getDuration();
        this.type = sessionForm.getType();
        this.date = sessionForm.getDate();
        this.time = sessionForm.getTime();
        this.highlights = sessionForm.getHighlights();
    }
    
    /* **********************************************************************
     * PUBLIC METHODS
     * **********************************************************************
     */ 

}