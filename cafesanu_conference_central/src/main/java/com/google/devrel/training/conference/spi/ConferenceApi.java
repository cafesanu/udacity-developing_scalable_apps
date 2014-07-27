package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.ofy;
import static com.google.devrel.training.conference.service.OfyService.factory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Logger;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.google.appengine.repackaged.org.joda.time.Duration;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Announcement;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.domain.Session;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.google.devrel.training.conference.form.SessionForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;

/**
 * Defines conference APIs.
 */
@Api(name = "conference", version = "v1", scopes = { Constants.EMAIL_SCOPE }, clientIds = { Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID }, description = "API for the Conference Central Backend application.")
public class ConferenceApi {

    private static final Logger LOG = Logger.getLogger(ConferenceApi.class.getName());

    /**
     * Just a wrapper for Boolean. We need this wrapped Boolean because
     * endpoints functions must return an object instance, they can't return a
     * Type class such as String or Integer or Boolean
     */
    public static class WrappedBoolean {

        private final Boolean result;
        private final String  reason;

        public WrappedBoolean(Boolean result) {
            this.result = result;
            this.reason = "";
        }

        public WrappedBoolean(Boolean result, String reason) {
            this.result = result;
            this.reason = reason;
        }

        public Boolean getResult() {
            return result;
        }

        public String getReason() {
            return reason;
        }
    }

