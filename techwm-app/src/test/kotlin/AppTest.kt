import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.*

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class AppTest {
    private val injector = Guice.createInjector(TestSifriTaubModule())
    private val sifriTaub = injector.getInstance<SifriTaub>()

    private fun registerFirstUser(): CompletableFuture<Pair<String, String>> {
        val username = "user"
        val password = "123456"
        return sifriTaub.register(username, password, true, 42)
            .thenApply { username to password }
    }

    private fun addBooksToCatalog(amount: Int): CompletableFuture<String> {
        val username = "user"
        val password = "123456"


        return sifriTaub.register(username, password, true, 25).thenCompose {
            sifriTaub.authenticate(username, password).thenCompose { token ->
                (1..amount).fold(CompletableFuture.completedFuture(Unit)) {prev, id ->
                    prev.thenCompose { sifriTaub.addBookToCatalog(token, id.toString(), "random${id.toString()}", 1) }
                }.thenCompose { CompletableFuture.completedFuture(token) }
            }
        }
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
                    .thenCompose {
                        sifriTaub.submitLoanRequest(
                            token,
                            "first-loan",
                            listOf("harry-potter", "intro-to-cs")
                        )
                    }
                    .thenCompose { loanId -> // Second scope, to have loanId
                        sifriTaub.waitForBooks(token, loanId).thenCompose {
                            sifriTaub.loanRequestInformation(token, loanId)
                        }.thenApply { loanInfo -> Assertions.assertEquals(loanInfo.loanStatus, LoanStatus.OBTAINED) }
                    }
            }.join()
    }


    @Test
    fun `a non-existing token throws exception on requests`() {
        val token = "HELLO_WORLD"

        val throwable = assertThrows<CompletionException> {
            sifriTaub.listBookIds(token).join()
        }
        assertThat(throwable.cause!!, isA<PermissionException>())
    }

    @Test
    fun `register of user that already exists throws exception`() {
        val username = "user-a"
        val passwordA = "123456"
        val passwordB = "AaBb"

        val throwable = assertThrows<CompletionException> {
            sifriTaub.register(username, passwordA, true, 25).thenCompose {
                sifriTaub.register(username, passwordB, true, 33)
            }.join()
        }
        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }

    @Test
    fun `register of user with negative age throws exception`() {
        val username = "user-a"
        val password = "123456"

        assertThrows<IllegalArgumentException> {
            sifriTaub.register(username, password, true, -6).join()
        }
    }

    @Test
    fun `authentication of existing user with incorrect password throws exception`() {
        val username = "user-a"
        val password = "123456"

        val throwable = assertThrows<CompletionException> {
            sifriTaub.register(username, password, true, 31).thenCompose {
                sifriTaub.authenticate(username, "WRONG")
            }.join()
        }
        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }

    @Test
    fun `get user information returns User object correctly`() {
        val username = "user-a"
        val password = "123456"
        sifriTaub.register(username, password, true, 31).thenCompose {
            sifriTaub.authenticate(username, password).thenApply { token ->
                Assertions.assertEquals(User(username, true, 31), sifriTaub.userInformation(token, username).get())
            }
        }

    }

    @Test
    fun `get user information returns null for non-existing user`() {
        val username = "user-a"
        val password = "123456"
        sifriTaub.register(username, password, true, 31)
            .thenCompose { sifriTaub.authenticate(username, password) }
            .thenApply { token ->
                Assertions.assertNull(sifriTaub.userInformation(token, "Non-existing").get())
            }
    }

    @Test
    fun `new token invalidates older one`() {
        val username = "user-b"
        val password = "123456"

        val throwable = assertThrows<CompletionException> {
            sifriTaub.register(username, password, true, 31)
                .thenCompose { sifriTaub.authenticate(username, password) }
                .thenCompose { token ->
                    sifriTaub.authenticate(username, password)
                        .thenCompose { sifriTaub.userInformation(token, username) }
                }.join()
        }
        assertThat(throwable.cause!!, isA<PermissionException>())
    }

    @Test
    fun `new token can be used properly`() {
        val username = "user-b"
        val password = "123456"

        sifriTaub.register(username, password, true, 31).thenCompose {
            sifriTaub.authenticate(username, password).thenCompose {
                sifriTaub.authenticate(username, password)
            }.thenApply { token ->
                assertDoesNotThrow {
                    sifriTaub.userInformation(token, username).get()
                }
            }
        }.join()
    }

    @Test
    fun `new book can be added properly`() {
        val username = "a"
        val password = "b"

        sifriTaub.register(username, password, true, 31)
            .thenCompose {
                sifriTaub.authenticate(username, password)
                    .thenApply { token ->
                        assertDoesNotThrow {
                            sifriTaub.addBookToCatalog(
                                token,
                                "12345",
                                "A wonderful book about cats and their owners",
                                5
                            ).get()
                        }
                    }
            }.join()
    }

    @Test
    fun `adding a book with an existing id throws exception`() {
        val username = "a"
        val password = "b"
        val id = "12345"

        val throwable = assertThrows<CompletionException> {
            sifriTaub.register(username, password, true, 31).thenCompose {
                sifriTaub.authenticate(username, password)
                    .thenCompose { token ->
                        sifriTaub.addBookToCatalog(token, id, "A wonderful book about cats and their owners", 5)
                            .thenCompose { sifriTaub.addBookToCatalog(token, id, "some random description", 1) }
                    }
            }.join()
        }
        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }

    @Test
    fun `get description returns correct description`() {
        val username = "a"
        val password = "b"
        val description = "A wonderful book about cats and their owners"
        val id = "12345"

        sifriTaub.register(username, password, true, 31)
            .thenCompose { sifriTaub.authenticate(username, password)
                .thenCompose { token -> sifriTaub.addBookToCatalog(token, id, description , 5)
                    .thenApply { Assertions.assertEquals(description, sifriTaub.getBookDescription(token, id).get()) }
                }}.join()
    }

    @Test
    fun `get description returns correct description for larger than 100B descriptions`() {
        val username = "a"
        val password = "b"
        val description = "A wonderful book about cats and their owners".repeat(6)
        val id = "12345"

        sifriTaub.register(username, password, true, 31)
            .thenCompose { sifriTaub.authenticate(username, password)
                .thenCompose { token -> sifriTaub.addBookToCatalog(token, id, description , 5)
                    .thenApply { Assertions.assertEquals(description, sifriTaub.getBookDescription(token, id).get()) }
                }}.join()
    }

    @Test
    fun `get description throws an exception for non-existent book id`() {
        val username = "a"
        val password = "b"
        val id = "12345"

        val throwable = assertThrows<CompletionException>{
            sifriTaub.register(username, password, true, 31)
                .thenCompose { sifriTaub.authenticate(username, password)
                    .thenCompose { token -> sifriTaub.getBookDescription(token, id) }}.join()
        }
        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }

    @Test
    fun `listBookIds works properly with default n`() {
        val username = "a"
        val password = "b"
        val description = "A wonderful book about cats and their owners"


        sifriTaub.register(username, password, true, 31)
            .thenCompose { sifriTaub.authenticate(username, password)
                .thenCompose {
                        token ->
                        for (id in 1..20){
                            sifriTaub.addBookToCatalog(token, id.toString(), description , 5).get()
                        }
                        sifriTaub.listBookIds(token).thenApply { result -> Assertions.assertEquals((1..10).toList().map{it.toString()}, result) }
                }
            }.join()
    }

    @Test
    fun `listBookIds returns the newest 3 book id's properly`() {
        val username = "a"
        val password = "b"
        val description = "A wonderful book about cats and their owners"
        val ids = (1..10).shuffled()


        sifriTaub.register(username, password, true, 31)
            .thenCompose { sifriTaub.authenticate(username, password)
                .thenCompose {
                        token ->
                        for (id in ids) {
                            sifriTaub.addBookToCatalog(token, id.toString(), description , 5).get()
                        }
                        sifriTaub.listBookIds(token, 3).thenApply { result -> Assertions.assertEquals(ids.take(3).map{it.toString()}, result) }

                }}.join()
    }

    @Test
    fun `users are persistent after reboot`() {
        val username = "user-a"
        val password = "123456"


        assertDoesNotThrow {
            sifriTaub.register(username, password, true, 25).thenCompose {
                val newInstance = injector.getInstance<SifriTaub>()
                newInstance.authenticate(username, password).thenApply { token -> Assertions.assertNotNull(token) }
            }.join()
        }
    }

    @Test
    fun `tokens are persistent after reboot`() {
        val username = "user-a"
        val password = "123456"

        sifriTaub.register(username, password, true, 25).thenCompose {
            val newInstance = injector.getInstance<SifriTaub>()
            sifriTaub.authenticate(username, password).thenApply { token -> assertDoesNotThrow { newInstance.listBookIds(token).get() } }
        }.join()
    }

    @Test
    fun `A token and user remain valid after the system restarts`() {
        val username = "user-a"
        val password = "123456"

        sifriTaub.register(username, password, true, 25).thenCompose {
            sifriTaub.authenticate(username, password).thenApply { token ->
                    val newInstance = injector.getInstance<SifriTaub>()
                    assertDoesNotThrow { newInstance.userInformation(token, username).thenApply { info -> Assertions.assertNotNull(info) } }
            }
        }.join()
    }

    @Test
    fun `listBookIDs lists only available books`() {
        val username = "user-a"
        val password = "123456"

        sifriTaub.register(username, password, true, 25).thenCompose {
            sifriTaub.authenticate(username, password).thenCompose { token ->
                sifriTaub.addBookToCatalog(token, "1", "harry-potter", 1)
                .thenCompose { sifriTaub.submitLoanRequest(token, "first", listOf("1") ) }
                    .thenCompose { loanId -> sifriTaub.waitForBooks(token, loanId) }
                    .thenCompose { sifriTaub.listBookIds(token).thenApply { ids -> assertTrue(ids.isEmpty()) } }
            }
        }.join()
    }

    @Test
    fun `submitLoanRequest throws PermissionException on invalid token`() {
        val throwable = assertThrows<CompletionException> {
            sifriTaub.submitLoanRequest("NO_SUCH_TOKEN", "1", listOf()).join()
        }
        assertThat(throwable.cause!!, isA<PermissionException>())
    }

    @Test
    fun `submitLoanRequest throws exception on nonexistent book`() {

        val (username, password) = registerFirstUser().join()

        val throwable = assertThrows<CompletionException> {
            sifriTaub.authenticate(username, password).thenCompose {
                token -> sifriTaub.submitLoanRequest(token, "1", listOf("1"))
            }.join()
        }
        assertThat(throwable.cause!!, isA<IllegalArgumentException>())
    }

    @Test
    fun `submitLoanRequest updates the loan`(){
        addBooksToCatalog(5).thenCompose {
                token ->  sifriTaub.submitLoanRequest(token, "first", listOf("1", "2"))
                  .thenCompose { loanId -> sifriTaub.loanRequestInformation(token, loanId)
                      .thenApply { loanInfo -> Assertions.assertEquals(LoanStatus.QUEUED, loanInfo.loanStatus) }}
        }.join()
    }


    @Test
    fun `cancelLoanRequest cancels the loan`(){
        addBooksToCatalog(5).thenCompose {
                token ->  sifriTaub.submitLoanRequest(token, "first", listOf("1", "2"))
                            .thenCompose { sifriTaub.submitLoanRequest(token, "first", listOf("3", "4")) }
            .thenCompose { loanId -> sifriTaub.cancelLoanRequest(token, loanId)
            .thenCompose { sifriTaub.loanRequestInformation(token, loanId) }
                .thenApply { loanInfo -> Assertions.assertEquals(LoanStatus.CANCELED, loanInfo.loanStatus) }}
        }.join()
    }

