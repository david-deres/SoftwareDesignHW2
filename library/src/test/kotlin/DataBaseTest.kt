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
        Assertions.assertNull(db.read("Hamlet").get())
    }

    @Test
    fun `write to db succeeds`() {
        val value = "World"

        assertDoesNotThrow {
            db.write(key, value.toByteArray()).join()
        }
    }

    @Test
    fun `read from an existing key returns correct value`() {
        db.read(key).thenApply { Assertions.assertEquals("World", String(it!!)) }.join()
    }

    @Test
    fun `write to an existing key overwrites the previous value`() {
        val value = "Again"
        db.write(key, value.toByteArray()).join()
        db.read(key).thenApply { Assertions.assertEquals("Again", String(it!!)) }.join()
    }

    @Test
    fun `new name for database returns empty database`() {
        val newDB = DataBase(manager, "new")
        Assertions.assertNull(newDB.read(key).get())
    }

    @Test
    fun `new DataBase instance with same name for database returns existing database`() {
        val newDB = DataBase(manager, "myDB")
        newDB.read(key).thenApply { Assertions.assertEquals("Again", String(it!!)) }.join()
    }


    @Test
    fun `write complex objects larger than 100B to db succeeds and can be read`() {
        val k = "long-string"
        val value = "Hello".repeat(100).toByteArray()

        db.write(k, value).join()
        db.read(k).thenApply { Assertions.assertEquals("Hello".repeat(100), String(it!!)) }.join()
    }
}


