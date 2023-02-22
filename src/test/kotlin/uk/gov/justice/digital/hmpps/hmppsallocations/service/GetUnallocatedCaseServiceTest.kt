package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

internal class GetUnallocatedCaseServiceTest {

  private val mockWorkforceAllocationsToDeliusApiClientClient: WorkforceAllocationsToDeliusApiClient = mockk()
  private val mockRepo: UnallocatedCasesRepository = mockk()

  @Test
  fun `must not return unallocated cases which get deleted during enrichment`() = runBlocking {
    val crn = "X123456"
    val id = 2L
    val unallocatedCaseEntity = UnallocatedCaseEntity(
      name = "case1",
      crn = crn,
      providerCode = "PC1",
      teamCode = "TM1",
      tier = "C2",
      id = id,
      convictionNumber = 1
    )
    every { mockRepo.findByTeamCode("TM1") } returns listOf(unallocatedCaseEntity)
    every { mockRepo.existsById(id) } returns false
    coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetails(listOf(unallocatedCaseEntity)) } returns emptyFlow()
    val cases = GetUnallocatedCaseService(mockRepo, mockk(), mockk(), mockWorkforceAllocationsToDeliusApiClientClient).getAllByTeam("TM1").toList()
    assertEquals(0, cases!!.size)
  }
}
