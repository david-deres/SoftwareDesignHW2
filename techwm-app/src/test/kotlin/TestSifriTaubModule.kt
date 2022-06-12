import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.IDsFactory
import il.ac.technion.cs.softwaredesign.ProductionIDsFactory
import il.ac.technion.cs.softwaredesign.ProductionTokenFactory
import il.ac.technion.cs.softwaredesign.TokenFactory
import il.ac.technion.cs.softwaredesign.loan.LoanService


class TestSifriTaubModule(private val loanService: LoanService) : KotlinModule() {


    override fun configure() {

        bind<TokenFactory>().to<ProductionTokenFactory>()
        install(TestSecureStorageModule())
        bind<LoanService>().toInstance(loanService)
        bind<IDsFactory>().to<ProductionIDsFactory>()

    }
}
