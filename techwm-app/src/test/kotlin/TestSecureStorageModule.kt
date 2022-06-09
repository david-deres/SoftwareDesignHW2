import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class TestSecureStorageModule : KotlinModule() {
    override fun configure() {
        bind<SecureStorageFactory>().to<TestSecureStorageFactory>().`in`<Singleton>()
    }
}