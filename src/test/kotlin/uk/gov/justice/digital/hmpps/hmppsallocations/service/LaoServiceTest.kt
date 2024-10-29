package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusAccessRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusApopUser

class LaoServiceTest {

  @MockK
  lateinit var workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient

  @InjectMockKs
  lateinit var laoService: LaoService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `returns correct restrictions object for a case with no restriction`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns emptyList()
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = emptyList(),
      exclusionMessage = "n/a",
      restrictionMessage = "n/a",
    )

    val restrictions = laoService.getCrnRestrictions(crn)
    assert(!restrictions.hasRestriction)
    assert(!restrictions.hasExclusion)
    assert(!restrictions.apopUserExcluded)
  }

  @Test
  fun `returns correct restrictions object for a case that has some user excluded`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "not fred", staffCode = "9991"))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = laoService.getCrnRestrictions(crn)
    assert(!restrictions.hasRestriction)
    assert(restrictions.hasExclusion)
    assert(!restrictions.apopUserExcluded)
  }

  @Test
  fun `returns correct restrictions object for a case that is restricted to certain users`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns emptyList()
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns
      DeliusAccessRestrictionDetails(
        crn = crn,
        restrictedTo = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
        excludedFrom = emptyList(),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )
    val restrictions = laoService.getCrnRestrictions(crn)
    assert(restrictions.hasRestriction)
    assert(!restrictions.hasExclusion)
    assert(!restrictions.apopUserExcluded)
  }

  @Test
  fun `returns correct restrictions object for a case that has excluded users who are also aPoP users`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "fred", staffCode = "12345"))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = laoService.getCrnRestrictions(crn)
    assert(!restrictions.hasRestriction)
    assert(restrictions.hasExclusion)
    assert(restrictions.apopUserExcluded)
  }
}
