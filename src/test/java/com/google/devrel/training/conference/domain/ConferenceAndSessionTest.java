package com.google.devrel.training.conference.domain;

import static com.google.devrel.training.conference.service.OfyService.ofy;
import static org.junit.Assert.*;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.SessionForm;
import com.googlecode.objectify.Key;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tests for Conference POJO.
 */
public class ConferenceAndSessionTest {
    
    //Conference

    private static final long CONFERENCE_ID = 123456L;

    private static final String CONFERENCE_NAME = "GCP Live";

    private static final String CONFERENCE_DESCRIPTION = "New announcements for Google Cloud Platform";

    private static final String CONFERENCE_ORGANIZER_USER_ID = "123456789";

    private static final String CONFERENCE_CITY = "San Francisco";

    private static final int CONFERENCE_MONTH = 3;

    private static final int CONFERENCE_CAP = 500;

    private Date conferenceStartDate;

    private Date conferenceEndDate;

    private List<String> conferenceTopics;

    private ConferenceForm conferenceForm;
    
    //Session attributes    

    private Conference sessionConference;
    
    private Key<Conference> sessionConferenceKey;
    
    private static final int SESSION_DURATION = 120;

    private static final String SESSION_HIGHLIGHTS = "This is a great session!";

    private static final long SESSION_ID = 456789L;

    private static final String SESSION_NAME = "Session 1";

    private static final String SESSION_SPEAKER = "Carlos Sanchez";

    private static final String SESSION_TIME = "12:30";

    private static final String SESSION_TYPE = "workshop";

    private  Date sessionDate;
    
    private SessionForm sessionForm;

