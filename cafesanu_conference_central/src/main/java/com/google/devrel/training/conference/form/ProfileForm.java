package com.google.devrel.training.conference.form;

/**
 * Pojo representing a profile form on the client side.
 */
public class ProfileForm {

    /* **********************************************************************
     * CONSTANTS
     * **********************************************************************
     */

    /* **********************************************************************
     * ENUMS
     * **********************************************************************
     */

    public static enum TeeShirtSize {
        NOT_SPECIFIED, XS, S, M, L, XL, XXL, XXXL
    }

    /* **********************************************************************
     * INNER CLASSES
     * **********************************************************************
     */

    /* **********************************************************************
     * ATTRIBUTES
     * **********************************************************************
     */
    /**
     * Any string user wants us to display him/her on this system.
     */
    private String       displayName;

    /**
     * T shirt size.
     */
    private TeeShirtSize teeShirtSize;

    /* **********************************************************************
     * CONSTRUCTORS
     * **********************************************************************
     */
    @SuppressWarnings("unused")
    private ProfileForm() {
    }

    /**
     * Constructor for ProfileForm, solely for unit test.
     * 
     * @param displayName
     *            A String for displaying the user on this system.
     * @param notificationEmail
     *            An e-mail address for getting notifications from this system.
     */
    public ProfileForm(String displayName, TeeShirtSize teeShirtSize) {
        this.displayName = displayName;
        this.teeShirtSize = teeShirtSize;
    }

    /* **********************************************************************
     * OVERRIDES
     * **********************************************************************
     */

    /* **********************************************************************
     * SETTERS AND GETTERS FOR ATTRIBUTES
     * **********************************************************************
     */

    /**
     * Getter for displayName.
     * 
     * @return displayName.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Getter for teeShirtSize.
     * 
     * @return teeShirtSize.
     */
    public TeeShirtSize getTeeShirtSize() {
        return teeShirtSize;
    }
}
