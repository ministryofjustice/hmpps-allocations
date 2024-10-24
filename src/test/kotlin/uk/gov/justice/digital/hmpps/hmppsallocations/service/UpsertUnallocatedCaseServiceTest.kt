package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeEach
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.UnallocatedEvents
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.ZonedDateTime

class UpsertUnallocatedCaseServiceTest {

    @MockK
    lateinit var repository: UnallocatedCasesRepository

    @MockK
    lateinit var dataBaseOperationService: UnallocatedDataBaseOperationService

    @MockK
    lateinit var hmppsTierApiClient: HmppsTierApiClient

    @MockK
    lateinit var workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient

    @InjectMockKs
    lateinit var cut: UpsertUnallocatedCaseService

    @BeforeEach
    fun setUp() {
      MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `upsertUnallocatedCase non lao case`() = runTest {
      val crn = "X1234567"
      val name = "Bob Jones"
      val teamCode = "N54ERT"
      val providerCode = "PC001"
      val tier = "C2"
      val uce = UnallocatedCaseEntity(1L, name, crn, tier, teamCode, providerCode, ZonedDateTime.now(), 1)
      val dca = DeliusCaseAccess(crn , false, false)
      val ae = ActiveEvent("1", teamCode, providerCode)
      val uae = UnallocatedEvents(crn, Name("Bob", "Crusher", "Jones"), listOf(ae))
      coEvery { workforceAllocationsToDeliusApiClient.getUserAccess(crn, any()) } returns dca
      coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn)} returns uae
      coEvery { hmppsTierApiClient.getTierByCrn(crn) } returns tier
      coEvery { repository.findByCrn(crn) } returns listOf(uce)
      cut.upsertUnallocatedCase(crn)
      verify(exactly = 1) { dataBaseOperationService.saveNewEvents(any(), any(), any(), crn, any()) }
    }

    @Test
    fun `upsertUnallocatedCase lao restricted case`() = runTest {
      val crn = "X1234567"
      val name = "Bob Jones"
      val teamCode = "N54ERT"
      val providerCode = "PC001"
      val tier = "C2"
      val uce = UnallocatedCaseEntity(1L, name, crn, tier, teamCode, providerCode, ZonedDateTime.now(), 1)
      val dca = DeliusCaseAccess(crn , true, false)
      val ae = ActiveEvent("1", teamCode, providerCode)
      val uae = UnallocatedEvents(crn, Name("Bob", "Crusher", "Jones"), listOf(ae))
      coEvery { workforceAllocationsToDeliusApiClient.getUserAccess(crn, any()) } returns dca
      coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn)} returns uae
      coEvery { hmppsTierApiClient.getTierByCrn(crn) } returns tier
      coEvery { repository.findByCrn(crn) } returns listOf(uce)
      cut.upsertUnallocatedCase(crn)
      verify(exactly = 0) { dataBaseOperationService.saveNewEvents(any(), any(), any(), crn, any()) }
    }

  @Test
  fun `upsertUnallocatedCase lao excluded case`() = runTest {
    val crn = "X1234567"
    val name = "Bob Jones"
    val teamCode = "N54ERT"
    val providerCode = "PC001"
    val tier = "C2"
    val uce = UnallocatedCaseEntity(1L, name, crn, tier, teamCode, providerCode, ZonedDateTime.now(), 1)
    val dca = DeliusCaseAccess(crn , false, true)
    val ae = ActiveEvent("1", teamCode, providerCode)
    val uae = UnallocatedEvents(crn, Name("Bob", "Crusher", "Jones"), listOf(ae))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccess(crn, any()) } returns dca
    coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn)} returns uae
    coEvery { hmppsTierApiClient.getTierByCrn(crn) } returns tier
    coEvery { repository.findByCrn(crn) } returns listOf(uce)
    cut.upsertUnallocatedCase(crn)
    verify(exactly = 1) { dataBaseOperationService.saveNewEvents(any(), any(), any(), crn, any()) }
  }
}