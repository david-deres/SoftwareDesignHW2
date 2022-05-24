package il.ac.technion.cs.softwaredesign

import DataBase
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class TokensManager (private val tokenFactory: TokenFactory, dbFactory: SecureStorageFactory, name: String) {

    private val userTokenDB = DataBase(dbFactory, name)
    private val dbName = this.hashCode().toString()
    private val tokensDB = DataBase(dbFactory, dbName)
    private val validTokenValue = "V".encodeToByteArray()
    private val invalidTokenValue = "X".encodeToByteArray()

    // We don't have the option to use delete in SecureStorage, thus, we use another method of invalidation
    // which has a cost of storage.
    fun invalidateToken(username: String){
        val prevToken = userTokenDB.read(username)
        if (prevToken != null) {
            tokensDB.write(String(prevToken), invalidTokenValue)
        }
    }

    fun createToken(username: String) : String {
        val token = tokenFactory.createToken()
        userTokenDB.write(username, token.encodeToByteArray())
        tokensDB.write(token, validTokenValue)
        return token
    }

    fun isValidToken(token: String) : Boolean {
        val value = tokensDB.read(token) ?: return false
        return (String(value) == String(validTokenValue))
    }
}