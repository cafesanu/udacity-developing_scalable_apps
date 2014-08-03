package com.google.devrel.training.conference.servlet;

import com.google.appengine.api.utils.SystemProperty;
import com.google.devrel.training.conference.Constants;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for sending a notification e-mail.
 */
@SuppressWarnings("serial")
public class SendConfirmationEmailServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(SendConfirmationEmailServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
                    throws ServletException, IOException {
        String emailType = request.getParameter("emailType");
        String emailInfo = "";
        String subject = "";
        String body = "";
        
        if (emailType.equals(Constants.NEW_CONFERENCE)) {
            emailInfo = request.getParameter("conferenceInfo");
            subject = "You created a new Conference!";
            body = "Hi, you have created a following conference.\n" + emailInfo;
        }
        else if (emailType.equals(Constants.NEW_SESSION)) {
            emailInfo = request.getParameter("sessionInfo");
            subject = "You created a new Session!";
            body = "Hi, you have created a following session.\n" + emailInfo;

        }

        String email = request.getParameter("email");

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        try {
            Message message = new MimeMessage(session);
            InternetAddress from = new InternetAddress(String.format("noreply@%s.appspotmail.com", SystemProperty.applicationId.get()), "Conference Central");
            message.setFrom(from);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email, ""));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        }
        catch (MessagingException e) {
            LOG.log(Level.WARNING, String.format("Failed to send an mail to %s", email), e);
            throw new RuntimeException(e);
        }
    }
}