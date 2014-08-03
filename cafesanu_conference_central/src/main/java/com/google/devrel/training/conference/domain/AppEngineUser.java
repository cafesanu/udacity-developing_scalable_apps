package com.google.devrel.training.conference.domain;

import com.google.appengine.api.users.User;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.Key;

@Entity
public class AppEngineUser {

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
    
    /* **********************************************************************
     * CONSTRUCTORS
     * **********************************************************************
     */
    @Id
    private String email;
    private User   user;

    @SuppressWarnings("unused")
    private AppEngineUser() {
    }

    public AppEngineUser(User user) {
        this.user = user;
        this.email = user.getEmail();
    }
    /* **********************************************************************
     * OVERRIDES
     * **********************************************************************
     */
    
    /* **********************************************************************
     * SETTERS AND GETTERS FOR ATTRIBUTES
     * **********************************************************************
     */
    public Key<AppEngineUser> getKey() {
        return Key.create(AppEngineUser.class, email);
    }

    public User getUser() {
        return user;
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
