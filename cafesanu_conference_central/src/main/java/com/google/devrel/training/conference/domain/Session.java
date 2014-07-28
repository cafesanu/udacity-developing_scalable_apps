package com.google.devrel.training.conference.domain;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.base.Preconditions;
import com.google.devrel.training.conference.form.SessionForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

@Entity
@Cache
public class Session {

    /**
     * The id for the datastore key.
     *
     * Use automatic id assignment for entities of Session class.
     */
    @Id
    private long            id;

    /**
     * Holds Conference key as the parent.
     */
    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Conference> conferenceKey;

    /**
     * The session name
     */
    @Index
    private String          name;

    /**
     * The highlights of the session
     */
    private String          highlights;

    /**
     * Name of the speaker
     */
    @Index
    private String          speaker;

    /**
     * Duration in minutes of the session
     */
    private int             duration;

    /**
     * The session type
     */
    @Index
    private String          type;

    /**
     * Date of session taking place
     */
    private Date            date;

    /**
     * Time the session starts if format HH:MM
     */
    private String          time;

    /**
     * Making the default constructor private.
     */
    @SuppressWarnings("unused")
    private Session() {
    }

    /**
     * 
     * @param sessionId
     *            the id of this entity
     * @param conferenceKey
     *            the id of the parent conference
     * @param sessionForm
     *            the properties of the new entity
     */
    public Session(final long sessionId, final Key<Conference> conferenceKey, final SessionForm sessionForm) {
        // Check preconditions before creating entity
        this.checkPreconditions(sessionForm);

        this.id = sessionId;
        this.conferenceKey = conferenceKey;
        this.updateWithSessionForm(sessionForm);
    }

    /**
     * 
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

    /**
     * 
     * @param sessionForm
     *            check user's input is valid, otherwise, throw exception
     */
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
            // If time is not in military time, parse will throw an exception
            militaryTime.parse(time);
        }
        catch (ParseException pe) {
            throw new IllegalArgumentException("Time must be in military hour");
        }
        catch (Exception pe) {
            throw new IllegalArgumentException("Unknown exception");
        }
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

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Key<Conference> getConferenceKey() {
        return conferenceKey;
    }

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

}