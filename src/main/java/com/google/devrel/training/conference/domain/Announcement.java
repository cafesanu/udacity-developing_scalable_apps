package com.google.devrel.training.conference.domain;

/**
 * A simple wrapper for announcement message.
 */
public class Announcement {
    
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
    private String message;
    
   /* **********************************************************************
    * CONSTRUCTORS
    * **********************************************************************
    */
   
    public Announcement(){
        
    }
    
    public Announcement(String message)
    {
        this.message = message;
    }
    
    /* **********************************************************************
     * SETTERS AND GETTERS FOR ATTRIBUTES
     * **********************************************************************
     */
    
    public String getMessage(){
        return this.message;
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