package com.google.devrel.training.conference.form;

import static com.google.devrel.training.conference.service.OfyService.ofy;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.collect.ImmutableList;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Session;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A simple Java object (POJO) representing a query options for Conference.
 */
public class SessionQueryForm {

    /* **********************************************************************
     * CONSTANTS
     * **********************************************************************
     */
    private static final Logger LOG = Logger.getLogger(ConferenceQueryForm.class.getName());

    /* **********************************************************************
     * ENUMS
     * **********************************************************************
     */ 
    /**
     * Enum representing a field type. For now it's only a string but for extensibility I leave it here
     */
    public static enum FieldType {
        STRING
    }

    /**
     * Enum representing a field.
     */
    public static enum Field {
        CONFERENCE_KEY("conferenceKey", FieldType.STRING), 
        TYPE("type", FieldType.STRING), 
        SPEAKER("speaker", FieldType.STRING),
        TIME("time", FieldType.STRING);

        private String    fieldName;

        private FieldType fieldType;

        private Field(String fieldName, FieldType fieldType) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
        }

        private String getFieldName() {
            return this.fieldName;
        }
    }

    /**
     * Enum representing an operator.
     */
    public static enum Operator {
        EQ("=="), LT("<"), GT(">"), LTEQ("<="), GTEQ(">="), NE("!=");

        private String queryOperator;

        private Operator(String queryOperator) {
            this.queryOperator = queryOperator;
        }

        private String getQueryOperator() {
            return this.queryOperator;
        }

        private boolean isInequalityFilter() {
            return this.queryOperator.contains("<") || this.queryOperator.contains(">") || this.queryOperator.contains("!");
        }
    }

    /* **********************************************************************
     * INNER CLASSES
     * **********************************************************************
     */
    /**
     * A class representing a single filter for the query.
     */
    public static class Filter {

        private Field    field;
        private Operator operator;
        private String   value;

        public Filter() {
        }

        public Filter(Field field, Operator operator, String value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }

        public Field getField() {
            return field;
        }

        public Operator getOperator() {
            return operator;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            // If the object is compared with itself then return true
            if (o == this) {
                return true;
            }
            /*
             * Check if o is an instance of Complex or not
             * "null instanceof [type]" also returns false
             */
            if (!(o instanceof Filter)) {
                return false;
            }
            Filter ob = (Filter) o;

            return ob.getField().equals(Field.CONFERENCE_KEY);
        }
    }
    
    /* **********************************************************************
     * ATTRIBUTES
     * **********************************************************************
     */

    /**
     * A list of query filters.
     */
    private List<Filter> filters = new ArrayList<>(0);

    /**
     * Holds the first inequalityFilter for checking the feasibility of the
     * whole query.
     */
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Filter       inequalityFilter;

    /* **********************************************************************
     * CONSTRUCTORS
     * **********************************************************************
     */
    public SessionQueryForm() {
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
     * Getter for filters.
     * 
     *
     * @return The List of filters.
     */
    public List<Filter> getFilters() {
        return ImmutableList.copyOf(filters);
    }

    /* **********************************************************************
     * PRIVATE METHODS
     * **********************************************************************
     */
    
    /**
     * Checks the feasibility of the whole query.
     */
    private void checkFilters() {
        for (Filter filter : this.filters) {
            if (filter.operator.isInequalityFilter()) {
                // Only one inequality filter is allowed.
                if (inequalityFilter != null && !inequalityFilter.field.equals(filter.field)) {
                    throw new IllegalArgumentException("Inequality filter is allowed on only one field.");
                }
                inequalityFilter = filter;
            }
        }
    }
    
    /* **********************************************************************
     * PUBLIC METHODS
     * **********************************************************************
     */  

    /**
     * Adds a query filter.
     *
     * @param filter
     *            A Filter object for the query.
     * @return this for method chaining.
     */
    public SessionQueryForm filter(Filter filter) {
        if (filter.operator.isInequalityFilter()) {
            // Only allows inequality filters on a single field.
            if (inequalityFilter != null && !inequalityFilter.field.equals(filter.field)) {
                throw new IllegalArgumentException("Inequality filter is allowed on only one field.");
            }
            inequalityFilter = filter;
        }
        filters.add(filter);
        return this;
    }

    /**
     * Returns an Objectify Query object for the specified filters.
     *
     * @return an Objectify Query.
     */
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Query<Session> getQuery() {
        // First check the feasibility of inequality filters.
        this.checkFilters();

        Query<Session> query = ofy().load().type(Session.class);

        // If conference key was passed, delete it from list of filters and get
        // sessions which ancestor is the conference key
        int conferenceKeyIndex = this.filters.indexOf(new Filter(Field.CONFERENCE_KEY, Operator.EQ, ""));
        if (conferenceKeyIndex >= 0) {
            Filter conferenceKeyFilter = this.filters.remove(conferenceKeyIndex);
            Key<Conference> conferenceKey = Key.create(conferenceKeyFilter.getValue());
            query = query.ancestor(conferenceKey);
        }

        if (inequalityFilter == null) {
            // Order by name.
            query = query.order("name");
        }
        else {
            // If we have any inequality filters, order by the field first.
            query = query.order(inequalityFilter.field.getFieldName());
            query = query.order("name");
        }
        
        for (Filter filter : this.filters) {
            // Applies filters in order.
            //For now, there is only STRING FieldTypes, so the if statement will always be true,
            //However for extensibility I leave it like this in case more field types are added
            if (filter.field.fieldType == FieldType.STRING) {
                query = query.filter(String.format("%s %s", filter.field.getFieldName(), filter.operator.getQueryOperator()), filter.value);
            }
        }
        LOG.info(query.toString());
        return query;
    }
}
