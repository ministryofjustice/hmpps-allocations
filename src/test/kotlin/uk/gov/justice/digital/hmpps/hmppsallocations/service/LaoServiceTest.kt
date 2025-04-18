package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusUserAccess
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
    assert(restrictions.apopUserExcluded)
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

  @Test
  fun `returns correct restriction statuses object for a a list of cases`() = runTest {
    val crns = listOf("crn1", "crn2", "crn3", "crn4", "crn5", "crn6", "crn7", "crn8", "crn9", "crn10")

    coEvery { workforceAllocationsToDeliusApiClient.getApopUsers() } returns listOf(
      DeliusApopUser(username = "001", staffCode = "12341"),
      DeliusApopUser(username = "002", staffCode = "12342"),
      DeliusApopUser(username = "003", staffCode = "12343"),
      DeliusApopUser(username = "004", staffCode = "12344"),
      DeliusApopUser(username = "005", staffCode = "12345"),
    )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccess(crns) } returns
      DeliusUserAccess(
        access = listOf(
          DeliusCaseAccess(crn = "crn1", userRestricted = false, userExcluded = false), // no restrictions
          DeliusCaseAccess(crn = "crn2", userRestricted = false, userExcluded = false), // no restrictions
          DeliusCaseAccess(crn = "crn3", userRestricted = false, userExcluded = true), // excluded
          DeliusCaseAccess(crn = "crn4", userRestricted = true, userExcluded = false), // restricted
          DeliusCaseAccess(crn = "crn5", userRestricted = false, userExcluded = true), // excluded
          DeliusCaseAccess(crn = "crn6", userRestricted = false, userExcluded = false), // no restriction
          DeliusCaseAccess(crn = "crn7", userRestricted = false, userExcluded = false), // no restrictions
          DeliusCaseAccess(crn = "crn8", userRestricted = false, userExcluded = true), // excluded
          DeliusCaseAccess(crn = "crn9", userRestricted = true, userExcluded = false), // restricted
          DeliusCaseAccess(crn = "crn10", userRestricted = false, userExcluded = true), // excluded
          DeliusCaseAccess(crn = "crn11", userRestricted = true, userExcluded = true), // excluded AND restricted
        ),
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn3") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn3",
        restrictedTo = emptyList(),
        excludedFrom = listOf(DeliusApopUser(username = "003", staffCode = "12343")), // APoP user
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn5") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn5",
        restrictedTo = emptyList(),
        excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "Notstaff")),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn8") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn8",
        restrictedTo = emptyList(),
        excludedFrom = listOf(DeliusApopUser(username = "005", staffCode = "12345")), // APoP user
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn10") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn10",
        restrictedTo = emptyList(),
        excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "Notstaff")),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    val restrictions = laoService.getCrnRestrictions(crns)

    val checkDeliusUserAccess = DeliusUserAccess(
      access = listOf(
        DeliusCaseAccess(crn = "crn1", userRestricted = false, userExcluded = false),
        DeliusCaseAccess(crn = "crn2", userRestricted = false, userExcluded = false),
        DeliusCaseAccess(crn = "crn3", userRestricted = true, userExcluded = true),
        DeliusCaseAccess(crn = "crn4", userRestricted = true, userExcluded = false), // Now restricted because excluded includes apop user
        DeliusCaseAccess(crn = "crn5", userRestricted = false, userExcluded = true),
        DeliusCaseAccess(crn = "crn6", userRestricted = false, userExcluded = false),
        DeliusCaseAccess(crn = "crn7", userRestricted = false, userExcluded = false),
        DeliusCaseAccess(crn = "crn8", userRestricted = true, userExcluded = true), // Now restricted because excluded includes apop user
        DeliusCaseAccess(crn = "crn9", userRestricted = true, userExcluded = false),
        DeliusCaseAccess(crn = "crn10", userRestricted = false, userExcluded = true),
        DeliusCaseAccess(crn = "crn11", userRestricted = true, userExcluded = true),
      ),
    )
    assert(restrictions.equals(checkDeliusUserAccess))
  }
}
