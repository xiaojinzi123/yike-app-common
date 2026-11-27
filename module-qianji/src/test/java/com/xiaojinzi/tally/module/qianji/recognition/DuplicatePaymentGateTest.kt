package com.xiaojinzi.tally.module.qianji.recognition

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DuplicatePaymentGateTest(private val case: SyntheticPaymentRegression.GateCase) {
    @Test
    fun monotonicGateMaintainsDuplicateContract() {
        case.verify()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> =
            (SyntheticPaymentRegression.gateCases + releaseCases).map { arrayOf<Any>(it) }

        private fun payment(order: String? = "ORDER-RETRY"): RecognizedPayment = RecognizedPayment(
            amountFen = 1230,
            income = false,
            merchant = "合成咖啡店",
            sourcePackage = "com.eg.android.AlipayGphone",
            occurredAt = 1L,
            ruleId = "synthetic.release.v1",
            orderReference = order,
        )

        private val releaseCases = listOf(
            SyntheticPaymentRegression.GateCase("写盘失败release后立即重试，成功后继续阻止重复写入") {
                val gate = DuplicatePaymentGate()
                val payment = payment()
                var writeAttempts = 0
                var savedCount = 0
                fun deliver(elapsedMillis: Long): Boolean {
                    if (!gate.tryAcquire(payment, elapsedMillis)) return false
                    try {
                        writeAttempts++
                        if (writeAttempts == 1) throw java.io.IOException("synthetic write failure")
                        savedCount++
                    } catch (_: java.io.IOException) {
                        gate.release(payment)
                        return false
                    }
                    return true
                }

                check(!deliver(100L))
                check(gate.size == 0)
                check(savedCount == 0)
                check(deliver(100L))
                check(gate.size == 1)
                check(!deliver(101L))
                check(writeAttempts == 2)
                check(savedCount == 1)
            },
            SyntheticPaymentRegression.GateCase("release按规范化字段匹配，不依赖墙钟和规则ID") {
                val gate = DuplicatePaymentGate()
                val original = payment(" ORDER\u00a0RETRY ").copy(merchant = " 合成\u3000咖啡店 ")
                val equivalent = original.copy(
                    merchant = "合成 咖啡店",
                    orderReference = "ORDER RETRY",
                    occurredAt = 999L,
                    ruleId = "synthetic.release.v2",
                )
                check(gate.tryAcquire(original, 0L))

                gate.release(equivalent)

                check(gate.size == 0)
                check(gate.tryAcquire(equivalent, 1L))
                check(!gate.tryAcquire(original, 2L))
            },
            SyntheticPaymentRegression.GateCase("字段补全后仍可用写盘失败时的原始观察release") {
                val gate = DuplicatePaymentGate()
                val incomplete = payment(null).copy(merchant = "")
                val complete = payment()
                check(gate.tryAcquire(incomplete, 0L))
                check(!gate.tryAcquire(complete, 1L))
                check(gate.size == 1)

                gate.release(incomplete)

                check(gate.size == 0)
                check(gate.tryAcquire(complete, 2L))
                check(!gate.tryAcquire(incomplete, 3L))
            },
            SyntheticPaymentRegression.GateCase("补全后的观察也能release尚缺字段的已接受记录") {
                val gate = DuplicatePaymentGate()
                val incomplete = payment(null).copy(merchant = "")
                val complete = payment()
                check(gate.tryAcquire(incomplete, 0L))

                gate.release(complete)

                check(gate.size == 0)
                check(gate.tryAcquire(complete, 1L))
                check(!gate.tryAcquire(incomplete, 2L))
            },
            SyntheticPaymentRegression.GateCase("release只撤销兼容交易，不解除其他交易的去重") {
                val gate = DuplicatePaymentGate()
                val failed = payment()
                val others = listOf(
                    failed.copy(amountFen = failed.amountFen + 1L),
                    failed.copy(income = true),
                    failed.copy(sourcePackage = "com.tencent.mm"),
                    failed.copy(merchant = "另一合成店"),
                    failed.copy(orderReference = "ORDER-OTHER"),
                )
                check(gate.tryAcquire(failed, 0L))
                others.forEachIndexed { index, other ->
                    check(gate.tryAcquire(other, index.toLong() + 1L))
                }

                gate.release(failed)

                check(gate.size == others.size)
                others.forEachIndexed { index, other ->
                    check(!gate.tryAcquire(other, index.toLong() + 10L))
                }
                check(gate.tryAcquire(failed, 20L))
                check(gate.size == others.size + 1)
            },
            SyntheticPaymentRegression.GateCase("release未知记录或重复release不影响剩余记录且允许重试") {
                val gate = DuplicatePaymentGate()
                val retained = payment("ORDER-RETAINED")
                val failed = payment()
                gate.release(failed)
                check(gate.size == 0)
                check(gate.tryAcquire(retained, 0L))
                gate.release(failed)
                check(gate.size == 1)
                check(!gate.tryAcquire(retained, 1L))
                check(gate.tryAcquire(failed, 2L))

                gate.release(failed)
                gate.release(failed)

                check(gate.size == 1)
                check(!gate.tryAcquire(retained, 3L))
                check(gate.tryAcquire(failed, 4L))
                check(gate.size == 2)
            },
        )
    }
}
