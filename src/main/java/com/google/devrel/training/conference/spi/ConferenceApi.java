package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.ofy;
import static com.google.devrel.training.conference.service.OfyService.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
import com.google.devrel.training.conference.form.SessionQueryForm;
import com.google.devrel.training.conference.utils.Time24HoursValidator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;

/**
 * Defines conference APIs.
 */
@Api(
    name = "conference", 
    version = "v1", 
    scopes = { Constants.EMAIL_SCOPE },
    clientIds = { 
                    Constants.WEB_CLIENT_ID,
                    Constants.API_EXPLORER_CLIENT_ID,
                    Constants.API_EXPLORER_CLIENT_ID
    }, 
    description = "API for the Conference Central Backend application.")
public class ConferenceApi {
    
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

    
   /* **********************************************************************
    * ATTRIBUTES
    * **********************************************************************
    */
    
   /* **********************************************************************
    * CONSTRUCTORS
    * **********************************************************************
    */
   
   /* **********************************************************************
    * OVERRIDES
    * **********************************************************************
    */
   
   /* **********************************************************************
    * SETTERS AND GETTERS FOR ATTRIBUTES
    * **********************************************************************
    */
   
   /* **********************************************************************
    * PRIVATE METHODS
    * **********************************************************************
    */
    /**
     * Adds a speaker announcement to memcache if speaker will talk at more than
     * one session in the conference
     * 
     * @param conferenceKey 
     *              the Conference key
     * @param conference
     *              The conference Object
     * @param speaker
     *            the speaker to check to be added to memcache
     */
    private void checkForSpeakerAnnouncement(Key<Conference> conferenceKey, Conference conference, String speaker) {
        // Check if speaker has more than one session. If yes, add announcement

        List<Session> conferenceSessionsWithSpeaker = ofy().load().type(Session.class)
                                                        .ancestor(conferenceKey)
                                                        .filter("speaker =", speaker)
                                                        .list();
        
        if (conferenceSessionsWithSpeaker.size() > 1) {
            StringBuilder speakerAnnouncementStringBuilder = new StringBuilder("Good news! ")
                                                                    .append(speaker)
                                                                    .append(" is having the following sessions at conference ")
                                                                    .append(conference.getName())
                                                                    .append(".");
            
            for (Session s : conferenceSessionsWithSpeaker) {
                speakerAnnouncementStringBuilder.append(" • ")
                                                .append(s.getName());
            }

            // Get the MemCache Service
            MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();

            // Put the speaker announcement String in memcache
            String announcementText = speakerAnnouncementStringBuilder.toString();

            memcacheService.put(Constants.MEMCACHE_FEATURED_SPEAKER_KEY, announcementText);

        }

    }
    
