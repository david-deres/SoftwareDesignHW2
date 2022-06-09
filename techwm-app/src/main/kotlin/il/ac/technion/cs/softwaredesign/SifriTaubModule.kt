package il.ac.technion.cs.softwaredesign


import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.loan.LoanService
import il.ac.technion.cs.softwaredesign.loan.impl.LoanServiceImpl
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import il.ac.technion.cs.softwaredesign.storage.impl.SecureStorageFactoryImpl


class SifriTaubModule: KotlinModule() {

    override fun configure() {
//        val injector = Guice.createInjector(SecureStorageModule())
//        val storageFactoryInstance = injector.getInstance<SecureStorageFactory>()


        bind<TokenFactory>().to<ProductionTokenFactory>().`in`<Singleton>()
        //TODO: install these modules and assure they are treated as singletons
        bind<SecureStorageFactory>().to<SecureStorageFactoryImpl>().`in`<Singleton>()
        bind<LoanService>().to<LoanServiceImpl>().`in`<Singleton>()
        bind<IDsFactory>().to<ProductionIDsFactory>().`in`<Singleton>()

    }
}