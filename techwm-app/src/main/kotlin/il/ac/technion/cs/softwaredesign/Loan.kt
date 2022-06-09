package il.ac.technion.cs.softwaredesign

import DataBase
import il.ac.technion.cs.softwaredesign.loan.LoanService
import java.util.concurrent.CompletableFuture

/**
 * implements the provided ObtainedLoan interface and is used in the waitForBooks function.
 *
 * @property loanId ID of the relevant Loan.
 * @property loansDB the database which stores information about loans.
 * @property loanService instance of the loaning service class.
 */
class Loan (private val loanId: String, private val loansDB: DataBase, private val loanService: LoanService, private val booksDB: DataBase, private val isCanceled: Boolean) : ObtainedLoan {

    private fun increaseBookCopies(bookId : String) : CompletableFuture<Unit> {
        return booksDB.read(bookId)
            .thenCompose {
                    b ->
                val book =  Book.fromJSON(String(b!!))
                book.copiesAmount++
                booksDB.write(bookId, book.toByteArray())
            }
    }

    override fun returnBooks(): CompletableFuture<Unit> {
        if (isCanceled ) return CompletableFuture.completedFuture(Unit)
        return loansDB.read(this.loanId).thenCompose { l ->
            val loan = LoanRequestInformation.fromJSON(String(l!!))
            // no double returns on the same Loan
            if (loan.loanStatus == LoanStatus.RETURNED) { CompletableFuture.completedFuture(Unit) }
            loan.loanStatus = LoanStatus.RETURNED
            loan.requestedBooks.fold(CompletableFuture.completedFuture(Unit)) { prev, bookID ->
                prev.thenCompose { increaseBookCopies(bookID)
                    .thenCompose { loanService.returnBook(bookID) }
                } }.thenCompose {
                loansDB.write(this.loanId, loan.toByteArray())
            }
        }
    }

}