    private final LocalServiceTestHelper helper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()
                    .setDefaultHighRepJobPolicyUnappliedJobPercentage(100));
    
  //Conference Tests

    @Before
    public void setUp() throws Exception {
        helper.setUp();
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        conferenceStartDate = dateFormat.parse("03/25/2014");
        conferenceEndDate = dateFormat.parse("03/26/2014");
        conferenceTopics = new ArrayList<>();
        conferenceTopics.add("Google");
        conferenceTopics.add("Cloud");
        conferenceTopics.add("Platform");
        conferenceForm = new ConferenceForm(CONFERENCE_NAME, CONFERENCE_DESCRIPTION, conferenceTopics, CONFERENCE_CITY, conferenceStartDate, conferenceEndDate,
                CONFERENCE_CAP);
        sessionConference = new Conference(CONFERENCE_ID, CONFERENCE_ORGANIZER_USER_ID, conferenceForm);
        sessionConferenceKey = Key.create(sessionConference.getProfileKey(), Conference.class, CONFERENCE_ID);
        sessionDate = dateFormat.parse("03/25/2014");
        sessionForm = new SessionForm(sessionDate, SESSION_DURATION, SESSION_HIGHLIGHTS, SESSION_NAME, SESSION_SPEAKER,SESSION_TIME, SESSION_TYPE);

    }

    @After
    public void tearDown() throws Exception {
        helper.tearDown();
    }

    @Test(expected = NullPointerException.class)
    public void testConferenceNullName() throws Exception {
        ConferenceForm nullConferenceForm = new ConferenceForm(null, CONFERENCE_DESCRIPTION, conferenceTopics, CONFERENCE_CITY,
                conferenceStartDate, conferenceEndDate, CONFERENCE_CAP);
        new Conference(CONFERENCE_ID, CONFERENCE_ORGANIZER_USER_ID, nullConferenceForm);
    }

    @Test
    public void testConference() throws Exception {
        Conference conference = new Conference(CONFERENCE_ID, CONFERENCE_ORGANIZER_USER_ID, conferenceForm);
        assertEquals(CONFERENCE_ID, conference.getId());
        assertEquals(CONFERENCE_NAME, conference.getName());
        assertEquals(CONFERENCE_DESCRIPTION, conference.getDescription());
        assertEquals(CONFERENCE_ORGANIZER_USER_ID, conference.getOrganizerUserId());
        assertEquals(conferenceTopics, conference.getTopics());
        assertEquals(conferenceStartDate, conference.getStartDate());
        assertEquals(conferenceEndDate, conference.getEndDate());
        assertEquals(CONFERENCE_MONTH, conference.getMonth());
        assertEquals(CONFERENCE_CAP, conference.getMaxAttendees());
        assertEquals(CONFERENCE_CAP, conference.getSeatsAvailable());
        // Test if they are defensive copies.
        assertNotSame(conferenceTopics, conference.getTopics());
        assertNotSame(conferenceStartDate, conference.getStartDate());
        assertNotSame(conferenceEndDate, conference.getEndDate());
    }

    @Test
    public void testConferenceGetOrganizerDisplayName() throws Exception {
        String displayName = "Udacity Student";
        Profile profile = new Profile(CONFERENCE_ORGANIZER_USER_ID, displayName, "", null);
        ofy().save().entity(profile).now();
        Conference conference = new Conference(CONFERENCE_ID, CONFERENCE_ORGANIZER_USER_ID, conferenceForm);
        assertEquals(displayName, conference.getOrganizerDisplayName());
    }

    @Test
    public void testConferenceBookSeats() throws Exception {
        Conference conference = new Conference(CONFERENCE_ID, CONFERENCE_ORGANIZER_USER_ID, conferenceForm);
        conference.bookSeats(1);
        assertEquals(CONFERENCE_CAP - 1, conference.getSeatsAvailable());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConferenceBookSeatsFailure() throws Exception {
        Conference conference = new Conference(CONFERENCE_ID, CONFERENCE_ORGANIZER_USER_ID, conferenceForm);
        conference.bookSeats(500);
        assertEquals(0, conference.getSeatsAvailable());
        // this will fail
        conference.bookSeats(1);
    }

    
    @Test
    public void testConferenceReturnSeats() throws Exception {
        Conference conference = new Conference(CONFERENCE_ID, CONFERENCE_ORGANIZER_USER_ID, conferenceForm);
        conference.bookSeats(1);
        assertEquals(CONFERENCE_CAP - 1, conference.getSeatsAvailable());
        conference.giveBackSeats(1);
        assertEquals(CONFERENCE_CAP, conference.getSeatsAvailable());
    }
    

    
    @Test(expected = IllegalArgumentException.class)
    public void testConferenceReturnSeatsFailure() throws Exception {
        Conference conference = new Conference(CONFERENCE_ID, CONFERENCE_ORGANIZER_USER_ID, conferenceForm);
        conference.giveBackSeats(1);
    }
    
    /* ****************************************************************
     * Session
     * ****************************************************************
     */
    @Test(expected = NullPointerException.class)
    public void testSessioNullName() throws Exception {
        SessionForm nullSessionForm = new SessionForm(sessionDate, SESSION_DURATION, SESSION_HIGHLIGHTS,null, SESSION_SPEAKER, SESSION_TIME, SESSION_TYPE);
        new Session(SESSION_ID, sessionConferenceKey, nullSessionForm);
    }

    @Test
    public void testSession() throws Exception {
        Session session = new Session(SESSION_ID, sessionConferenceKey, sessionForm);
        assertEquals(SESSION_DURATION, session.getDuration());
        assertEquals(SESSION_HIGHLIGHTS, session.getHighlights());
        assertEquals(SESSION_ID, session.getId());
        assertEquals(SESSION_NAME, session.getName());
        assertEquals(SESSION_SPEAKER, session.getSpeaker());
        assertEquals(SESSION_TIME, session.getTime());
        assertEquals(SESSION_TYPE, session.getType());
        assertEquals(sessionDate, session.getDate());
        // Test if they are defensive copies.
        assertNotSame(sessionDate, session.getDate());
    }   
}
