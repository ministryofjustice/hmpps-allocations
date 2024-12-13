package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusAccessRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusApopUser
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForLAOException

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

  @Test
  fun `returns correct restrictions when no users are not restricted`() = runTest {
    val crn = "X1234567"
    val deliusAccessRestrictionDetails = DeliusAccessRestrictionDetails(
      crn = crn,
      excludedFrom = emptyList(),
      restrictedTo = emptyList(),
      exclusionMessage = "",
      restrictionMessage = "",
    )

    coEvery { workforceAllocationsToDeliusApiClient.getAccessRestrictionsForStaffCodesByCrn(crn, listOf("123", "456", "789")) } returns deliusAccessRestrictionDetails

    val crnStaffRestrictions = laoService.getCrnRestrictionsForUsers(crn, listOf("123", "456", "789"))

    assert(crnStaffRestrictions.staffRestrictions.size == 3)
    assert(!crnStaffRestrictions.staffRestrictions.get(0).isExcluded)
    assert(!crnStaffRestrictions.staffRestrictions.get(1).isExcluded)
    assert(!crnStaffRestrictions.staffRestrictions.get(2).isExcluded)
    assert(crnStaffRestrictions.staffRestrictions.get(0).staffCode == "123")
    assert(crnStaffRestrictions.staffRestrictions.get(1).staffCode == "456")
    assert(crnStaffRestrictions.staffRestrictions.get(2).staffCode == "789")
  }

  @Test
  fun `returns correct restrictions when some users restricted `() = runTest {
    val crn = "X1234567"
    val deliusAccessRestrictionDetails = DeliusAccessRestrictionDetails(
      crn = crn,
      excludedFrom = listOf(DeliusApopUser("user1", "456")),
      restrictedTo = emptyList(),
      exclusionMessage = "",
      restrictionMessage = "",
    )

    coEvery { workforceAllocationsToDeliusApiClient.getAccessRestrictionsForStaffCodesByCrn(crn, listOf("123", "456", "789")) } returns deliusAccessRestrictionDetails

    val crnStaffRestrictions = laoService.getCrnRestrictionsForUsers(crn, listOf("123", "456", "789"))

    assert(crnStaffRestrictions.staffRestrictions.size == 3)
    assert(!crnStaffRestrictions.staffRestrictions.get(0).isExcluded)
    assert(crnStaffRestrictions.staffRestrictions.get(1).isExcluded)
    assert(!crnStaffRestrictions.staffRestrictions.get(2).isExcluded)
    assert(crnStaffRestrictions.staffRestrictions.get(0).staffCode == "123")
    assert(crnStaffRestrictions.staffRestrictions.get(1).staffCode == "456")
    assert(crnStaffRestrictions.staffRestrictions.get(2).staffCode == "789")
  }

  @Test
  fun `returns correct restrictions when other users restricted `() = runTest {
    val crn = "X1234567"
    val deliusAccessRestrictionDetails = DeliusAccessRestrictionDetails(
      crn = crn,
      excludedFrom = listOf(DeliusApopUser("user1", "456"), DeliusApopUser("user2", "789"), DeliusApopUser("user3", "212")),
      restrictedTo = emptyList(),
      exclusionMessage = "",
      restrictionMessage = "",
    )

    coEvery { workforceAllocationsToDeliusApiClient.getAccessRestrictionsForStaffCodesByCrn(crn, listOf("123", "456")) } returns deliusAccessRestrictionDetails

    val crnStaffRestrictions = laoService.getCrnRestrictionsForUsers(crn, listOf("123", "456"))

    assert(crnStaffRestrictions.staffRestrictions.size == 2)
    assert(!crnStaffRestrictions.staffRestrictions.get(0).isExcluded)
    assert(crnStaffRestrictions.staffRestrictions.get(1).isExcluded)
    assert(crnStaffRestrictions.staffRestrictions.get(0).staffCode == "123")
    assert(crnStaffRestrictions.staffRestrictions.get(1).staffCode == "456")
  }

  @Test
  fun `is not restricted when no users are restricted`() = runTest {
    val crn = "X1234567"
    val deliusAccessRestrictionDetails = DeliusAccessRestrictionDetails(
      crn = crn,
      excludedFrom = emptyList(),
      restrictedTo = emptyList(),
      exclusionMessage = "",
      restrictionMessage = "",
    )

    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "green", staffCode = "12345"))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns deliusAccessRestrictionDetails

    assert(!laoService.isCrnRestricted(crn))
  }

  @Test
  fun `is restricted when users are restricted and we don't care about apop users`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "green", staffCode = "12345"))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "green", staffCode = "non apop")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    assert(laoService.isCrnRestricted(crn))
  }

  @Test
  fun `throws 403 when case is restricted to certain individuals only`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "green", staffCode = "12345"))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = listOf(DeliusApopUser(username = "JamesBond", staffCode = "007")),
      excludedFrom = emptyList(),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    assertThrows<NotAllowedForLAOException> { (laoService.isCrnRestricted(crn)) }
  }

  @Test
  fun `throws 403 when an apop user is excluded`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "fred", staffCode = "apop"))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "green", staffCode = "apop")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    assertThrows<NotAllowedForLAOException> { (laoService.isCrnRestricted(crn)) }
  }

  @Test
  fun `returns correct restriction status object for a case that has no restrictions`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "fred", staffCode = "apop"))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = emptyList(),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = laoService.getCrnRestrictionStatus(crn)
    assert(restrictions.crn == crn)
    assert(!restrictions.isRedacted)
    assert(!restrictions.isRestricted)
  }

  @Test
  fun `returns correct restriction status object for a case that has some user excluded`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "not fred", staffCode = "9991"))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = laoService.getCrnRestrictionStatus(crn)
    assert(restrictions.crn == crn)
    assert(!restrictions.isRedacted)
    assert(restrictions.isRestricted)
  }

  fun `returns correct restriction status object for a case that has apop user excluded`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(DeliusApopUser(username = "fred", staffCode = "9991"))
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = laoService.getCrnRestrictionStatus(crn)
    assert(restrictions.crn == crn)
    assert(!restrictions.isRedacted)
    assert(restrictions.isRestricted)
  }

  @Test
  fun `returns correct restriction status object for a case that is restricted to certain users`() = runTest {
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
    val restrictions = laoService.getCrnRestrictionStatus(crn)
    assert(restrictions.crn == crn)
    assert(restrictions.isRestricted)
    assert(restrictions.isRedacted)
  }
}
