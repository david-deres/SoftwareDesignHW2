package il.ac.technion.cs.softwaredesign

import DataBase
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class TokensManager (private val tokenFactory: TokenFactory, dbFactory: SecureStorageFactory, name: String) {

    private val userTokenDB = DataBase(dbFactory, name)
    private val dbName = this.hashCode().toString()
    private val tokensUserDB = DataBase(dbFactory, dbName)

    // We don't have the option to use delete in SecureStorage, thus, we use another method of invalidation
    // which has a cost of storage.
    fun invalidateToken(username: String){
        val prevToken = userTokenDB.read(username)
        if (prevToken != null) {
            tokensUserDB.write(String(prevToken), "INVALID".encodeToByteArray())
        }
    }

    fun createToken(username: String) : String {
        val token = tokenFactory.createToken()
        userTokenDB.write(username, token.encodeToByteArray())
        tokensUserDB.write(token, username.encodeToByteArray())
        return token
    }

    fun isValidToken(token: String) : Boolean {
        val value = tokensUserDB.read(token) ?: return false
        return (String(value) != "INVALID")
    }

    fun getUsernameFromToken(token: String) : String? {
        val username = tokensUserDB.read(token)
        if ((username == null) or (String(username!!) == "INVALID")) {
            return null
        }
        return String(username)
    }
}