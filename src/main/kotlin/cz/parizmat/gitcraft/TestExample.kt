package cz.parizmat.gitcraft

import java.util.concurrent.ConcurrentHashMap

data class Execution(
    val orderId: Long,
    val quantity: Long
)

class ExecutionTracker {

    private val executions = ConcurrentHashMap<Long, Long>()

    fun record(execution: Execution) {
        val current = executions[execution.orderId] ?: 0L
        executions[execution.orderId] =
            current + execution.quantity
    }

    fun quantity(orderId: Long): Long {
        return executions[orderId] ?: 0L
    }
}