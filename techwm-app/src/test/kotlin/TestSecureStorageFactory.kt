import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class TestSecureStorageFactory : SecureStorageFactory {

    private val openDBS = hashMapOf<String, SecureStorage>()

    override fun open(name: ByteArray): SecureStorage {
        var db = openDBS[String(name)]
        if (db != null) {
            return db
        } else {
            db = TestSecureStorage(hashMapOf())
            openDBS[String(name)] = db
            return db
        }
    }
}