//    @Test
//    fun `cancelLoanRequest cancels the loan and releases the queue`(){
//        addBooksToCatalog(5).thenCompose {
//                token ->  sifriTaub.submitLoanRequest(token, "first", listOf("1", "2"))
//            .thenCompose { sifriTaub.submitLoanRequest(token, "first", listOf("3", "4")) }
//            .thenCompose { loanId -> sifriTaub.cancelLoanRequest(token, loanId)
//                .thenCompose { sifriTaub.loanRequestInformation(token, loanId) }
//                .thenApply { loanInfo -> Assertions.assertEquals(LoanStatus.CANCELED, loanInfo.loanStatus) }}
//        }.join()
//    }
}


//
//    @Test
//    fun `adding many users and books and use all methods randomly but correctly succeeds`() {
//        val numOfUsersToRegister = 1000
//        val numOfBooksToAdd = 100
//        val password = "1"
//
//        for (i in 1..numOfUsersToRegister){
//            manager.register(i.toString(), password, true, 21)
//        }
//
//        for (k in 1..numOfUsersToRegister) {
//            manager.authenticate(nextInt(1, numOfUsersToRegister).toString() , password)
//        }
//
//        val userToAuthenticate = nextInt(1, numOfUsersToRegister).toString()
//        val token = manager.authenticate(userToAuthenticate , password)
//
//        for (j in 1..numOfBooksToAdd) {
//            manager.addBookToCatalog(token, j.toString(), "Some Random Description... $j", 1)
//        }
//        val bookToCheck = nextInt(1, numOfBooksToAdd).toString()
//        Assertions.assertEquals("Some Random Description... $bookToCheck", manager.getBookDescription(token, bookToCheck))
//        Assertions.assertEquals(listOf("1"), manager.listBookIds(token, 1))
//
//
//        val username = nextInt(1, numOfUsersToRegister).toString()
//        Assertions.assertEquals(User(username,true, 21), manager.userInformation(token, username))
//    }
//}
//
