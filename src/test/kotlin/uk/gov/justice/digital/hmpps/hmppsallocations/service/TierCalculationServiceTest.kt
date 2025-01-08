package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.ZonedDateTime

class TierCalculationServiceTest {

  @MockK
  lateinit var hmppsTierApiClient: HmppsTierApiClient

  @MockK
  lateinit var repository: UnallocatedCasesRepository

  @InjectMockKs
  lateinit var service: TierCalculationService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun updateTier() = runTest {
    val crn = "X1234567"
    val name = "Bob Jones"
    val teamCode = "N54ERT"
    val providerCode = "PC001"
    val tier = "C2"
    val unallocatedCaseEntity = UnallocatedCaseEntity(1L, name, crn, tier, teamCode, providerCode, ZonedDateTime.now(), 1)
    coEvery { repository.existsByCrn(crn) }.returns(true)
    coEvery { repository.findByCrn(crn) } returns listOf(unallocatedCaseEntity)
    coEvery { hmppsTierApiClient.getTierByCrn(crn) }.returns(tier)
    coEvery { repository.save(unallocatedCaseEntity) } returns unallocatedCaseEntity
    service.updateTier(crn)
    verify(exactly = 1) { repository.save(any()) }
  }

  @Test
  fun getTier() = runTest {
    val crn = "X1234567"
    coEvery { hmppsTierApiClient.getTierByCrn(crn) } returns "C2"
    val actual = service.getTier(crn)
    assertEquals("C2", actual)
  }
}
