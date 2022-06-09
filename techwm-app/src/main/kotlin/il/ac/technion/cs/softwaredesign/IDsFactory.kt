package il.ac.technion.cs.softwaredesign

import java.util.UUID

/**
 * A factory of IDs.
 *
 */
interface IDsFactory {
    fun createID() : String
}