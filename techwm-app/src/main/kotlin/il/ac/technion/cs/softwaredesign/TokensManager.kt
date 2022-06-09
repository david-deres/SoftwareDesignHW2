package il.ac.technion.cs.softwaredesign

import DataBase
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture

/**
 * a manager for the tokens which is responsible for working with the tokens.
 * uses two databases, userTokenDB which holds mappings of user -> token.
 *                     tokensUserDB which holds mappings of token -> token status.
 *
 * @property tokenFactory a factory of tokens used to create tokens.
 * @param dbFactory factory of Databases, mainly used for opening existing databases.
 * @param name name of the database.
 */
class TokensManager (private val tokenFactory: TokenFactory, dbFactory: SecureStorageFactory, name: String) {

    private val userTokenDB = DataBase(dbFactory, name)
    private val dbName = "tokens"
    private val tokensUserDB = DataBase(dbFactory, dbName)

    // We don't have the option to use delete in SecureStorage, thus, we use another method of invalidation
    // which has a cost of storage.
    /**
     * given a username, invalidates its existing token.
     *
     * @param username the username of which its token must get invalidated.
     * @return CompleteableFuture<Unit>, the function is supposed to update the databases and therefore doesnt return.
     */
    fun invalidateToken(username: String): CompletableFuture<Unit> {
        return userTokenDB.read(username).thenApply { prevToken ->
            if (prevToken != null) {
                tokensUserDB.write(String(prevToken), "INVALID".encodeToByteArray())
            }
        }
    }

    /**
     * creates a token by using the factory, and inserts it to the relevant databases.
     *
     * @param username the username the token should be associated with.
     * @return a completeableFuture<String> since working with the databases is done asynchronously, this too, must do so.
     */
    fun createToken(username: String): CompletableFuture<String> {
        val token = tokenFactory.createToken()
        return userTokenDB.write(username, token.encodeToByteArray()).thenApply {
            tokensUserDB.write(token, username.encodeToByteArray())
            return@thenApply token
        }
    }

    /**
     * checks if the given token is valid or not.
     *
     * @param token is the actual token to be used to fetch data from the databases.
     * @return returns a completeableFuture<boolean> since working with the databases is done asynchronously, this too, must do so.
     */
    fun isValidToken(token: String): CompletableFuture<Boolean> {
        return tokensUserDB.read(token).thenApply { value ->
            if (value == null) {
                return@thenApply false
            } else {
                return@thenApply (String(value) != "INVALID")
            }
        }
    }

    /**
     * given a token, this function returns the username associated to it.
     *
     * @param token token of which the associated username is wanted.
     * @return completeableFuture<String> since working with the databases is done asynchronously, this too, must do so.
     */
    fun getUsernameFromToken(token: String): CompletableFuture<String?> {
        return tokensUserDB.read(token).thenApply {
            if ((it == null) or (String(it!!) == "INVALID")) {
                return@thenApply null
            } else {
                return@thenApply String(it)
            }

        }
    }
}