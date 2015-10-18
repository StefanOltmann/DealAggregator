package de.stefan_oltmann.deals;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

@SuppressWarnings("serial")
public class MailHandlerServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger("MailHandlerServlet");

    public static final String FROM_ADDRESS = "deals@stefanoltmann.appspotmail.com";

    @SuppressWarnings("unchecked")
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        try {

            Session session = Session.getDefaultInstance(new Properties(), null);
            MimeMessage incomingMessage = new MimeMessage(session, req.getInputStream());

            /* Kann ich nichts mit anfangen. */
            if (incomingMessage.getFrom().length == 0) {
                LOGGER.log(Level.INFO, "Nachricht ohne FROM gedropped.");
                return;
            }

            InternetAddress absender = (InternetAddress) incomingMessage.getFrom()[0];

            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            Key aboKey = KeyFactory.createKey("Abo", absender.getAddress());

            if (incomingMessage.getSubject().equalsIgnoreCase("unsubscribe")) {
                datastore.delete(aboKey);
                sendMail(absender, "Eintrag gelöscht.", "Ihr Abo wurde beendet.", false);
                return;
            }

            Multipart multipart = (Multipart) incomingMessage.getContent();

            String textContent = null;

            for (int i = 0; i < multipart.getCount(); i++) {

                BodyPart bodyPart = multipart.getBodyPart(i);

                if (bodyPart.getContentType().startsWith("text/plain")
                        || bodyPart.getContentType().startsWith("TEXT/PLAIN"))
                    textContent = (String) bodyPart.getContent();
            }

            if (textContent == null) {
                sendMail(absender, "Format nicht verstanden.", "Das Format Ihrer E-Mail wurde leider nicht verstanden.", false);
                LOGGER.log(Level.INFO, "Nachricht in unpassenden Format erhalten: " + incomingMessage.getContentType());
                return;
            }

            Set<String> tags = new TreeSet<String>();
            for (String tag : textContent.split(",")) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty())
                    tags.add(trimmedTag);
            }

            if (tags.isEmpty()) {
                sendMail(absender, "Keine Tags gefunden.", "Bitte geben Sie Komma-separiert an, welche Schlüsselworte Sie interessieren.", false);
                return;
            }

            /* Speichern */

            {
                /* Bestehende Keys hinzufügen */

                Query query = new Query("Abo");
                query.setFilter(new Query.FilterPredicate("mail", FilterOperator.EQUAL, absender.getAddress()));

                Entity aboEntity = datastore.prepare(query).asSingleEntity();

                if (aboEntity != null)
                    tags.addAll((List<String>) aboEntity.getProperty("tags"));
            }

            Entity aboEntity = new Entity(aboKey);
            aboEntity.setProperty("mail", absender.getAddress());
            aboEntity.setProperty("name", absender.getPersonal());
            aboEntity.setProperty("tags", tags);
            datastore.put(aboEntity);

            StringBuilder sb = new StringBuilder();
            sb.append("Sie werden über das Aufkommen folgender Tags informiert:\n<ul>");
            for (String tag : tags)
                sb.append("<li>" + tag + "</li>\n");
            sb.append("</ul>\nZur Beendigung des Abos antworten Sie auf diese Mail mit dem Betreff 'unsubscribe'.");

            sendMail(absender, "Tags hinzugefügt.", sb.toString(), true);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Es ist ein Fehler aufgetreten.", e);
        }
    }

    public static void sendMail(InternetAddress toAdress, String subject, String text, boolean html)
            throws AddressException, MessagingException {

        Session session = Session.getDefaultInstance(new Properties(), null);

        Message replyMessage = new MimeMessage(session);
        replyMessage.setHeader("Content-Type", "text/plain; charset=UTF-8");
        replyMessage.setFrom(new InternetAddress(FROM_ADDRESS));
        replyMessage.addRecipient(Message.RecipientType.TO, toAdress);
        replyMessage.setSubject(subject);

        if (html)
            replyMessage.setContent(text, "text/html; charset=UTF-8");
        else
            replyMessage.setContent(text, "text/plain; charset=UTF-8");

        Transport.send(replyMessage);
    }

}