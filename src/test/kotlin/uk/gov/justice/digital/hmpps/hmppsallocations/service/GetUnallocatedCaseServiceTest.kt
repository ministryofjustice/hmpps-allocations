package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

internal class GetUnallocatedCaseServiceTest {

  private val mockWorkforceAllocationsToDeliusApiClientClient: WorkforceAllocationsToDeliusApiClient = mockk()
  private val mockRepo: UnallocatedCasesRepository = mockk()

  @Test
  fun `must not return unallocated cases which get deleted during enrichment`() {
    val crn = "X123456"
    val id = 2L
    val unallocatedCaseEntity = UnallocatedCaseEntity(
      caseType = CaseTypes.COMMUNITY,
      name = "case1",
      convictionId = 1L,
      crn = crn,
      providerCode = "PC1",
      tier = "C2",
      id = id,
      convictionNumber = 1
    )
    every { mockRepo.findByTeamCode("TM1") } returns listOf(unallocatedCaseEntity)
    every { mockRepo.existsById(id) } returns false
    every { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetails(listOf(unallocatedCaseEntity)) } returns Flux.empty()
    val cases = GetUnallocatedCaseService(mockRepo, mockk(), mockk(), mockWorkforceAllocationsToDeliusApiClientClient).getAllByTeam("TM1").collectList().block()
    assertEquals(0, cases!!.size)
  }
}
