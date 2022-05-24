package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture

class TestSecureStorageFactory : SecureStorageFactory {

    private val openDBS = hashMapOf<String, SecureStorage>()

    override fun open(name: ByteArray): CompletableFuture<SecureStorage> {
        var db = openDBS[String(name)]
        if (db != null) {
            return CompletableFuture.supplyAsync{ db }
        } else {
            db = TestSecureStorage(hashMapOf())
            openDBS[String(name)] = db
            return CompletableFuture.supplyAsync { db }
        }
    }
}