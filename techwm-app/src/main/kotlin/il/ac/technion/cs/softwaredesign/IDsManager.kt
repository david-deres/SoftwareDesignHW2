package il.ac.technion.cs.softwaredesign

import DataBase

class IDsManager (private val db: DataBase) {

    private val idsKey = "allIDs"
    private val idsDelimiter = "!"

    fun addId(id: String) {
        db.read(idsKey)?.let { db.write(idsKey, String(it).plus("${idsDelimiter}${id}").encodeToByteArray()) }
            ?: db.write(idsKey, id.encodeToByteArray())
    }

    fun getIds() : Set<String> {
        return db.read(idsKey)?.let { String(it).split(idsDelimiter).toSet() } ?: return setOf()
    }
}