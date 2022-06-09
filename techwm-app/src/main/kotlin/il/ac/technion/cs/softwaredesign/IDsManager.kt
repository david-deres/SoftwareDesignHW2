package il.ac.technion.cs.softwaredesign

import DataBase
import java.util.concurrent.CompletableFuture

/**
 * a manager of IDs, used for working with a database of IDs, supports adding a single ID and retrieving all existing IDs.
 *
 * @property db the relevant database that stores the IDs
 */
class IDsManager (private val db: DataBase) {

    private val idsKey = "allIDs"
    private val idsDelimiter = "!"

    /**
     * adds an ID to the database.
     *
     * @param id the id to be added.
     * @return completeableFuture<Unit> , addition operation shouldn't return a value.
     */
    fun addId(id: String) : CompletableFuture<Unit> {
        return db.read(idsKey).thenCompose {
            ids ->
            if (ids == null) {
                db.write(idsKey, id.encodeToByteArray())
            }
            else {
                db.write(idsKey, String(ids).plus("${idsDelimiter}${id}").encodeToByteArray())
            }
        }
    }

    /**
     * retrieves all the IDs from the database.
     *
     * @return a completeableFuture of a String Set .
     */
    fun getIds() : CompletableFuture<Set<String>> {
        return db.read(idsKey).thenApply {
                ids ->
            if (ids == null) {
                return@thenApply setOf()
            }
            else {
                return@thenApply String(ids).split(idsDelimiter).toSet()
            }
        }
    }
}