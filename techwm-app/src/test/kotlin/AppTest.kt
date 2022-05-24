import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.*

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random.Default.nextInt

class AppTest {
    private val injector = Guice.createInjector(TestSifriTaubModule())
    private val manager = injector.getInstance<SifriTaub>()

    @Test
    fun `a non-existing user throws exception on authenticate`() {
        val username = "non-existing"
        val password = "non-existing"

        assertThrows<IllegalArgumentException> {
            manager.authenticate(username, password)
        }
    }

    @Test
    fun `a non-existing token throws exception on requests`() {
        val token = "HELLO_WORLD"

        assertThrows<PermissionException> {
            manager.listBookIds(token)
        }
    }

    @Test
    fun `first user is successfully registered`() {
        val username = "user-a"
        val password = "123456"

        assertDoesNotThrow {
            manager.register(username, password, true, 25)
        }
        val token = manager.authenticate(username, password)
        val info = manager.userInformation(token, username)
        Assertions.assertEquals(info, User(username, true, 25))
    }

    @Test
    fun `register of user that already exists throws exception`() {
        val username = "user-a"
        val passwordA = "123456"
        val passwordB = "AaBb"

        manager.register(username, passwordA, true, 25)
        assertThrows<IllegalArgumentException> { manager.register(username, passwordB, true, 33)  }
    }

    @Test
    fun `register of user with negative age throws exception`() {
        val username = "user-a"
        val password = "123456"

        assertThrows<IllegalArgumentException> { manager.register(username, password, true, -6)  }
    }

    @Test
    fun `authentication of existing user with incorrect password throws exception`() {
        val username = "user-a"
        val password = "123456"

        manager.register(username, password, true, 25)
        assertThrows<IllegalArgumentException> { manager.authenticate(username, "WRONG")  }
    }

    @Test
    fun `get user information returns User object correctly`() {
        val username = "user-a"
        val password = "123456"
        manager.register(username, password, true, 31)
        val token =  manager.authenticate(username, password)
        Assertions.assertEquals(User(username, true, 31), manager.userInformation(token, username))
    }

    @Test
    fun `get user information returns null for non-existing user`() {
        val username = "user-a"
        val password = "123456"
        manager.register(username, password, true, 31)
        val token =  manager.authenticate(username, password)
        Assertions.assertNull(manager.userInformation(token, "Non-existing"))
    }

    @Test
    fun `new token invalidates older one`() {
        val username = "user-b"
        val password = "123456"

        manager.register(username, password, true, 31)
        val token = manager.authenticate(username, password)
        manager.authenticate(username, password)
        assertThrows<PermissionException> {
            manager.userInformation(token, username)
        }
    }

    @Test
    fun `new token can be used properly`() {
        val username = "user-b"
        val password = "123456"

        manager.register(username, password, true, 31)
        manager.authenticate(username, password)
        val token =  manager.authenticate(username, password)
        assertDoesNotThrow {
            manager.userInformation(token, username)
        }
    }

    @Test
    fun `new book can be added properly`() {
        val username = "a"
        val password = "b"

        manager.register(username, password, true, 31)
        val token =  manager.authenticate(username, password)
        assertDoesNotThrow {
            manager.addBookToCatalog(token, "12345", "A wonderful book about cats and their owners" , 5)
        }
    }

    @Test
    fun `adding a book with an existing id throws exception`() {
        val username = "a"
        val password = "b"
        val id = "12345"

        manager.register(username, password, true, 31)
        val token =  manager.authenticate(username, password)
        manager.addBookToCatalog(token, id , "A wonderful book about cats and their owners" , 5)
        assertThrows<IllegalArgumentException> {
            manager.addBookToCatalog(token, id, "some random description" , 1)
        }

    }

    @Test
    fun `get description returns correct description`() {
        val username = "a"
        val password = "b"
        val description = "A wonderful book about cats and their owners"
        val id = "12345"

        manager.register(username, password, true, 31)
        val token =  manager.authenticate(username, password)
        manager.addBookToCatalog(token, id, description , 5)
        Assertions.assertEquals(description, manager.getBookDescription(token, id))
    }

    @Test
    fun `get description returns correct description for larger than 100B descriptions`() {
        val username = "a"
        val password = "b"
        val description = "A wonderful book about cats and their owners".repeat(6)
        val id = "12345"

        manager.register(username, password, true, 31)
        val token =  manager.authenticate(username, password)
        manager.addBookToCatalog(token, id, description , 5)
        Assertions.assertEquals(description, manager.getBookDescription(token, id))
    }

    @Test
    fun `get description throws an exception for non-existent book id`() {
        val username = "a"
        val password = "b"
        val id = "12345"

        manager.register(username, password, true, 31)
        val token =  manager.authenticate(username, password)
        assertThrows<IllegalArgumentException> {
            manager.getBookDescription(token, id)
        }
    }

    @Test
    fun `listBookIds works properly with default n`() {
        val username = "a"
        val password = "b"
        val description = "A wonderful book about cats and their owners"


        manager.register(username, password, true, 31)
        val token =  manager.authenticate(username, password)
        for (id in 1..20){
            manager.addBookToCatalog(token, id.toString(), description , 5)
        }
        Assertions.assertEquals((1..10).toList().map{it.toString()}, manager.listBookIds(token))
    }

    @Test
    fun `listBookIds returns the newest 3 book id's properly`() {
        val username = "a"
        val password = "b"
        val description = "A wonderful book about cats and their owners"


        manager.register(username, password, true, 31)
        val token =  manager.authenticate(username, password)
        val ids = (1..10).shuffled()
        for (id in ids) {
            manager.addBookToCatalog(token, id.toString(), description , 5)
        }
        Assertions.assertEquals(ids.take(3).map{it.toString()}, manager.listBookIds(token, 3))
    }

    @Test
    fun `users are persistent after reboot`() {
        val username = "user-a"
        val password = "123456"

        manager.register(username, password, true, 25)
        val newInstance = injector.getInstance<SifriTaub>()

        assertDoesNotThrow {
            newInstance.authenticate(username, password)
        }
    }

    @Test
    fun `tokens are persistent after reboot`() {
        val username = "user-a"
        val password = "123456"

        manager.register(username, password, true, 25)
        val newInstance = injector.getInstance<SifriTaub>()
        val token = newInstance.authenticate(username, password)
        assertDoesNotThrow {
            newInstance.listBookIds(token)
        }
    }

    @Test
    fun `adding many users and books and use all methods randomly but correctly succeeds`() {
        val numOfUsersToRegister = 1000
        val numOfBooksToAdd = 100
        val password = "1"

        for (i in 1..numOfUsersToRegister){
            manager.register(i.toString(), password, true, 21)
        }

        for (k in 1..numOfUsersToRegister) {
            manager.authenticate(nextInt(1, numOfUsersToRegister).toString() , password)
        }

        val userToAuthenticate = nextInt(1, numOfUsersToRegister).toString()
        val token = manager.authenticate(userToAuthenticate , password)

        for (j in 1..numOfBooksToAdd) {
            manager.addBookToCatalog(token, j.toString(), "Some Random Description... $j", 1)
        }
        val bookToCheck = nextInt(1, numOfBooksToAdd).toString()
        Assertions.assertEquals("Some Random Description... $bookToCheck", manager.getBookDescription(token, bookToCheck))
        Assertions.assertEquals(listOf("1"), manager.listBookIds(token, 1))


        val username = nextInt(1, numOfUsersToRegister).toString()
        Assertions.assertEquals(User(username,true, 21), manager.userInformation(token, username))
    }
}

