import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.util.concurrent.CompletableFuture

/**
 * this is the class responsible for creating instances of secure storages, it implements both read and write
 * operations with regard to the limits, and supports asynchronous programming.
 *
 * @param dbFactory the secure storage factory to be used to create instances.
 * @param name the wanted name of the newly created database.
 */
open class DataBase @Inject constructor(dbFactory: SecureStorageFactory, name:String) {
    private val database = dbFactory.open(name.encodeToByteArray())


    // Size is in Bytes
    private val maxDBEntrySize = 100

    /**
     * performs the read operation from the database, notice that we saved the number of partitions done to the written
     * value in the place "key_-1" and the rest of partitions according to their index in the keys
     * "key_0", "key_1" ... "key_n"
     *
     * @param key the wanted key to be read
     * @return completeableFuture of byte array which is the actual data read from the database.
     */
    fun read (key: String): CompletableFuture<ByteArray?> {
        return database.thenCompose { db ->
            var res = byteArrayOf()
            db.read(key.plus("_-1").encodeToByteArray())
                .thenApply { numOfBlocks ->
                    if (numOfBlocks == null) {
                        return@thenApply null
                    }
                    (0..String(numOfBlocks).toInt()).fold(CompletableFuture.completedFuture(Unit)) { prev, index ->
                        prev.thenCompose {
                            db.read(key.plus("_${index}").encodeToByteArray()).thenApply { value -> res += value!! }
                        }
                    }
                    return@thenApply res
                }
        }
    }



    // TODO: maybe use thenCombine to concat results
//    fun read (key: String): CompletableFuture<ByteArray?> = CompletableFuture.supplyAsync {
//        database.thenApply { db ->
//            db.read(key.plus("_-1").encodeToByteArray()).thenApply { numOfBlocks ->
//                if (numOfBlocks == null) {
//                    return@thenApply null
//                }
//                else {
//                    var returnedValue = byteArrayOf()
//                    val futures = arrayListOf<CompletableFuture<ByteArray?>>()
//                    for (i in 0..String(numOfBlocks).toInt()){
//                        futures.add(db.read(key.plus("_${i}").encodeToByteArray()))
//                    }
//                    CompletableFuture.allOf()
//                }
//            }
//
//        }
//        return@supplyAsync "".encodeToByteArray()
//    }


    /**
     * performs the write operation, notice that the partitioning to maxDBEntry sizes is done in the while loop.
     *
     * @param key they key to be written
     * @param value value associated with the key
     * @return comppletablefuture<unit> since nothing has to be returned.
     */
    // TODO: maybe use thenCompose for sequential writes to DB
    fun write (key: String, value: ByteArray) : CompletableFuture<Unit> = CompletableFuture.supplyAsync {
        database.thenApply { db ->
            val bytesToWrite = value.size
            var counter = 0
            var bytesWritten = 0
            var currentKey: ByteArray
            while ( (bytesToWrite - bytesWritten) > maxDBEntrySize ) {
                currentKey = key.plus("_${counter}").encodeToByteArray()
                // subtraction of 1 from maxDBEntrySize is because of zero base counting
                db.write(currentKey, value.slice(bytesWritten..bytesWritten + ( maxDBEntrySize - 1) ).toByteArray())
                counter += 1
                bytesWritten += maxDBEntrySize
            }
            currentKey = key.plus("_${counter}").encodeToByteArray()
            db.write(currentKey, value.slice(bytesWritten..bytesWritten + (bytesToWrite - bytesWritten - 1) ).toByteArray())
            // write the number of blocks
            db.write(key.plus("_-1").encodeToByteArray(), counter.toString().encodeToByteArray())
        }

    }
}