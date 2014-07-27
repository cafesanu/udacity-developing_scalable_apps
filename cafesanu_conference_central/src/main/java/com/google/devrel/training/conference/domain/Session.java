package com.google.devrel.training.conference.domain;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.repackaged.org.joda.time.Duration;
import com.google.common.base.Preconditions;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.SessionForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

@Entity
@Cache
public class Session {

    /**
     * The id for the datastore key.
     *
     * We use automatic id assignment for entities of Conference class.
     */
    @Id
    private long   id;

    /**
     * Holds Conference key as the parent.
     */
    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Conference> conferenceKey;

    private long conferenceId;

    private String sessionName;
    private String highlights;
    String         speaker;
    int            duration;
    String         type;
    Date           date;
    String         time;


    /**
     * Just making the default constructor private.
     */
    // @SuppressWarnings("unused")
    // private Session() {}

    /**
     * 
     * @param id
     * @param organizerUserId
     * @param sessionForm
     */
    public Session(final long sessionId, final long conferenceId, final SessionForm sessionForm) {    
        //Check preconditions
        this.checkPreconditions(sessionForm);

        this.id = sessionId;
        this.conferenceKey = Key.create(Conference.class, conferenceId);
        this.conferenceId = conferenceId;
        this.updateWithSessionForm(sessionForm);
    }

    private void updateWithSessionForm(SessionForm sessionForm) {
        this.sessionName = sessionForm.getName();
        this.speaker = sessionForm.getSpeaker();
        this.duration = sessionForm.getDuration();
        this.type = sessionForm.getType();
        this.date = sessionForm.getDate();
        this.time = sessionForm.getTime();
        this.highlights = sessionForm.getHighlights();        
    }

    private void checkPreconditions(SessionForm sessionForm) {
        String sessionName = sessionForm.getName();
        String speaker = sessionForm.getSpeaker();
        int duration = sessionForm.getDuration();
        String type = sessionForm.getType();
        Date date = sessionForm.getDate();
        String time = sessionForm.getTime();
        

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

        try {
            SimpleDateFormat militaryTime = new SimpleDateFormat("HH:mm");
            militaryTime.setLenient(false);
            //If time is not in military time, parse will throw an exception
            militaryTime.parse(time);
        }
        catch (ParseException pe) {
            throw new IllegalArgumentException("Time must be in military hour");
        }  
        catch (Exception pe) {
            throw new IllegalArgumentException("Unknown exception");
        }  
    }

    public Session() {
        super();
        this.sessionName = "Hello";
        this.highlights = "HL";
        this.speaker = "Speaker";
        this.duration = 10;
        this.type = "Music";
        this.date = new Date(10000);
        this.time = "12:50";
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getHighlights() {
        return highlights;
    }

    public void setHighlights(String highlights) {
        this.highlights = highlights;
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getConferenceId() {
        return conferenceId;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Id: " + this.id + "\n")
                .append("Name: ").append(this.sessionName).append("\n");
        if (this.highlights != null) {
            stringBuilder.append("Highlights: ").append(this.highlights).append("\n");
        }
        stringBuilder.append("Speaker: ").append(this.speaker).append("\n")
            .append("Duration: ").append(this.duration).append("\n")
            .append("Type: ").append(this.type).append("\n")
            .append("Date: ").append(this.date.toString()).append("\n")
            .append("Time: ").append(this.time).append("\n");
        return stringBuilder.toString();
    }

}