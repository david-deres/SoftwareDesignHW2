import il.ac.technion.cs.softwaredesign.loan.LoanService
import java.util.concurrent.CompletableFuture

class TestLoanService : LoanService {
    private var loanCalled = 0
    private var returnCalled = 0

    override fun loanBook(id: String): CompletableFuture<Unit> {
        loanCalled++
        return CompletableFuture.completedFuture(Unit)
    }

    override fun returnBook(id: String): CompletableFuture<Unit> {
        returnCalled++
        return CompletableFuture.completedFuture(Unit)
    }

    fun getLoanCalledNum() = loanCalled
    fun getReturnCalledNum() = returnCalled
}