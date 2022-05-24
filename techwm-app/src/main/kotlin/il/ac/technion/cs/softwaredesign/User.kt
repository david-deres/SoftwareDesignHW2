package il.ac.technion.cs.softwaredesign

import com.google.gson.Gson


/**
 * A class holding a single user's information in the system.
 *
 * @property username A unique username identifying the user throughout the system
 * @property isFromCS Whether the student is from CS faculty or external.
 * @property age The age of the student.
 */
data class User(val username: String, val isFromCS: Boolean, val age: Int) {
    companion object {
        var gson = Gson()
        fun fromJSON(user : String) : User {
            return gson.fromJson(user, User::class.java)
        }
    }

    fun toByteArray() : ByteArray {
        val gson = Gson()
        return gson.toJson(this).toByteArray()
    }
}