package il.ac.technion.cs.softwaredesign

import java.util.UUID

class ProductionIDsFactory : IDsFactory {
    override fun createID(): String {
        return UUID.randomUUID().toString()
    }
}