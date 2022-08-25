package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDate

internal class GetUnallocatedCaseServiceTest {

  private val mockClient: CommunityApiClient = mockk()
  private val mockRepo: UnallocatedCasesRepository = mockk()

  @Test
  fun `must not return unallocated cases which get deleted during enrichment`() {
    val crn = "X123456"
    val sentenceDate = LocalDate.now()
    val id = 2L
    val unallocatedCaseEntity = UnallocatedCaseEntity(
      caseType = CaseTypes.COMMUNITY,
      name = "case1",
      convictionId = 1L,
      crn = crn,
      providerCode = "PC1",
      sentenceDate = sentenceDate,
      status = "status",
      tier = "C2",
      id = id
    )
    every { mockRepo.findAll() } returns listOf(unallocatedCaseEntity)
    every { mockRepo.existsById(id) } returns false
    every { mockClient.getInductionContacts(crn, sentenceDate) } returns Mono.just(listOf())
    val cases = GetUnallocatedCaseService(mockRepo, mockClient, mockk(), mockk(), mockk()).getAll().collectList().block()
    assertEquals(0, cases!!.size)
  }
}
