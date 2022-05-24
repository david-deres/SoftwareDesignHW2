import il.ac.technion.cs.softwaredesign.storage.SecureStorage

class TestSecureStorage(private val hashMap: HashMap<String, ByteArray>) : SecureStorage {
    override fun read(key: ByteArray): ByteArray? {
        return hashMap[String(key)]
    }

    override fun write(key: ByteArray, value: ByteArray) {
        if (value.size > 100) return
        hashMap[String(key)] = value
    }
}