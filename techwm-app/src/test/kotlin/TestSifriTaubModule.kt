import com.google.inject.Guice
import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.IDsFactory
import il.ac.technion.cs.softwaredesign.ProductionIDsFactory
import il.ac.technion.cs.softwaredesign.ProductionTokenFactory
import il.ac.technion.cs.softwaredesign.TokenFactory
import il.ac.technion.cs.softwaredesign.loan.LoanService
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import io.mockk.mockk

class TestSifriTaubModule : KotlinModule() {


    private val loanServiceMock = mockk<LoanService>(relaxed=true)

    override fun configure() {
//        val injector = Guice.createInjector(TestSecureStorageModule())
//        val storageFactoryInstance = injector.getInstance<TestSecureStorageFactory>()


        bind<TokenFactory>().to<ProductionTokenFactory>().`in`<Singleton>()
        install(TestSecureStorageModule())
//        install(TestSecureStorageModule())
        bind<LoanService>().toInstance(loanServiceMock)
        bind<IDsFactory>().to<ProductionIDsFactory>().`in`<Singleton>()

    }
}