    /*
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     */
    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    /**
     * Gets the Profile entity for the current user or creates it if it doesn't
     * exist
     * 
     * @param user
     * @return user's Profile
     */
    private static Profile getProfileFromUser(User user) {
        // First fetch the user's Profile from the datastore.
        Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            // Create a new Profile if it doesn't exist.
            // Use default displayName and teeShirtSize
            String userId = user.getUserId();
            String email = user.getEmail();
            String displayName = extractDefaultDisplayNameFromEmail(email);
            TeeShirtSize teeShirtSize = TeeShirtSize.NOT_SPECIFIED;
            profile = new Profile(userId, displayName, email, teeShirtSize);
        }
        return profile;
    }

    /**
     * Creates or updates a Profile object associated with the given user
     * object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @param profileForm
     *            A ProfileForm object sent from the client form.
     * @return Profile object just created.
     * @throws UnauthorizedException
     *             when the User object is null.
     */

    // Declare this method as a method available externally through Endpoints
    @ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
    // The request that invokes this method should provide data that
    // conforms to the fields defined in ProfileForm
    public Profile saveProfile(final User user, ProfileForm profileForm) throws UnauthorizedException {

        // If the user is not logged in, throw an UnauthorizedException
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId and mainEmail
        String mainEmail = user.getEmail();
        String userId = user.getUserId();

        // Get the displayName and teeShirtSize sent by the request.

        String displayName = profileForm.getDisplayName();
        TeeShirtSize teeShirtSize = profileForm.getTeeShirtSize();

        // Get the Profile from the datastore if it exists
        // otherwise create a new one
        Profile profile = ofy().load().key(Key.create(Profile.class, userId)).now();

        if (profile == null) {
            // Populate the displayName and teeShirtSize with default values
            // if not sent in the request
            if (displayName == null) {
                displayName = extractDefaultDisplayNameFromEmail(user.getEmail());
            }
            if (teeShirtSize == null) {
                teeShirtSize = TeeShirtSize.NOT_SPECIFIED;
            }
            // Now create a new Profile entity
            profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
        }
        else {
            // The Profile entity already exists
            // Update the Profile entity
            profile.update(displayName, teeShirtSize);
        }

        // Save the entity in the datastore
        ofy().save().entity(profile).now();

        // Return the profile
        return profile;
    }

    /**
     * Returns a Profile object associated with the given user object. The cloud
     * endpoints system automatically inject the User object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @return Profile object.
     * @throws UnauthorizedException
     *             when the User object is null.
     */
    @ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
    public Profile getProfile(final User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // load the Profile Entity
        String userId = user.getUserId();
        Key<Profile> key = Key.create(Profile.class, userId);
        Profile profile = ofy().load().key(key).now(); // load the Profile
                                                       // entity
        return profile;
    }

    /**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user
     *            A user who invokes this method, null when the user is not
     *            signed in.
     * @param conferenceForm
     *            A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException
     *             when the user is not signed in.
     */
    @ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
    public Conference createConference(final User user, final ConferenceForm conferenceForm) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId of the logged in User
        final String userId = user.getUserId();

        // Get the key for the User's Profile
        Key<Profile> profileKey = Key.create(Profile.class, userId);

        // Allocate a key for the conference -- let App Engine allocate the ID
        // Don't forget to include the parent Profile in the allocated ID
        final Key<Conference> conferenceKey = factory().allocateId(profileKey, Conference.class);

        // Get the Conference Id from the Key
        final long conferenceId = conferenceKey.getId();

        // Get the default queue
        final Queue queue = QueueFactory.getDefaultQueue();

        // Start a transaction
        Conference conference = ofy().transact(new Work<Conference>() {

            @Override
            public Conference run() {
                // Get the existing Profile entity for the current user if there
                // is one
                // Otherwise create a new Profile entity with default values
                Profile profile = ConferenceApi.getProfileFromUser(user);

                // Create a new Conference Entity, specProfile profile = ConferenceApi.getProfileFromUser(user);ifying the user's Profile
                // entity
                // as the parent of the conference
                Conference conference = new Conference(conferenceId, userId, conferenceForm);

                // Save conference and Profile
                ofy().save().entities(profile, conference).now();

                // add send email to queue
                queue.add(ofy().getTransaction(),
                                TaskOptions.Builder.withUrl("/tasks/send_confirmation_email")
                                    .param("emailType", Constants.NEW_CONFERENCE )
                                    .param("email", profile.getMainEmail())
                                    .param("conferenceInfo", conference.toString())

                );
                return conference;
            }

        });

        return conference;
    }

    /**
     * Queries against the datastore with the given filters and returns the
     * result.
     *
     * Normally this kind of method is supposed to get invoked by a GET HTTP
     * method, but we do it with POST, in order to receive conferenceQueryForm
     * Object via the POST body.
     *
     * @param conferenceQueryForm
     *            A form object representing the query.
     * @return A List of Conferences that match the query.
     */
    @ApiMethod(name = "queryConferences", path = "queryConferences", httpMethod = HttpMethod.POST)
    public List<Conference> queryConferences(ConferenceQueryForm conferenceQueryForm) {
        // Query<Conference> query =
        // ofy().load().type(Conference.class).order("name");

        Iterable<Conference> conferenceIterable = conferenceQueryForm.getQuery();
        List<Conference> result = new ArrayList<>(0);
        List<Key<Profile>> organizersKeyList = new ArrayList<>(0);
        for (Conference c : conferenceIterable) {
            organizersKeyList.add(Key.create(Profile.class, c.getOrganizerUserId()));
            result.add(c);
        }
        // To avoid separate datasotre gets for each conference, pre-fetch
        // Profiles
        ofy().load().keys(organizersKeyList);

        return result;
    }

    /**
     * Returns a list of Conferences that the user created. In order to receive
     * the websafeConferenceKey via the JSON params, uses a POST method.
     *
     * @param user
     *            A user who invokes this method, null when the user is not
     *            signed in.
     * @return a list of Conferences that the user created.
     * @throws UnauthorizedException
     *             when the user is not signed in.
     */
    @ApiMethod(name = "getConferencesCreated", path = "getConferencesCreated", httpMethod = HttpMethod.POST)
    public List<Conference> getConferencesCreated(final User user) throws UnauthorizedException {

        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        Key<Profile> profileKey = Key.create(Profile.class, user.getUserId());
        Query<Conference> q = ofy().load().type(Conference.class).ancestor(profileKey);
        return q.list();

    }

    /**
     * Returns a conference object with given conferenceId
     * 
     * @param websafeConferenceKey
     *            The Siring representation of the Conference Key
     * @return a Conference Object with Given conferenceId
     * @throws NotFoundException
     *             when there is no conference with the given conferenceID
     */
    @ApiMethod(name = "getConference", path = "conference/{websafeConferenceKey}", httpMethod = HttpMethod.GET)
    public Conference getConference(@Named("websafeConferenceKey") final String websafeConferenceKey) throws NotFoundException {
        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (conference == null) {
            throw new NotFoundException(String.format("Not conference found with key: %s", websafeConferenceKey));
        }
        return conference;

    }

    /**
     * Register to attend the specified Conference.
     *
     * @param user
     *            An user who invokes this method, null when the user is not
     *            signed in.
     * @param websafeConferenceKey
     *            The String representation of the Conference Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException
     *             when the user is not signed in.
     * @throws NotFoundException
     *             when there is no Conference with the given conferenceId.
     */
    @ApiMethod(name = "registerForConference", path = "conference/{websafeConferenceKey}/registration", httpMethod = HttpMethod.POST)
    public WrappedBoolean registerForConference(final User user, @Named("websafeConferenceKey") final String websafeConferenceKey)
                    throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {

        // if not signed in, throw a 401 error
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId
        // final String userId = user.getUserId();

        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {

            @Override
            public WrappedBoolean run() {
                try {
                    // Get the Conference key -- you can get it from
                    // websafeCongerenceKey
                    // Will throw ForbiddenException if the key cannot be
                    // created
                    Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                    // Get the Conference entity for the datastore
                    Conference conference = ofy().load().key(conferenceKey).now();

                    // 404 when there is no Conference with the given
                    // ConferenceId
                    if (conference == null) {
                        return new WrappedBoolean(false, String.format("No conference found with Key: %s", websafeConferenceKey));
                    }

                    // Get the user's profile entity
                    Profile profile = getProfile(user);

                    // Has the user already registered to attend this
                    // conference?
                    if (profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
                        return new WrappedBoolean(false, "Already Registered");
                    }
                    else if (conference.getSeatsAvailable() <= 0) {
                        return new WrappedBoolean(false, "No seats available");
                    }
                    else {
                        // All looks good, go ahead and book the seat

                        // Add the websafeConferenceKey to the proifile's
                        // conferencesToAttend property

                        profile.addConferenceKeysToAttend(websafeConferenceKey);

                        // Decrease the conference's seat's available
                        // You can use the bookSeats() method on Conference
                        conference.bookSeats(1);

                        // Save the Conference and Profile entities
                        ofy().save().entities(conference, profile).now();

                        // We are booked!
                        return new WrappedBoolean(true, "Registration successful");

                    }

                }
                catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");
                }
            }

        });

        if (!result.getResult()) {
            String failReason = result.getReason();
            if (failReason.contains("No conference found with Key")) {
                throw new NotFoundException(result.getReason());
            }
            else if (failReason.equals("Already Registered")) {
                throw new ConflictException("You have already registered");
            }
            else if (failReason.equals("No seats available")) {
                throw new ConflictException("There are no seats available");
            }
        }
        return result;
    }

    /**
     * Returns a collection of Conference Object that the user is going to
     * attend.
     *
     * @param user
     *            An user who invokes this method, null when the user is not
     *            signed in.
     * 
     * @return a Collection of Conferences that the user is going to attend.
     * 
     * @throws UnauthorizedException
     *             when the User object is null.
     * @throws NotFoundException
     *             when the profile doesn'e exist.
     */
    @ApiMethod(name = "getConferencesToAttend", path = "getConferencesToAttend", httpMethod = HttpMethod.GET)
    public Collection<Conference> getConferencesToAttend(final User user) throws UnauthorizedException, NotFoundException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        String userId = user.getUserId();
        Key<Profile> key = Key.create(Profile.class, userId);
        Profile profile = ofy().load().key(key).now();

        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist");
        }

        // Get the value of the Profile's conferenceKeysToAttend property
        List<String> keyStringsToAttend = profile.getConferenceKeysToAttend();

        // Iterate over keyStringsToAttend, end return a Collection of the
        // Conference entities that the user has registered to attend
        List<Key<Conference>> keysToAttend = new ArrayList<>(0);
        for (String k : keyStringsToAttend) {
            Key<Conference> conferenceKey = Key.create(k);
            keysToAttend.add(conferenceKey);
        }
        return ofy().load().keys(keysToAttend).values();

    }

    /**
     * Unregister from the specified Conference.
     *
     * @param user
     *            An user who invokes this method, null when the user is not
     *            signed in.
     * @param websafeConferenceKey
     *            The String representation of the Conference Key to unregister
     *            from.
     * @return Boolean true when success, otherwise false.
     * @throws UnauthorizedException
     *             when the user is not signed in.
     * @throws NotFoundException
     *             when there is no Conference with the given conferenceId.
     */
    @ApiMethod(name = "unregisterFromConference", path = "conference/websafeConferenceKey/registration", httpMethod = HttpMethod.DELETE)
    public WrappedBoolean unregisterFromConference(final User user, @Named("websafeConferenceKey") final String websafeConferenceKey)
                    throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // if not signed in, throw a 401 error
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId
        // final String userId = user.getUserId();

        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {

            @Override
            public WrappedBoolean run() {
                try {
                    // Get the Conference key -- you can get it from
                    // websafeCongerenceKey
                    // Will throw ForbiddenException if the key cannot be
                    // created
                    Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                    // Get the Conference entity for the datastore
                    Conference conference = ofy().load().key(conferenceKey).now();

                    // 404 when there is no Conference with the given
                    // ConferenceId
                    if (conference == null) {
                        return new WrappedBoolean(false, String.format("No conference found with Key: %s", websafeConferenceKey));
                    }

                    // Get the user's profile entity
                    Profile profile = getProfile(user);

                    // Has the user already registered to attend this
                    // conference?
                    if (!profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
                        return new WrappedBoolean(false, "You are not registered for this conference");
                    }
                    else {
                        // All looks good, go ahead and book the seat

                        // Add the websafeConferenceKey to the proifile's
                        // conferencesToAttend property

                        profile.unregisterFromConference(websafeConferenceKey);

                        // Decrease the conference's seat's available
                        // You can use the bookSeats() method on Conference
                        conference.giveBackSeats(1);

                        // Save the Conference and Profile entities
                        ofy().save().entities(conference, profile).now();

                        // We are booked!
                        return new WrappedBoolean(true);

                    }

                }
                catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");
                }
            }

        });

        if (!result.getResult()) {
            String failReason = result.getReason();
            if (failReason.contains("No conference found with Key")) {
                throw new NotFoundException(result.getReason());
            }
            else if (failReason.equals("You are not registered for this conference")) {
                throw new ForbiddenException(result.getReason());
            }
        }
        return result;
    }

    @ApiMethod(name = "getAnnouncement", path = "announcement", httpMethod = HttpMethod.GET)
    public Announcement getAnnouncement() {
        MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
        String announcementKey = Constants.MEMCACHE_ANNOUNCEMENTS_KEY;
        Object message = memcacheService.get(announcementKey);
        if (message != null) {
            return new Announcement(message.toString());
        }
        return null;
    }

    /**
     * 
     * @param user
     * @param sessionForm
     * @param websafeConferenceKey
     * @return
     * @throws UnauthorizedException
     */
    @ApiMethod(name = "createSession", path = "session", httpMethod = HttpMethod.POST)
    public Session createSession(
                    final User user, 
                    final SessionForm sessionForm,
                    @Named("websafeConferenceKey") final String websafeConferenceKey
                   )throws UnauthorizedException, NotFoundException
    {
        //Verify user is logged in
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        
        //Verify conference key is a valid conference key
        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (conference == null) {
            throw new NotFoundException(String.format("Not conference found with key: %s", websafeConferenceKey));
        }
        
        //Check that user trying to create session was the same one that created the conference
        String userId = user.getUserId();
        Key<Profile> userProfilekey = Key.create(Profile.class, userId);
        
        Key<Profile> conferenceProfileKey = conference.getProfileKey();
        if(!conferenceProfileKey.equals(userProfilekey))
        {
            throw new UnauthorizedException("Only user who created conference can add sessions");
        }
     // Get the userId of the logged in User
        final long conferenceId = conference.getId();

        // Allocate a key for the session -- let App Engine allocate the ID
        // include the parent Conference in the allocated ID
        final Key<Session> sessionKey = factory().allocateId(conferenceKey, Session.class);

        // Get the session Id from the Key
        final long sessionId = sessionKey.getId();

        // Get the default queue
        final Queue queue = QueueFactory.getDefaultQueue();

        // Start a transaction
        Session session = ofy().transact(new Work<Session>() {

            @Override
            public Session run() {
                // Get user profile to get email
                Profile profile = ConferenceApi.getProfileFromUser(user);

                // Create a new Session Entity, specifying the conference's
                // entity as the parent of the session
                Session session = new Session(sessionId, conferenceId, sessionForm);

                // Save conference and Profile
                ofy().save().entity(session).now();

                // add send email to queue
                queue.add(ofy().getTransaction(),
                                TaskOptions.Builder.withUrl("/tasks/send_confirmation_email")
                                    .param("emailType", Constants.NEW_SESSION )
                                    .param("email", profile.getMainEmail())
                                    .param("conferenceInfo", session.toString())

                );
                return session;
            }
        });

        return session;
    }

    public List<Conference> filterPlayground() {
        Query<Conference> q = ofy().load().type(Conference.class);
        q = q.filter("city =", "London");
        q = q.filter("topics = ", "Medical Innovations");
        q = q.filter("month =", 6);
        q = q.filter("maxAttendees >", 10);
        q = q.order("maxAttendees");
        q = q.order("name");

        return q.list();

    }

}
