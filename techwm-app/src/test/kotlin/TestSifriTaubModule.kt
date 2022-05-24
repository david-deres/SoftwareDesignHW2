import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.KotlinModule
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.ProductionTokenFactory
import il.ac.technion.cs.softwaredesign.TokenFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class TestSifriTaubModule : KotlinModule() {

    override fun configure() {
        val injector = Guice.createInjector(TestSecureStorageModule())
        val storageFactoryInstance = injector.getInstance<SecureStorageFactory>()

        bind<TokenFactory>().to<ProductionTokenFactory>()
        bind<SecureStorageFactory>().toInstance(storageFactoryInstance)

    }
}