    /**
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     *
     * @param email the email
     * 
     * @return display name from the user's email
     */
    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    /**
     * Gets the Profile entity for the current user or creates it if it doesn't
     * exist
     * 
     * @param user the logged-in user
     * @return user's Profile
     */
    private static Profile getProfileFromUser(User user) {
        // First fetch the user's Profile from the datastore.
        Key<Profile> profileKey = Key.create(Profile.class, user.getUserId());
        Profile profile = ofy().load().key(profileKey).now();
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
   
   /* **********************************************************************
    * PUBLIC METHODS
    * **********************************************************************
    */  

    /**
     * Add session to user's wishlist
     * 
     * @param user
     *            the logged-in user
     * @param websafeSessionKey
     *            the session key whose user want to add to Wishlist
     * @return true if session was added to wishlist, false otherwise
     * @throws UnauthorizedException
     *             when the user is not signed in.
     * @throws NotFoundException
     *             when there is no Session with the given Session key.
     * @throws ForbiddenException
     *             if the key cannot be created
     * @throws ConflictException
     *             if user has not register to conference first
     */
    @ApiMethod(
        name = "addSessionToWishlist",
        path = "session/{websafeSessionKey}/wishlist",
        httpMethod = HttpMethod.POST
    )
    public WrappedBoolean addSessionToWishlist(final User user, @Named("websafeSessionKey") final String websafeSessionKey) 
                    throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException 
    {

        // if not signed in, throw a 401 error
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        
        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {

            @Override
            public WrappedBoolean run() {
                try {
                    // Get the Session key -- you can get it from websafeCongerenceKey
                    // Will throw IllegalArgumentException if the key cannot be created
                    Key<Session> sessionKey = Key.create(websafeSessionKey);
                    Key<Conference> conferenceParentKey = sessionKey.getParent();

                    // Get the user's profile entity
                    Profile profile = getProfile(user);

                    // User must be register to session's conference in order to
                    // add session to Wishlist
                    if (!profile.isRegisteredForConference(conferenceParentKey.getString())) {
                        return new WrappedBoolean(false, String.format("Please register to the conference first"));
                    }

                    // Get the Session entity for the datastore
                    Session Session = ofy().load().key(sessionKey).now();

                    // 404 when there is no Session with the given SessionKey
                    if (Session == null) {
                        return new WrappedBoolean(false, String.format("No session found with Key: %s", websafeSessionKey));
                    }

                    // Has the user already added the session to the wishlist?
                    if (profile.isSessionInWishlist(websafeSessionKey)) {
                        return new WrappedBoolean(false, "Session already in Wishlist");
                    }
                    else {
                        // All looks good, go ahead and add session to Wishlist

                        // Add the websafeSessionKey to the profile's  sessionKeysWishlist property

                        profile.addSessionKeyToWishlist(websafeSessionKey);

                        // Save the Profile entity
                        ofy().save().entity(profile).now();

                        // We are booked!
                        return new WrappedBoolean(true, "Session succesfully added to Wishlist");

                    }

                }
                catch (IllegalArgumentException e) {
                    return new WrappedBoolean(false, "No session found with Key");
                }
                catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");
                }
            }

        });

