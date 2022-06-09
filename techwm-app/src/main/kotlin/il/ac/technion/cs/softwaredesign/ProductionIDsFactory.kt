package il.ac.technion.cs.softwaredesign

import java.util.UUID

/**
 * an implementation of IDs factory, generates a UUID (universally unique identifier) whenever createID is called.
 *
 */
class ProductionIDsFactory : IDsFactory {
    override fun createID(): String {
        return UUID.randomUUID().toString()
    }
}