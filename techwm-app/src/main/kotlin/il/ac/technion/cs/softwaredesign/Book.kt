package il.ac.technion.cs.softwaredesign

import com.google.gson.Gson
import java.time.LocalDateTime

/**
 * A class holding a single book's information in the system.
 *
 * @property id A unique string identifying the book throughout the system
 * @property description description about the book.
 * @property copiesAmount How many copies are available for this book.
 * @property timeAdded The time the book was added to the system.
 */
data class Book(val id: String, var description: String, var copiesAmount: Int, val timeAdded: LocalDateTime) {

    companion object {
        var gson = Gson()
        fun fromJSON(book : String) : Book {
            return gson.fromJson(book, Book::class.java)
        }
    }

    fun toByteArray() : ByteArray {
        val gson = Gson()
        return gson.toJson(this).toByteArray()
    }
}
