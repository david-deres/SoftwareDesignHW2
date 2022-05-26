import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory


open class DataBase (dbFactory: SecureStorageFactory, name:String) {
    private val db = dbFactory.open(name.encodeToByteArray()).get()

    // Size is in Bytes
    private val maxDBEntrySize = 100

    // TODO: maybe use thenCombine to concat results
    fun read (key: String): ByteArray?{
        var returnedValue = byteArrayOf()
        var counter = 1
        var currentKey = "${key}_0".encodeToByteArray()
        var currentValue = db.read(currentKey).get()
        while (currentValue != null) {
            returnedValue += currentValue
            currentKey = key.plus("_${counter}").encodeToByteArray()
            currentValue = db.read(currentKey).get()
            counter += 1
        }
        return if (returnedValue.isEmpty()) {
            null
        } else returnedValue
    }

    // TODO: maybe use thenCompose for sequential writes to DB
    fun write (key: String, value: ByteArray) {
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
        currentKey = key.plus("_${counter}").toByteArray()
        db.write(currentKey, value.slice(bytesWritten..bytesWritten + (bytesToWrite - bytesWritten - 1) ).toByteArray())
    }
}