package il.ac.technion.cs.softwaredesign

//import il.ac.technion.cs.softwaredesign.loan.GeneralBookLoanApproval
//import il.ac.technion.cs.softwaredesign.loan.FacultyStaffBookLoanApproval
//import il.ac.technion.cs.softwaredesign.loan.RegularBookLoanApproval
import java.util.concurrent.CompletableFuture

/**
 * Represents a loan status in the system:
 * * [QUEUED] The loan is queued and the user waits for all the books to become available.
 * * [OBTAINED] The user was given all of the books and they are not returned yet.
 * * [RETURNED] The books where returned and the loan is no longer in the queue.
 * * [CANCELED] The user canceled the loan, and no longer waits for the books.
 */
enum class LoanStatus {
    QUEUED,
    OBTAINED,
    RETURNED,
    CANCELED
}

/**
 * A description of a loan in the system.
 * * [requestedBooks] - A list of requested books IDs.
 * * [loanName] - The submitted loan name, as specified by the user.
 * * [ownerUserId] - The user who requested this loan.
 * * [loanStatus] - A flag indicating this loan's status in the system.
 */
data class LoanRequestInformation(val loanName: String,
                                  val requestedBooks: List<String>,
                                  val ownerUserId: String,
                                  val loanStatus: LoanStatus)


/**
 * An [ObtainedLoan] represents a loan which its books where given to the user, and the user has not returned them yet.
 * Using this object we can return the books back to the library.
 */
interface ObtainedLoan {
    /**
     * Marks the loan request as [LoanStatus.RETURNED], returning all book to the library, marking them available for other loan
     * requests. This should also call LoanService.returnBook(id) for each book that was loaned.
     */
    fun returnBooks(): CompletableFuture<Unit>
}