package il.ac.technion.cs.softwaredesign

import DataBase
import com.google.inject.Inject
import com.google.inject.Provider
import il.ac.technion.cs.softwaredesign.loan.LoanService
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.Queue
import java.util.LinkedList

/**
 * This is the main class implementing SifriTaub, the new book borrowing system.
 *
 * users database stores mappings of username -> User object
 * books database stores mappings of id -> Book object
 * auth database stores mappings of username -> password
 * ids database stores mappings of username -> User object
 * loans database stores mappings of loanId -> LoanRequestInformation object
 * loansQueue is the queue used to store loan requests, and should be updated accordingly.
 *
 * Currently specified:
 * + Managing users
 * + Managing Books
 * + Queueing book loans
 */
class SifriTaub @Inject constructor (tokenFactory: TokenFactory,
                                     dataBaseProvider: Provider<SecureStorageFactory>, private val loanService: LoanService,
                                     private val iDsFactory: IDsFactory) {

    private val dbFactory = dataBaseProvider.get()
    private val usersDB = DataBase(dbFactory, "users")
    private val booksDB = DataBase(dbFactory, "books")
    private val authDB = DataBase(dbFactory, "auth")
    private val tokensManager = TokensManager(tokenFactory, dbFactory, "tokens")
    private val idsManager = IDsManager(DataBase(dbFactory, "ids"))
    private val loansDB = DataBase(dbFactory, "loans")
    private val loansQueue: Queue<String> = LinkedList(listOf())

    /**
     * responsible for the authentication of a loan request.
     *
     * @param token token of user who's performing the request.
     * @param loanId ID of loan request.
     * @throws PermissionException if username is null
     * @throws IllegalArgumentException if loanId does not exist in the loans DB, or the user,is not the one who requested the loan.
     * @return CompletableFuture<LoanRequestInformation> for the given loan ID.
     */
    private fun authenticateLoan(token: String, loanId: String) : CompletableFuture<LoanRequestInformation> {
        return tokensManager.getUsernameFromToken(token).thenCompose {
            username->
            if (username == null) throw PermissionException()
            loansDB.read(loanId).thenApply {
                loan ->
                if (loan == null) { throw IllegalArgumentException("No such Loan ID!") }
                val returnedLoan = LoanRequestInformation.fromJSON(String(loan))
                if (returnedLoan.ownerUserId != username) {
                    throw IllegalArgumentException("This loan does not belong to the requesting user!")
                }
                return@thenApply returnedLoan
            }
        }
    }

    private fun waitTillBooksAreAvailable(booksIDs : List<String>) : CompletableFuture<Unit> {
         var result = false
         while (!result){
             result = true
             booksIDs.fold(CompletableFuture.completedFuture(Unit)){prev, id ->
                 prev.thenCompose { booksDB.read(id).thenApply {
                         book -> if (Book.fromJSON(String(book!!)).copiesAmount == 0) { result = false}
                 } }
            }
         }
        return CompletableFuture.completedFuture(Unit)
    }


    private fun decreaseBookCopies(bookId : String) : CompletableFuture<Unit> {
        return booksDB.read(bookId)
            .thenCompose {
                    b ->
                val book =  Book.fromJSON(String(b!!))
                book.copiesAmount--
                booksDB.write(bookId, book.toByteArray())
            }
    }

    private fun removeCanceledLoans() : CompletableFuture<Unit> {
    var isFinished = loansQueue.isEmpty()
    while (!isFinished){
        loansDB.read(loansQueue.peek()).thenApply {
                l -> if (LoanRequestInformation.fromJSON(String(l!!)).loanStatus==LoanStatus.CANCELED)
                        { loansQueue.remove() }
                    else
                        { isFinished = true }
        }
    }
    return CompletableFuture.completedFuture(Unit)
}



    /**
     * Authenticate a user identified by [username] and [password].
     *
     * If successful, this method returns a unique authentication
     * token which can be used by the user in subsequent calls to other methods. If the user previously logged in,
     * all previously valid tokens are *invalidated*
     *
     * This is a *read* operation.
     *
     * @throws IllegalArgumentException If the password does not match the username, or this user does not exist in the
     * system.
     * @return An authentication token to be used in future calls.
     */

    fun authenticate(username: String, password: String): CompletableFuture<String> {
        return authDB.read(username).thenApply {
            pass ->
            if (pass == null) {
                throw IllegalArgumentException("No such user exists")
            }
            if (password != String(pass)) {
                throw IllegalArgumentException("Wrong Password!")
            }
            tokensManager.invalidateToken(username)
        }.thenCompose { tokensManager.createToken(username) }
    }

    /**
     * Register a user to the system, allowing him to start using it.
     *
     * This is a *create* operation.
     *
     * @param username The username to register the user under (unique).
     * @param password The password associated with the registered user.
     * @param isFromCS Whether the student is from CS faculty or external.
     * @param age The (positive) age of the student.
     * @param password The password associated with the registered user.
     *
     * @throws IllegalArgumentException If a user with the same [username] already exists or the [age] is negative.
     */
    fun register(username: String, password: String, isFromCS: Boolean, age: Int): CompletableFuture<Unit> {
        if (age < 0) {
            throw IllegalArgumentException("Negative age is illegal")
        }
        return usersDB.read(username).thenCompose {
                user ->
                if (user != null){
                    throw IllegalArgumentException("User already exists")
                 }
                usersDB.write(username, User(username, isFromCS, age).toByteArray()).thenCompose {
                    authDB.write(username, password.encodeToByteArray())
                }
        }
    }

    /**
     * Retrieve information about a user.
     *
     * **Note**: This method can be invoked by all users to query information about other users.
     *
     * This is a *read* operation.
     *
     * @param token A token of some authenticated user, asking for information about the user with username [username].
     * @throws PermissionException If [token] is invalid
     *
     * @return If the user exists, returns a [User] object containing information about the found user. Otherwise,
     * return `null`, indicating that there is no such user
     */
    fun userInformation(token: String, username: String): CompletableFuture<User?> {
        return tokensManager.isValidToken(token).thenApply {
            isValid ->
            if (!isValid) throw PermissionException()
        }.thenCompose {
            return@thenCompose usersDB.read(username).thenApply { userInfo ->
                if (userInfo == null) return@thenApply null
                else return@thenApply User.fromJSON(String(userInfo))
            }
        }
    }


    /**
     * Add a certain book to the library catalog, making it available for borrowing.
     *
     * This is a *create* operation
     *
     * @param token A token used to authenticate the requesting user
     * @param id An id supplied to this book. This must be unique across all books in the system.
     * @param description A human-readable description of the book with unlimited length.
     * @param copiesAmount number of copies that will be available in the library of this book.
     *
     * @throws PermissionException If the [token] is invalid.
     * @throws IllegalArgumentException If a book with the same [id] already exists.
     */
    fun addBookToCatalog(token: String, id: String, description: String, copiesAmount: Int): CompletableFuture<Unit> {
        return tokensManager.isValidToken(token).thenApply {
                isValid ->
            if (!isValid) throw PermissionException()
        }.thenCompose {
            booksDB.read(id).thenCompose {
                    book  ->
                if (book != null) {
                    throw IllegalArgumentException("Book already exists")
                }
                idsManager.addId(id).thenCompose {
                    booksDB.write(id, Book(id, description, copiesAmount, LocalDateTime.now()).toByteArray())
                }
            }
        }
    }


    /**
     * Get the description for the book.
     *
     * This is a *read* operation
     *
     * @param token A token used to authenticate the requesting user
     *
     * @throws PermissionException If the [token] is invalid
     * @throws IllegalArgumentException If a book with the given [id] was not added to the library catalog by [addBookToCatalog].
     * @return A description string of the book with [id]
     */
    fun getBookDescription(token: String, id: String): CompletableFuture<String> {
        return tokensManager.isValidToken(token).thenApply {
                isValid ->
            if (!isValid) throw PermissionException()
        }.thenCompose {
            booksDB.read(id).thenApply {
                    book ->
                    if (book == null) throw IllegalArgumentException("No such book")
                    return@thenApply Book.fromJSON(String(book)).description
            }
        }
    }

    /**
     * List the ids of the first [n] unique books (no id should appear twice).
     *
     * This is a *read* operation.
     *
     * @param token A token used to authenticate the requesting user
     * @throws PermissionException If the [token] is invalid.
     *
     * @return A list of ids, of size [n], sorted by time of addition (determined by a call to [addBookToCatalog]).
     * If there are less than [n] ids of books, this method returns a list of all book ids (sorted as defined above).
     */
    fun listBookIds(token: String, n: Int = 10): CompletableFuture<List<String>> {
        val booksList = mutableListOf<Book>()
        return tokensManager.isValidToken(token).thenApply {
                isValid ->
            if (!isValid) throw PermissionException()
        }.thenCompose {
            idsManager.getIds().thenCompose {
                ids ->
                ids.fold(CompletableFuture.completedFuture(Unit)){
                        prev, id ->
                    prev.thenCompose { booksDB.read(id)
                        .thenCompose { book ->
                            booksList.add(Book.fromJSON(String(book!!)))
                            CompletableFuture.completedFuture(Unit)
                        } }
                }.thenApply {
                    booksList.asSequence()
                        .filter { it.copiesAmount > 0}
                        .sortedBy { it.timeAdded }
                        .take( n )
                        .map { it.id }
                        .toList()
                }
            }
        }
    }

    /**
     * Submit a books loan to the queue with a given list of book IDs and their amount ([bookIds]).
     * A loan submission adheres to a FIFO queue semantic, with the following key points:
     * - A loan rests at the top of the queue, until all requested [bookIds] are available.
     * - If a loan is not at the top of the queue, it cannot be obtained (the books cannot be given) until
     *   all loans before it have been obtained.
     * - The loan queue is sorted by submission time. That is, the sooner a loan is submitted,
     *   the sooner it can be obtained.
     *
     * Important: you must call LoanService.loanBook(id) for each book that is loaned. When the books are returned
     * you must call LoanService.returnBook(id). LoanService should be injected with Guice.
     * This is a *create* operation.
     *
     * @throws PermissionException If the [token] is invalid.
     * @throws IllegalArgumentException if at least on of the requested books does not exist.
     *
     * @return An id for the loan request which can be used for SifriTaub methods such as
     * [loanRequestInformation], [cancelLoanRequest], [waitForBooks].
     * Implementation notes:
     * When a loan is returned, all books are returned and available for loaning.
     * - Even when a loan is not yet obtained, it should still show up in the system, so that [loanRequestInformation] calls
     * succeed and view this loan request as queued.
     */
    fun submitLoanRequest(token: String, loanName: String, bookIds: List<String>): CompletableFuture<String> {
        return tokensManager.isValidToken(token).thenCompose {
            isValid ->
            if (!isValid) throw PermissionException()
            // check for the books to exist
            bookIds.fold(CompletableFuture.completedFuture(Unit))
            { prev, id ->
                prev.thenCompose {
                    booksDB.read(id).thenApply {
                        book ->
                        if (book == null) { throw IllegalArgumentException() }
                    }
                }
            }.thenCompose {
                val loanId = iDsFactory.createID()
                loansQueue.add(loanId)
                tokensManager.getUsernameFromToken(token).thenCompose { username ->
                    loansDB.write(
                        loanId,
                        LoanRequestInformation(loanName, bookIds, username!!, LoanStatus.QUEUED).toByteArray()
                    ).thenApply {
                        return@thenApply loanId
                    }
                }
            }
        }
    }
    /**
     * Return information about a specific loan in the system. [id] is the loan id.
     *
     * This is a *read* operation.
     *
     * @throws PermissionException If the [token] is invalid.
     * @throws IllegalArgumentException If a loan with the supplied [id] does not exist in the system.
     */
    fun loanRequestInformation(token: String, id: String): CompletableFuture<LoanRequestInformation> {
        return tokensManager.isValidToken(token).thenCompose {
            isValid ->
            if (!isValid) throw PermissionException()
            loansDB.read(id).thenApply {
                loan ->
                if (loan == null) throw IllegalArgumentException("No such Loan ID!")
                return@thenApply LoanRequestInformation.fromJSON(String(loan))
            }
        }
    }

    /**
     * Cancel currently queued loan.
     * The loan's status becomes [LoanStatus.CANCELED], and all the books are returned to the library and become available.
     *
     * **Note**: This method can only be invoked by the user which is the owner of the loan.
     *
     * This is a *delete* operation.
     *
     * @throws PermissionException If the [token] is invalid.
     * @throws IllegalArgumentException If the loan associated with [loanId] does not belong to the calling user,
     * does not exist, or it is not currently in a [LoanStatus.QUEUED] state.
     */
    fun cancelLoanRequest(token: String, loanId: String): CompletableFuture<Unit> {
        return authenticateLoan(token, loanId).thenCompose {
            loan ->
            if (loan.loanStatus != LoanStatus.QUEUED) {throw IllegalArgumentException("Loan status is not QUEUED!")}
            loan.loanStatus = LoanStatus.CANCELED
            loansDB.write(loanId, loan.toByteArray()).thenCompose { removeCanceledLoans() }
        }
    }

    /**
     * @return a future that is finished only when the loan is obtained (according to the docs in [submitLoanRequest]).
     * If the loan is already obtained or canceled, the future finishes immediately without an error.
     * @throws PermissionException If the [token] is invalid.
     * @throws IllegalArgumentException If the loan associated with [loanId] does not belong to the calling user
     * or does not exist.
     */
    fun waitForBooks(token: String, loanId: String): CompletableFuture<ObtainedLoan> {
        return authenticateLoan(token, loanId).thenCompose {
            loan ->

            if ((loan.loanStatus == LoanStatus.OBTAINED) or (loan.loanStatus == LoanStatus.CANCELED)){
                CompletableFuture.completedFuture(Loan(loanId, loansDB, loanService, booksDB, loan.loanStatus == LoanStatus.CANCELED))
            }
            else {
                removeCanceledLoans()
                // busy wait until request is at the head of the queue
                while (loanId != loansQueue.peek()) {}
                loan.loanStatus = LoanStatus.OBTAINED
                waitTillBooksAreAvailable(loan.requestedBooks)
                    .thenCompose {
                        loan.requestedBooks.fold(CompletableFuture.completedFuture(Unit)) {
                                prev, bookId ->
                            prev.thenCompose { decreaseBookCopies(bookId)
                                .thenCompose { loanService.loanBook(bookId)  }
                            } }
                            .thenCompose {
                                loansDB.write(loanId, loan.toByteArray())
                            }
                    }
                    .thenApply { loansQueue.remove()
                                return@thenApply Loan(loanId, loansDB, loanService, booksDB, false) }
//                    }
            }
        }
    }
}