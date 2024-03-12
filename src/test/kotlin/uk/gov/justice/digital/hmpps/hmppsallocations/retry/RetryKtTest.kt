package uk.gov.justice.digital.hmpps.hmppsallocations.retry

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.rmi.UnexpectedException
import java.util.concurrent.atomic.AtomicInteger

internal class RetryKtTest{
  @Test
  fun `when exception thrown retry until max retries`() {
    val counter = AtomicInteger(0)
    org.junit.jupiter.api.assertThrows<UnexpectedException> {
      retry(3) {
        counter.incrementAndGet()
        throw UnexpectedException("Unexpected Exception")
      }
    }
    MatcherAssert.assertThat(counter.get(), Matchers.equalTo(3))
  }

  @Test
  fun `when successful no retries`() {
    val counter = AtomicInteger(0)
    val result = retry(3) {
      counter.incrementAndGet()
    }
    MatcherAssert.assertThat(result, Matchers.equalTo(1))
  }
}