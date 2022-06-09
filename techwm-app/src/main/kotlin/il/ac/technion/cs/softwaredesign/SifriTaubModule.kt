package il.ac.technion.cs.softwaredesign


import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.loan.LoanServiceModule
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule


class SifriTaubModule: KotlinModule() {

    override fun configure() {

        bind<TokenFactory>().to<ProductionTokenFactory>().`in`<Singleton>()
        install(SecureStorageModule())
        install(LoanServiceModule())
        bind<IDsFactory>().to<ProductionIDsFactory>().`in`<Singleton>()

    }
}