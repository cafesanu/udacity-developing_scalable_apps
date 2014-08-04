package com.google.devrel.training.conference.domain;

import com.google.common.collect.ImmutableList;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.ArrayList;
import java.util.List;

@Entity
@Cache
public class Profile {

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
     * Keys of the conferences that this user registers to attend.
     */
    private List<String> conferenceKeysToAttend = new ArrayList<>(0);

    /**
     * Any string user wants us to display him/her on this system.
     */
    private String       displayName;

    /**
     * User's main e-mail address.
     */
    private String       mainEmail;

    /**
     * Keys of the session that this user wishes to attend.
     */
    private List<String> sessionKeysWishlist    = new ArrayList<>(0);

    /**
     * The user's tee shirt size. Options are defined as an Enum in ProfileForm
     */
    private TeeShirtSize teeShirtSize;

    /**
     * Use userId as the datastore key.
     */
    @Id
    private String       userId;

    /* **********************************************************************
     * CONSTRUCTORS
     * **********************************************************************
     */

    /**
     * Just making the default constructor private.
     */
    @SuppressWarnings("unused")
    private Profile() {
    }

    /**
     * Public constructor for Profile.
     * 
     * @param userId
     *            The user id, obtained from the email
     * @param displayName
     *            Any string user wants us to display him/her on this system.
     * @param mainEmail
     *            User's main e-mail address.
     * @param teeShirtSize
     *            The User's tee shirt size
     * 
     */
    public Profile(String userId, String displayName, String mainEmail, TeeShirtSize teeShirtSize) {
        this.userId = userId;
        this.displayName = displayName;
        this.mainEmail = mainEmail;
        this.teeShirtSize = teeShirtSize;
    }

    /* **********************************************************************
     * SETTERS AND GETTERS FOR ATTRIBUTES
     * **********************************************************************
     */

    /**
     * Getter for displayName
     * 
     * @return Immutable copy of conferenceKeysToAttend
     */
    public List<String> getConferenceKeysToAttend() {
        return ImmutableList.copyOf(conferenceKeysToAttend);
    }

    /**
     * Getter for displayName.
     * 
     * @return displayName.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Getter for mainEmail.
     * 
     * @return mainEmail.
     */
    public String getMainEmail() {
        return mainEmail;
    }

    /**
     * Getter for sessionKeysWishlist
     * 
     * @return Immutable copy of sessionKeysWishlist
     */
    public List<String> getSessionsInWhishlist() {
        return ImmutableList.copyOf(this.sessionKeysWishlist);
    }

    /**
     * Getter for teeShirtSize.
     * 
     * @return teeShirtSize.
     */
    public TeeShirtSize getTeeShirtSize() {
        return teeShirtSize;
    }

    /**
     * Getter for userId.
     * 
     * @return userId.
     */
    public String getUserId() {
        return userId;
    }

    /* **********************************************************************
     * PRIVATE METHODS
     * **********************************************************************
     */
    
    /* **********************************************************************
     * PUBLIC METHODS
     * **********************************************************************
     */ 

    /**
     * Adds a ConferenceId to conferenceIdsToAttend.
     *
     * The method initConferenceIdsToAttend is not thread-safe, but we need a
     * transaction for calling this method after all, so it is not a practical
     * issue.
     *
     * @param conferenceKey
     *            a websafe String representation of the Conference Key.
     */
    public void addConferenceKeysToAttend(String conferenceKey) {
        conferenceKeysToAttend.add(conferenceKey);
    }

    /**
     * Adds a SessionKey to sessionKeysWishlist
     * 
     * @param sessionKey
     *            a websage STriong representation of the Session Key.
     */
    public void addSessionKeyToWishlist(String sessionKey) {
        sessionKeysWishlist.add(sessionKey);
    }

    /**
     * Returns whether conferenceKey is in conferenceKeysToAttend
     * 
     * @param conferenceKey
     *            The conference key to check
     * 
     * @return true if conferenceKey is in conferenceKeysToAttend. False
     *         otherwise
     */
    public boolean isRegisteredForConference(String conferenceKey) {
        return conferenceKeysToAttend.contains(conferenceKey);
    }

    /**
     * Returns whether sessionKey is in sessionKeysWishlist
     * 
     * @param sessionKey
     *            The session key to check
     * 
     * @return true if sessionKey is in sessionKeysWishlist. False otherwise
     */
    public boolean isSessionInWishlist(String sessionKey) {
        return this.sessionKeysWishlist.contains(sessionKey);
    }

    /**
     * Removes sessionKey to sessionKeysWishlist if it exists.
     * 
     * @param sessionKey
     *            the session key to remove
     * 
     * @throws IllegalArgumentException
     *             When sessionKey is not in sessionKeysWishlist
     */
    public void removeSessionKeysToWishlist(String sessionKey) {
        if (sessionKeysWishlist.contains(sessionKey)) {
            sessionKeysWishlist.remove(sessionKey);
        }
        else {
            throw new IllegalArgumentException(String.format("Invalid sessionKey: %s", sessionKey));
        }
    }

    /**
     * Remove conferenceKey from ConferenceKeysToAttend
     * 
     * @param conferenceKey
     *            a websafe String representation of the Conference Key
     * 
     * @throws IllegalArgumentException
     *             When conferenceKey is not in conferenceKeysToAttend
     */
    public void unregisterFromConference(String conferenceKey) throws IllegalArgumentException {
        if (conferenceKeysToAttend.contains(conferenceKey)) {
            conferenceKeysToAttend.remove(conferenceKey);
        }
        else {
            throw new IllegalArgumentException(String.format("Invalid conferenceKey: %s", conferenceKey));
        }

    }

    /**
     * Updates the name and/or tee-shirt size of user
     * 
     * @param displayName
     *            the updated name
     * @param teeShirtSize
     *            the new tee-shirt size
     */
    public void update(String displayName, TeeShirtSize teeShirtSize) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (teeShirtSize != null) {
            this.teeShirtSize = teeShirtSize;
        }
    }

}
