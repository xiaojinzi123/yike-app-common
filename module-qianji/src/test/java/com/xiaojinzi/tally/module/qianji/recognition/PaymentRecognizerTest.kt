package com.xiaojinzi.tally.module.qianji.recognition

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** 合成 fixtures 的 JUnit4 入口，和纯 JVM 自执行入口使用同一组业务断言。 */
@RunWith(Parameterized::class)
class PaymentRecognizerTest(private val case: SyntheticPaymentRegression.RecognitionCase) {
    @Test
    fun syntheticPageRequiresUnambiguousEvidence() {
        SyntheticPaymentRegression.verify(case)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> = SyntheticPaymentRegression.recognitionCases.map { arrayOf<Any>(it) }
    }
}
