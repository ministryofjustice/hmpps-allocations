package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusApopUser
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusLimitedAccessDetails

class LaoServiceTest {

  @MockK
  lateinit var workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient

  @InjectMockKs
  lateinit var cut: LaoService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `non lao case`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns emptyList()
    coEvery { workforceAllocationsToDeliusApiClient.getAllUserAccessByCrn(crn) } returns DeliusLimitedAccessDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = emptyList(),
      exclusionMessage = "n/a",
      restrictionMessage = "n/a",
    )

    val restrictions = cut.getCrnRestrictions(crn)
    assert(!restrictions.hasRestriction)
    assert(!restrictions.hasExclusion)
    assert(!restrictions.apopUserExcluded)
  }

  @Test
  fun `lao excluded case`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "not fred", staffCode = "9991"))
    coEvery { workforceAllocationsToDeliusApiClient.getAllUserAccessByCrn(crn) } returns DeliusLimitedAccessDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = cut.getCrnRestrictions(crn)
    assert(!restrictions.hasRestriction)
    assert(restrictions.hasExclusion)
    assert(!restrictions.apopUserExcluded)
  }

  @Test
  fun `lao restricted case`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns emptyList()
    coEvery { workforceAllocationsToDeliusApiClient.getAllUserAccessByCrn(crn) } returns
      DeliusLimitedAccessDetails(
        crn = crn,
        restrictedTo = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
        excludedFrom = emptyList(),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )
    val restrictions = cut.getCrnRestrictions(crn)
    assert(restrictions.hasRestriction)
    assert(!restrictions.hasExclusion)
    assert(!restrictions.apopUserExcluded)
  }

  @Test
  fun `lao aPoP excluded case`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "fred", staffCode = "12345"))
    coEvery { workforceAllocationsToDeliusApiClient.getAllUserAccessByCrn(crn) } returns DeliusLimitedAccessDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = cut.getCrnRestrictions(crn)
    assert(!restrictions.hasRestriction)
    assert(restrictions.hasExclusion)
    assert(restrictions.apopUserExcluded)
  }
}
