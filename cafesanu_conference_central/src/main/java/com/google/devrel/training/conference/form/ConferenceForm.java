package com.google.devrel.training.conference.form;

import com.google.common.collect.ImmutableList;

import java.util.Date;
import java.util.List;

/**
 * A simple Java object (POJO) representing a Conference form sent from the client.
 */
public class ConferenceForm {

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
     * The city where the conference will take place.
     */
    private String city;

    /**
     * The description of the conference.
     */
    private String description;
    
    /**
     * The end date of the conference.
     */
    private Date endDate;
    
    /**
     * The capacity of the conference.
     */
    private int maxAttendees;
    
    /**
     * The name of the conference.
     */
    private String name;

    /**
     * The start date of the conference.
     */
    private Date startDate;

    /**
     * Topics that are discussed in this conference.
     */
    private List<String> topics;

    /* **********************************************************************
     * CONSTRUCTORS
     * **********************************************************************
     */

    @SuppressWarnings("unused")
    private ConferenceForm() {}

    /**
     * Public constructor is solely for Unit Test.
     * @param name
     * @param description
     * @param topics
     * @param city
     * @param startDate
     * @param endDate
     * @param maxAttendees
     */
    public ConferenceForm(String name, String description, List<String> topics, String city,
                          Date startDate, Date endDate, int maxAttendees) {
        this.name = name;
        this.description = description;
        this.topics = topics == null ? null : ImmutableList.copyOf(topics);
        this.city = city;
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
        this.maxAttendees = maxAttendees;
    }
    
    /* **********************************************************************
    * SETTERS AND GETTERS FOR ATTRIBUTES
    * **********************************************************************
    */
    
    /**
     * Getter for city.
     * 
     * @return city.
     */
    public String getCity() {
        return city;
    }
    
    /**
     * Getter for endDate.
     * 
     * @return endDate.
     */
    public Date getEndDate() {
        return endDate;
    }

    
    /**
     * Getter for maxAttendees.
     * 
     * @return maxAttendees.
     */
    public int getMaxAttendees() {
        return maxAttendees;
    }

    
    /**
     * Getter for startDate.
     * 
     * @return startDate.
     */
    public Date getStartDate() {
        return startDate;
    }

    
    /**
     * Getter for description.
     * 
     * @return description.
     */
    public String getDescription() {
        return description;
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
     * Getter for topics.
     * 
     * @return topics.
     */
    public List<String> getTopics() {
        return topics;
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
