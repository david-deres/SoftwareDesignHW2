package il.ac.technion.cs.softwaredesign

import java.security.MessageDigest
import java.time.LocalDateTime

class ProductionTokenFactory : TokenFactory {
    override fun createToken(): String {
        val time = LocalDateTime.now().toString()
        return MessageDigest.getInstance("sha-1").digest(time.toByteArray()).toString()
    }
}