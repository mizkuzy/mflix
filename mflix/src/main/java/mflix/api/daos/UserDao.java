package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

    public static final String USER_ID_FIELD = "user_id";
    public static final String USER_EMAIL_FIELD = "email";
    public static final String USER_FIELD_PREFERENCES = "preferences";
    private final MongoCollection<User> usersCollection;
    //DONE> Ticket: User Management - do the necessary changes so that the sessions collection
    //returns a Session object
    private final MongoCollection<Session> sessionsCollection;

    private final Logger log;

    @Autowired
    public UserDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
        log = LoggerFactory.getLogger(this.getClass());
        //DONE> Ticket: User Management - implement the necessary changes so that the sessions
        // collection returns a Session objects instead of Document objects.
        sessionsCollection = db.getCollection("sessions", Session.class).withCodecRegistry(pojoCodecRegistry);
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) {
        //DONE > Ticket: Durable Writes -  you might want to use a more durable write concern here!
        log.debug("Add a user {}", user);

        //DONE > Ticket: Handling Errors - make sure to only add new users
        // and not users that already exist.
        if (getUser(user.getEmail()) != null) {
            return true;
        }

        try {
            usersCollection.withWriteConcern(WriteConcern.MAJORITY).insertOne(user);
        } catch (MongoWriteException e) {
            log.debug("Adding user {} was failed", user.getEmail());
            return false;
        }

        return true;
    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {
        //DONE> Ticket: User Management - implement the method that allows session information to be
        // stored in it's designated collection.
        Session session = new Session();
        session.setJwt(jwt);
        session.setUserId(userId);

        //DONE > Ticket: Handling Errors - implement a safeguard against
        // creating a session with the same jwt token.
        if (getUserSession(userId) != null) {
            return true;
        }

        try {
            sessionsCollection.insertOne(session);
        } catch (MongoWriteException e) {
            log.debug("Session for user {} already exists", userId);

            return false;
        }

        return true;
    }

    /**
     * Returns the User object matching the an email string value.
     *
     * @param email - email string to be matched.
     * @return User object or null.
     */
    public User getUser(String email) {
        log.debug("Get user with email {}", email);
        //DONE> Ticket: User Management - implement the query that returns the first User object.
        return usersCollection.find(eq(USER_EMAIL_FIELD, email)).first();
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        // DONE> Ticket: User Management - implement the method that returns Sessions for a given
        // userId

        return sessionsCollection.find(eq(USER_ID_FIELD, userId)).first();
    }

    public boolean deleteUserSessions(String userId) {
        //DONE> Ticket: User Management - implement the delete user sessions method
        DeleteResult deleteResult = sessionsCollection.deleteOne(eq(USER_ID_FIELD, userId));
        long deletedCount = deleteResult.getDeletedCount();
        log.debug("Number of deleted Sessions for user {}: {}", userId, deletedCount);

        return deletedCount > 0;
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        // remove user sessions
        deleteUserSessions(email);
        //DONE> Ticket: User Management - implement the delete user method
        return usersCollection.deleteOne(eq(USER_EMAIL_FIELD, email)).wasAcknowledged();
        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions.
    }

    /**
     * Updates the preferences of an user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
        //DONE> Ticket: User Preferences - implement the method that allows for user preferences to
        // be updated.

        if (userPreferences == null || userPreferences.isEmpty()) {
            throw new IncorrectDaoOperation("User preferences can not be null.");
        }

        usersCollection.updateOne(eq(USER_EMAIL_FIELD, email), set(USER_FIELD_PREFERENCES, userPreferences));

        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions when updating an entry.
        return true;
    }
}
