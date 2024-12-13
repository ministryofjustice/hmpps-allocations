package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusUserAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusCrnRestrictions
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

internal class GetUnallocatedCaseServiceTest {

  private val mockWorkforceAllocationsToDeliusApiClientClient: WorkforceAllocationsToDeliusApiClient = mockk()
  private val mockRepo: UnallocatedCasesRepository = mockk()
  private val mockOutOfAreaTransferService: OutOfAreaTransferService = mockk()
  private val mockLaoService: LaoService = mockk()

  @Test
  fun `must not return unallocated cases which get deleted during enrichment`() {
    runBlocking {
      val crn = "X123456"
      val id = 2L
      val unallocatedCaseEntity = UnallocatedCaseEntity(
        name = "case1",
        crn = crn,
        providerCode = "PC1",
        teamCode = "TM1",
        tier = "C2",
        id = id,
        convictionNumber = 1,
      )
      every { mockRepo.findByTeamCode("TM1") } returns listOf(unallocatedCaseEntity)
      every { mockRepo.existsById(id) } returns false
      coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getUserAccess(listOf(crn)) } returns
        DeliusUserAccess(
          access = listOf(DeliusCaseAccess(crn = crn, userRestricted = false, userExcluded = false)),
        )
      coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetailsCases(listOf(unallocatedCaseEntity)) } returns emptyFlow()
      coEvery { mockLaoService.getCrnRestrictions(crn) } returns
        DeliusCrnRestrictions(false, false, false)
      val cases = GetUnallocatedCaseService(mockRepo, mockOutOfAreaTransferService, mockk(), mockWorkforceAllocationsToDeliusApiClientClient, mockLaoService)
        .getAllByTeam("TM1").toList()
      assertEquals(0, cases.size)
    }
  }

  @Test
  fun `must not return unallocated cases which are restricted and excluded `() = runBlocking {
    val crn = "X123456"
    val unallocatedCaseEntity = UnallocatedCaseEntity(
      name = "restricted",
      crn = crn,
      providerCode = "PC1",
      teamCode = "TM1",
      tier = "C2",
      id = 2L,
      convictionNumber = 1,
    )

    every { mockRepo.findByTeamCode("TM1") } returns listOf(unallocatedCaseEntity)
    coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getUserAccess(listOf(crn)) } returns
      DeliusUserAccess(
        access = listOf(DeliusCaseAccess(crn = crn, userRestricted = true, userExcluded = true)),
      )

    coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetailsCases(emptyList()) } returns emptyFlow()

    val cases =
      GetUnallocatedCaseService(mockRepo, mockOutOfAreaTransferService, mockk(), mockWorkforceAllocationsToDeliusApiClientClient, mockLaoService).getAllByTeam("TM1")
        .toList()

    verify(exactly = 0) { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetailsCases(listOf(unallocatedCaseEntity)) }
    assertEquals(0, cases.size)
  }

  @Test
  fun `must not return unallocated cases which are restricted `() = runBlocking {
    val crn = "X123456"
    val unallocatedCaseEntity = UnallocatedCaseEntity(
      name = "restricted",
      crn = crn,
      providerCode = "PC1",
      teamCode = "TM1",
      tier = "C2",
      id = 2L,
      convictionNumber = 1,
    )

    every { mockRepo.findByTeamCode("TM1") } returns listOf(unallocatedCaseEntity)
    coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getUserAccess(listOf(crn)) } returns
      DeliusUserAccess(
        access = listOf(DeliusCaseAccess(crn = crn, userRestricted = true, userExcluded = false)),
      )

    coEvery { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetailsCases(emptyList()) } returns emptyFlow()

    val cases =
      GetUnallocatedCaseService(mockRepo, mockOutOfAreaTransferService, mockk(), mockWorkforceAllocationsToDeliusApiClientClient, mockLaoService).getAllByTeam("TM1")
        .toList()

    verify(exactly = 0) { mockWorkforceAllocationsToDeliusApiClientClient.getDeliusCaseDetailsCases(listOf(unallocatedCaseEntity)) }
    assertEquals(0, cases.size)
  }
}