        if (!result.getResult()) {
            String failReason = result.getReason();
            if (failReason.contains("No session found with Key")) {
                throw new NotFoundException(result.getReason());
            }
            else if (failReason.equals("Session already in Wishlist")) {
                throw new ConflictException("Session already in Wishlist");
            }
            else if (failReason.equals("Please register to the conference first")) {
                throw new ConflictException("Please register to the conference first");
            }
        }
        return result;
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
    @ApiMethod(
        name = "createConference", 
        path = "conference", 
        httpMethod = HttpMethod.POST
    )
    public Conference createConference(final User user, final ConferenceForm conferenceForm) 
                    throws UnauthorizedException 
    {
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
                // is one. Otherwise create a new Profile entity with default values
                Profile profile = ConferenceApi.getProfileFromUser(user);

                // Create a new Conference Entity, specifying the user's
                // Profile entity as the parent of the conference
                Conference conference = new Conference(conferenceId, userId, conferenceForm);

                // Save conference and Profile
                ofy().save().entities(profile, conference).now();

                // add send email to queue
                queue.add(  ofy().getTransaction(), 
                            TaskOptions.Builder
                                .withUrl("/tasks/send_confirmation_email")
                                .param("emailType", Constants.NEW_CONFERENCE)
                                .param("email", profile.getMainEmail())
                                .param("conferenceInfo", conference.toString())

                );
                return conference;
            }
        });
        return conference;
    }
    
    /**
     * 
     * @param user
     *            An user who invokes this method, null when the user is not
     *            signed in.
     * @param sessionForm
     *            A SessionForm object representing user's inputs.
     * @param websafeConferenceKey
     *            The String representation of the Conference Key to unregister
     *            from.
     * @return A newly created Session Object.
     * @throws UnauthorizedException
     *             when the user is not signed in.
     */
    @ApiMethod(
        name = "createSession", 
        path = "session", 
        httpMethod = HttpMethod.POST
    )
    public Session createSession(final User user, final SessionForm sessionForm, @Named("websafeConferenceKey") final String websafeConferenceKey)
                    throws UnauthorizedException, NotFoundException 
    {
        // Verify user is logged in
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Verify conference key is a valid conference key
        final Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (conference == null) {
            throw new NotFoundException(String.format("Not conference found with key: %s", websafeConferenceKey));
        }

        // Check that user trying to create session was the same one that
        // created the conference
        String userId = user.getUserId();
        Key<Profile> userProfilekey = Key.create(Profile.class, userId);

        Key<Profile> conferenceProfileKey = conference.getProfileKey();
        if (!conferenceProfileKey.equals(userProfilekey)) {
            throw new UnauthorizedException("Only user who created conference can add sessions");
        }

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
                Session session = new Session(sessionId, conferenceKey, sessionForm);

                // Save sessions
                ofy().save().entity(session).now();

                // add send email to queue
                queue.add(ofy().getTransaction(), TaskOptions.Builder.withUrl("/tasks/send_confirmation_email").param("emailType", Constants.NEW_SESSION)
                                .param("email", profile.getMainEmail()).param("sessionInfo", session.toString())

                );
                return session;
            }
        });

        this.checkForSpeakerAnnouncement(conferenceKey, conference, sessionForm.getSpeaker());

        return session;
    }

    /**
     * Method to test queries
     * 
     * @return the query list
     */
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

    /**
     * Delete session from user's wishlist
     * 
     * @param user
     *            the logged-in user
     * @param websafeSessionKey
     *            the session key whose user want to add to Wishlist
     * @return true if session was added to Wishlist, false otherwise
     * @throws UnauthorizedException
     *             when the user is not signed in.
     * @throws NotFoundException
     *             when there is no Session with the given Session key.
     * @throws ForbiddenException
     *             if the key cannot be created
     */
    @ApiMethod(
        name = "deleteSessionFromWishlist",
        path = "session/{websafeSessionKey}/wishlist",
        httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean deleteSessionFromWishlist(final User user, @Named("websafeSessionKey") final String websafeSessionKey) 
                    throws UnauthorizedException, NotFoundException, ForbiddenException 
    {
        // if not signed in, throw a 401 error
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {

            @Override
            public WrappedBoolean run() {
                try {
                    // Get the Session key
                    // Will throw ForbiddenException if the key cannot be created
                    Key<Session> sessionKey = Key.create(websafeSessionKey);

                    // Get the session entity for the datastore
                    Session session = ofy().load().key(sessionKey).now();

                    // 404 when there is no Session with the given sessionKey
                    if (session == null) {
                        return new WrappedBoolean(false, String.format("No sesssion found with Key: %s", websafeSessionKey));
                    }

                    // Get the user's profile entity
                    Profile profile = getProfile(user);

                    // Has the user not already added session to wishlist?
                    if (!profile.isSessionInWishlist(websafeSessionKey)) {
                        return new WrappedBoolean(false, "Session is not on your wishlist");
                    }
                    else {
                        // All looks good, go ahead and delete session from
                        // wishlist

                        profile.removeSessionKeysToWishlist(websafeSessionKey);

                        // Save the Profile entity
                        ofy().save().entities(profile).now();

                        // Successfully removed session from wishlist!
                        return new WrappedBoolean(true);
                    }
                }
                catch (IllegalArgumentException e) {
                    return new WrappedBoolean(false, "No session found with Key");
                }
                catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");
                }
            }

        });

        if (!result.getResult()) {
            String failReason = result.getReason();
            if (failReason.contains("No sesssion found with Key")) {
                throw new NotFoundException(result.getReason());
            }
            else if (failReason.equals("Session is not on your wishlist")) {
                throw new ForbiddenException(result.getReason());
            }
        }
        return result;
    }

    /**
     * Gets last announcement from Memcache whose Memcache key is Constants.MEMCACHE_ANNOUNCEMENTS_KEY
     * @return a string representing the message
     */
    @ApiMethod(
        name = "getAnnouncement",
        path = "announcement",
        httpMethod = HttpMethod.GET
    )
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
     * Returns a conference object with given conferenceId
     * 
     * @param websafeConferenceKey
     *            The Siring representation of the Conference Key
     * @return a Conference Object with Given conferenceId
     * @throws NotFoundException
     *             when there is no conference with the given conferenceID
     */
    @ApiMethod(
        name = "getConference",
        path = "conference/{websafeConferenceKey}",
        httpMethod = HttpMethod.GET
    )
    public Conference getConference(@Named("websafeConferenceKey") final String websafeConferenceKey) 
                    throws NotFoundException 
    {
        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (conference == null) {
            throw new NotFoundException(String.format("Not conference found with key: %s", websafeConferenceKey));
        }
        return conference;
    }

    /**
     * Returns a list of sessions the belong to a conference. In order to
     * receive the websafeConferenceKey via the JSON params, uses a POST method.
     *
     * @param websafeConferenceKey
     *            The conference key which the user wants its sessions
     * @return a list of Sessions that belong to the conference.
     */
    @ApiMethod(name = "getConferenceSessions", path = "getConferenceSessions", httpMethod = HttpMethod.POST)
    public List<Session> getConferenceSessions(@Named("websafeConferenceKey") final String websafeConferenceKey) {

        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Query<Session> q = ofy().load().type(Session.class).ancestor(conferenceKey);
        return q.list();

    }

    /**
     * Returns a list of sessions the belong to a conference with the specified
     * speaker. In order to receive the websafeConferenceKey via the JSON
     * params, uses a POST method.
     *
     * @param websafeConferenceKey
     *            The conference key which the user wants its sessions
     * @param speaker
     *            The speaker
     * @return a list of Session that belong to the conference with the specified speaker.
     */
    @ApiMethod(
        name = "getConferenceSessionsBySpeaker",
        path = "getConferenceSessionsBySpeaker",
        httpMethod = HttpMethod.POST
    )
    public List<Session> getConferenceSessionsBySpeaker(@Named("websafeConferenceKey") final String websafeConferenceKey, @Named("speaker") final String speaker) {
        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

        Query<Session> q = ofy().load().type(Session.class)
                                        .ancestor(conferenceKey)
                                        .filter("speaker =", speaker);
        return q.list();
    }

    /**
     * Returns a list of sessions the belong to a conference with the specified
     * session type. In order to receive the websafeConferenceKey via the JSON
     * params, uses a POST method.
     *
     * @param websafeConferenceKey
     *            The conference key which the user wants its sessions
     * @param sessionType
     *            The session type
     * @return a list of Session that belong to the conference with the
     *         specified session type.
     */
    @ApiMethod(name = "getConferenceSessionsByType", path = "getConferenceSessionsByType", httpMethod = HttpMethod.POST)
    public List<Session> getConferenceSessionsByType(
                            @Named("websafeConferenceKey") final String websafeConferenceKey, 
                            @Named("sessionType") final String sessionType
    )
    {
        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Query<Session> q = ofy().load().type(Session.class)
                                        .ancestor(conferenceKey)
                                        .filter("type =", sessionType);
        return q.list();
    }

    /**
     * Return a list of session given a queryForm with Filters
     * 
     * @param sessionQueryForm
     *            the form containing the filters
     * @return A list of filtered sessions
     */
    @ApiMethod(
        name = "getConferenceSessionsQueryForm", 
        path = "getConferenceSessionsQueryForm",
        httpMethod = HttpMethod.POST
    )
    public List<Session> getConferenceSessionsQueryForm(SessionQueryForm sessionQueryForm) {

        Iterable<Session> conferenceIterable = sessionQueryForm.getQuery();
        List<Session> result = new ArrayList<>(0);
        List<Key<Conference>> conferencesKeyList = new ArrayList<>(0);
        for (Session s : conferenceIterable) {
            conferencesKeyList.add(Key.create(Conference.class, s.getConferenceKey().getId()));
            result.add(s);
        }
        // To avoid separate datastore gets for each conference, pre-fetch Profiles
        ofy().load().keys(conferencesKeyList);

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
    @ApiMethod(
        name = "getConferencesCreated",
        path = "getConferencesCreated",
        httpMethod = HttpMethod.POST
    )
    public List<Conference> getConferencesCreated(final User user)
                    throws UnauthorizedException
    {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        Key<Profile> profileKey = Key.create(Profile.class, user.getUserId());
        Query<Conference> q = ofy().load().type(Conference.class).ancestor(profileKey);
        return q.list();
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
    @ApiMethod(
        name = "getConferencesToAttend",
        path = "getConferencesToAttend", 
        httpMethod = HttpMethod.GET
    )
    public Collection<Conference> getConferencesToAttend(final User user) 
                    throws UnauthorizedException, NotFoundException
    {
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
     * Gets an announcement announcing all speakers talking on more than one
     * session
     * 
     * @return String with announcing all speakers talking on more than one
     *         session, null if there are no featured speaker
     * 
     */
    @ApiMethod(
        name = "getFeaturedSpeaker",
        path = "getFeaturedSpeaker", 
        httpMethod = HttpMethod.GET
    )
    public Announcement getFeaturedSpeaker() {

        MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
        String announcementKey = Constants.MEMCACHE_FEATURED_SPEAKER_KEY;
        Object message = memcacheService.get(announcementKey);
        if (message != null) {
            return new Announcement(message.toString());
        }
        return null;
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
    @ApiMethod(
        name = "getProfile", 
        path = "profile", 
        httpMethod = HttpMethod.GET
    )
    public Profile getProfile(final User user) 
                    throws UnauthorizedException 
    {
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
     * Returns a list of sessions which type is different than type whit time
     * less than time
     *
     * @param type
     *            The session type
     * @param time
     *            The time before 
     * @return a list of sessions which type is different than type whit time
     * less than time
     */
    @ApiMethod(
        name = "getSessionsBeforeTimeOtherThanType",
        path = "getSessionsBeforeTimeOtherThanType",
        httpMethod = HttpMethod.POST
    )
    public List<Session> getSessionsBeforeTimeOtherThanType(@Named("type") final String type, @Named("time") final String time) 
    {
        Query<Session> q = ofy().load().type(Session.class)
                                    .filter("time <", time);
        List<Session> sessionsBeforeTime = q.list();
        List<Session> sessionsBeforeTimeOtherThanType = new ArrayList<>(0);
        for (Session s : sessionsBeforeTime) {
            if (!s.getType().equals(type)) {
                sessionsBeforeTimeOtherThanType.add(s);
            }
        }
        return sessionsBeforeTimeOtherThanType;
    }

    /**
     * Returns a list of sessions with the specified date. In order to receive
     * the websafeConferenceKey via the JSON params, uses a POST method.
     *
     * @param date
     *            The date session starts
     * @return a list of Session with the specified date
     */
    @ApiMethod(
        name = "getSessionsByDate", 
        path = "getSessionsByDate",
        httpMethod = HttpMethod.POST
    )
    public List<Session> getSessionsByDate(@Named("date") final Date date) {
        Query<Session> q = ofy().load().type(Session.class)
                                        .filter("date =", date);
        return q.list();
    }

    /**
     * Returns a list of sessions within the specified time. In order to
     * receive the websafeConferenceKey via the JSON params, uses a POST method.
     *
     * @param after
     *            The minimum start time
     * @param before
     *            The maximum start time
     *
     * @throws IllegalArgumentException
     *            If times are not in HH:MM format
     *            
     * @return a list of Session with the specified time
     */
    @ApiMethod(
        name = "getSessionsByTimeRange",
        path = "getSessionsByTimeRange",
        httpMethod = HttpMethod.POST
    )
    public List<Session> getSessionsByTimeRange(@Named("after") final String after, @Named("before") final String before) 
                    throws IllegalArgumentException
    {

        boolean validAfter = Time24HoursValidator.validate(after);
        boolean validBefore = Time24HoursValidator.validate(before);
        if(!validAfter || !validBefore ){
            throw new IllegalArgumentException("Time error. Enter times in format HH:MM.");
        }
        Query<Session> q = ofy().load().type(Session.class)
                                        .filter("time >=", after)
                                        .filter("time <=", before);
        return q.list();
    }

    /**
     * Returns a collection of Sessions that the user has in the whishlist
     *
     * @param user
     *            An user who invokes this method, null when the user is not
     *            signed in.
     * 
     * @return A collection of Sessions that the user has in the whishlist
     * 
     * @throws UnauthorizedException
     *             when the User object is null.
     * @throws NotFoundException
     *             when the profile doesn'e exist.
     */
    @ApiMethod(
        name = "getSessionsInWishlist",
        path = "getSessionsInWishlist", 
        httpMethod = HttpMethod.GET
    )
    public Collection<Session> getSessionsInWishlist(final User user) 
                    throws UnauthorizedException, NotFoundException
    {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        String userId = user.getUserId();
        Key<Profile> key = Key.create(Profile.class, userId);
        Profile profile = ofy().load().key(key).now();

        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist");
        }

        // Get the value of the Profile's keyStringsInWhishlist property
        List<String> keyStringsInWhishlist = profile.getSessionsInWhishlist();

        // Iterate over keyStringsInWhishlist, end return a Collection of the
        // Session entities that the user has added to wishlist
        List<Key<Session>> keysInWishlist = new ArrayList<>(0);
        for (String k : keyStringsInWhishlist) {
            Key<Session> sessionKey = Key.create(k);
            keysInWishlist.add(sessionKey);
        }
        return ofy().load().keys(keysInWishlist).values();
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
    @ApiMethod(
        name = "queryConferences",
        path = "queryConferences",
        httpMethod = HttpMethod.POST
    )
    public List<Conference> queryConferences(ConferenceQueryForm conferenceQueryForm) {

        Iterable<Conference> conferenceIterable = conferenceQueryForm.getQuery();
        List<Conference> result = new ArrayList<>(0);
        List<Key<Profile>> organizersKeyList = new ArrayList<>(0);
        for (Conference c : conferenceIterable) {
            organizersKeyList.add(Key.create(Profile.class, c.getOrganizerUserId()));
            result.add(c);
        }
        // To avoid separate datasotre gets for each conference, pre-fetch Profiles
        ofy().load().keys(organizersKeyList);

        return result;
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
    @ApiMethod(
        name = "registerForConference",
        path = "conference/{websafeConferenceKey}/registration",
        httpMethod = HttpMethod.POST
    )
    public WrappedBoolean registerForConference(final User user, @Named("websafeConferenceKey") final String websafeConferenceKey)
                    throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException 
    {

        // if not signed in, throw a 401 error
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {

            @Override
            public WrappedBoolean run() {
                try {
                    // Get the Conference key -- you can get it from websafeCongerenceKey
                    // Will throw ForbiddenException if the key cannot be created
                    Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                    // Get the Conference entity for the datastore
                    Conference conference = ofy().load().key(conferenceKey).now();

                    // 404 when there is no Conference with the given ConferenceId
                    if (conference == null) {
                        return new WrappedBoolean(false, String.format("No conference found with Key: %s", websafeConferenceKey));
                    }

                    // Get the user's profile entity
                    Profile profile = getProfile(user);

                    // Has the user already registered to attend this conference?
                    if (profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
                        return new WrappedBoolean(false, "Already Registered");
                    }
                    else if (conference.getSeatsAvailable() <= 0) {
                        return new WrappedBoolean(false, "No seats available");
                    }
                    else {
                        // All looks good, go ahead and book the seat

                        // Add the websafeConferenceKey to the proifile's conferencesToAttend property

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
    @ApiMethod(
        name = "saveProfile", 
        path = "profile", 
        httpMethod = HttpMethod.POST
    )
    public Profile saveProfile(final User user, ProfileForm profileForm) 
                    throws UnauthorizedException 
    {

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
     * Unregister from the specified Conference.
     *
     * @param user
     *            An user who invokes this method, null when the user is not
     *            signed in.
     * @param websafeConferenceKey
     *            The String representation of the Conference Key to unregister
     *            from.
     * @return WrappedBoolean true when success, otherwise false with error message
     * @throws UnauthorizedException
     *             when the user is not signed in.
     * @throws NotFoundException
     *             when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
        name = "unregisterFromConference",
        path = "conference/websafeConferenceKey/registration",
        httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean unregisterFromConference(final User user, @Named("websafeConferenceKey") final String websafeConferenceKey)
                    throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // if not signed in, throw a 401 error
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {

            @Override
            public WrappedBoolean run() {
                try {
                    // Get the Conference key -- you can get it from websafeCongerenceKey
                    // Will throw ForbiddenException if the key cannot be created
                    Key<Conference> conferenceKey = Key.create(websafeConferenceKey);

                    // Get the Conference entity for the datastore
                    Conference conference = ofy().load().key(conferenceKey).now();

                    // 404 when there is no Conference with the given  ConferenceId
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

                        // Add the websafeConferenceKey to the proifile's conferencesToAttend property

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
}
