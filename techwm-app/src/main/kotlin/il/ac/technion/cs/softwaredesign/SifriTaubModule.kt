package il.ac.technion.cs.softwaredesign

import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.KotlinModule
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.loan.LoanService
import il.ac.technion.cs.softwaredesign.loan.impl.LoanServiceImpl
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule


class SifriTaubModule: KotlinModule() {

    override fun configure() {
        val injector = Guice.createInjector(SecureStorageModule())
        val storageFactoryInstance = injector.getInstance<SecureStorageFactory>()


        bind<TokenFactory>().to<ProductionTokenFactory>()
        bind<SecureStorageFactory>().toInstance(storageFactoryInstance)
        bind<LoanService>().to<LoanServiceImpl>()
        bind<IDsFactory>().to<ProductionIDsFactory>()

    }
}