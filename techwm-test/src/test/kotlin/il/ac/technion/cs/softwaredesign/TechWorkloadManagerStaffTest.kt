package il.ac.technion.cs.softwaredesign

import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import dev.misfitlabs.kotlinguice4.getInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class TechWorkloadsifriTaubStaffTest {
    private val injector = Guice.createInjector(SifriTaubModule())
    private val sifriTaub = injector.getInstance<SifriTaub>()

    private fun registerFirstUser(): CompletableFuture<Pair<String, String>> {
        val username = "user"
        val password = "123456"
        return sifriTaub.register(username, password, true, 42)
            .thenApply { username to password }
    }

    @Test
    fun `a non-existing user throws exception on authenticate`() {
        val username = "non-existing"
        val password = "non-existing"

        val throwable = assertThrows<CompletionException> {
            sifriTaub.authenticate(username, password).join()
        }
        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }

    @Test
    fun `first user is successfully registered`() {
        val username = "admin"
        val password = "123456"

        assertDoesNotThrow {
            sifriTaub.register(username, password, false, 18).join()
        }
    }

    @Test
    fun `a single loan is obtained when there are enough books`() {
        /* join() is only allowed in tests */
        val (username, password) = registerFirstUser().join()

        sifriTaub.authenticate(username, password)
            .thenCompose { token -> // First scope, to have token
                sifriTaub.addBookToCatalog(token, "harry-potter", "nice book", 1)
                    .thenCompose { sifriTaub.addBookToCatalog(token, "intro-to-cs", "", 1) }
                    .thenCompose { sifriTaub.submitLoanRequest(token, "first-loan", listOf("harry-potter", "intro-to-cs")) }
                    .thenCompose { loanId -> // Second scope, to have loanId
                        sifriTaub.waitForBooks(token, loanId).thenCompose {
                            sifriTaub.loanRequestInformation(token, loanId)
                        }.thenApply { loanInfo -> assertEquals(loanInfo.loanStatus, LoanStatus.OBTAINED) }
                    }
            }.join()
    }
}