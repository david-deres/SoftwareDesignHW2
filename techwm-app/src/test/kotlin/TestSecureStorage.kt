import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.util.concurrent.CompletableFuture

class TestSecureStorage(private val hashMap: HashMap<String, ByteArray>) : SecureStorage {
    override fun read(key: ByteArray): CompletableFuture<ByteArray?> {
        return CompletableFuture.completedFuture(hashMap[String(key)])
    }

    override fun write(key: ByteArray, value: ByteArray): CompletableFuture<Unit> {
        if (value.size > 100) return CompletableFuture.completedFuture(Unit)
        hashMap[String(key)] = value
        return CompletableFuture.completedFuture(Unit)
    }
}