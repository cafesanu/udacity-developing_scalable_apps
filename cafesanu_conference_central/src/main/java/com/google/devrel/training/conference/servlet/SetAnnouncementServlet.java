package com.google.devrel.training.conference.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.repackaged.com.google.common.base.Joiner;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Conference;

import static com.google.devrel.training.conference.service.OfyService.ofy;

/**
 * A servlet for putting announcements in memcache. This announcement announces
 * conferences that are nearly sold out (defined as having 1-5 seats left)
 *
 */

@SuppressWarnings("serial")
public class SetAnnouncementServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Query for conferences with less than 5 seats left
        Iterable<Conference> conferences = ofy().load().type(Conference.class).filter("seatsAvailable <", 6).filter("seatsAvailable >", 0);

        // Get the names of the nearly sold out conferences
        List<String> conferenceNames = new ArrayList<>(0);
        for (Conference c : conferences) {
            conferenceNames.add(c.getName());
        }

        if (conferenceNames.size() > 0) {
            StringBuilder announcementStringBuilder = new StringBuilder("Oh look! Last chance to attend! The following conferences are nearly sold out: ");
            
            Joiner joiner = Joiner.on(", ").skipNulls();
            announcementStringBuilder.append(joiner.join(conferenceNames));
            
            //Get the MemCache Service
            MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
            
            //Put the announcement String in memcache,
            //keyed by Constants.MEMCACHE_ANNOUNCEMENTS_KEY
            String announcementKey = Constants.MEMCACHE_ANNOUNCEMENTS_KEY;
            String announcementText = announcementStringBuilder.toString();
            
            memcacheService.put(announcementKey, announcementText);
            
        }
        
        //Set the response status to 204, which means
        //the request was successful but there's no data to send back
        //Browser stays on the same page if the get came from the browser
        response.setStatus(204);
    }

}
