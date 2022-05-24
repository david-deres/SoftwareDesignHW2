package il.ac.technion.cs.softwaredesign

import DataBase
import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import org.junit.jupiter.api.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataBaseTest {
    private val injector = Guice.createInjector(TestSecureStorageModule())
    private val manager = injector.getInstance<SecureStorageFactory>()

    private lateinit var db: DataBase
    private val key = "Hello"


    @BeforeAll
    fun openDataBase() {
        db = DataBase(manager, "myDB")
    }


    @Test
    fun `read a non existent key returns null`() {
        Assertions.assertNull(db.read("Hamlet"))
    }

    @Test
    fun `write to db succeeds`() {
        val value = "World"

        assertDoesNotThrow {
            db.write(key, value.toByteArray())
        }
    }

    @Test
    fun `read from an existing key returns correct value`() {
        Assertions.assertEquals("World", db.read(key)?.let { String(it) })
    }

    @Test
    fun `write to an existing key overwrites the previous value`() {
        val value = "Again"
        db.write(key, value.toByteArray())
        Assertions.assertEquals("Again", db.read(key)?.let { String(it) })
    }

    @Test
    fun `new name for database returns empty database`() {
        val newDB = DataBase(manager, "new")
        Assertions.assertNull(newDB.read(key))
    }

    @Test
    fun `new DataBase instance with same name for database returns existing database`() {
        val newDB = DataBase(manager, "myDB")
        Assertions.assertEquals("Again", newDB.read(key)?.let { String(it) })
    }


    @Test
    fun `write complex objects larger than 100B to db succeeds and can be read`() {
        val k = "long-string"
        val value = "Hello".repeat(100).toByteArray()

        db.write(k, value)
        Assertions.assertEquals("Hello".repeat(100), db.read(k)?.let { String(it) })
    }
}


