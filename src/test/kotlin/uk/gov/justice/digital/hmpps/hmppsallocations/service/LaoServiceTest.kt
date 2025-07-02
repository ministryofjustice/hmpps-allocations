package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusUserAccess
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.client.OfficerView
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusAccessRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusApopUser
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException
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
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = emptyList(),
      exclusionMessage = "n/a",
      restrictionMessage = "n/a",
    )

    val restrictions = laoService.getCrnRestrictions("user1", crn)
    assert(!restrictions.hasRestriction)
    assert(!restrictions.hasExclusion)
    assert(!restrictions.apopUserExcluded)
  }

  @Test
  fun `returns correct restrictions object for a case that has some user excluded`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = laoService.getCrnRestrictions("not fred", crn)
    assert(!restrictions.hasRestriction)
    assert(restrictions.hasExclusion)
    assert(!restrictions.apopUserExcluded)
  }

  @Test
  fun `returns correct restrictions object for a case that is restricted to certain users`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns
      DeliusAccessRestrictionDetails(
        crn = crn,
        restrictedTo = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
        excludedFrom = emptyList(),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )
    val restrictions = laoService.getCrnRestrictions("not fred", crn)
    assert(restrictions.hasRestriction)
    assert(!restrictions.hasExclusion)
    assert(restrictions.apopUserExcluded)
  }

  @Test
  fun `returns correct restrictions object for a case that has excluded users including current user`() = runTest {
    val crn = "X1234567"

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "Not fred", staffCode = "12345"), DeliusApopUser(username = "fred", staffCode = "12345")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = laoService.getCrnRestrictions("fred", crn)
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

    coEvery {
      workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(
        crn,
      )
    } returns deliusAccessRestrictionDetails

    listOf("123", "456", "789").forEach { staffCode ->
      coEvery { workforceAllocationsToDeliusApiClient.getOfficerView(staffCode) } returns OfficerView(
        staffCode,
        Name("John", "Paul", "Smith"),
        "PO",
        "email@email.com",
        1,
        1,
        1,
      )
    }

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
  fun `returns correct restrictions when no users are not restricted and we have a dummy user`() = runTest {
    val crn = "X1234567"
    val deliusAccessRestrictionDetails = DeliusAccessRestrictionDetails(
      crn = crn,
      excludedFrom = emptyList(),
      restrictedTo = emptyList(),
      exclusionMessage = "",
      restrictionMessage = "",
    )

    coEvery {
      workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(
        crn,
      )
    } returns deliusAccessRestrictionDetails

    coEvery { workforceAllocationsToDeliusApiClient.getOfficerView("DUMMY") } throws EntityNotFoundException("not found")

    listOf("123", "456", "789").forEach { staffCode ->
      coEvery { workforceAllocationsToDeliusApiClient.getOfficerView(staffCode) } returns OfficerView(
        staffCode,
        Name("John", "Paul", "Smith"),
        "PO",
        "email@email.com",
        1,
        1,
        1,
      )
    }

    val crnStaffRestrictions = laoService.getCrnRestrictionsForUsers(crn, listOf("123", "456", "789", "DUMMY"))

    assert(crnStaffRestrictions.staffRestrictions.size == 4)
    assert(!crnStaffRestrictions.staffRestrictions.get(0).isExcluded)
    assert(!crnStaffRestrictions.staffRestrictions.get(1).isExcluded)
    assert(!crnStaffRestrictions.staffRestrictions.get(2).isExcluded)
    assert(crnStaffRestrictions.staffRestrictions.get(3).isExcluded)
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

    coEvery {
      workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn)
    } returns deliusAccessRestrictionDetails

    listOf("123", "456", "789").forEach { staffCode ->
      coEvery { workforceAllocationsToDeliusApiClient.getOfficerView(staffCode) } returns OfficerView(
        staffCode,
        Name("John", "Paul", "Smith"),
        "PO",
        "email@email.com",
        1,
        1,
        1,
      )
    }

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
      excludedFrom = listOf(
        DeliusApopUser("user1", "456"),
        DeliusApopUser("user2", "789"),
        DeliusApopUser("user3", "212"),
      ),
      restrictedTo = emptyList(),
      exclusionMessage = "",
      restrictionMessage = "",
    )

    coEvery {
      workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(
        crn,
      )
    } returns deliusAccessRestrictionDetails

    listOf("123", "456", "789").forEach { staffCode ->
      coEvery { workforceAllocationsToDeliusApiClient.getOfficerView(staffCode) } returns OfficerView(
        staffCode,
        Name("John", "Paul", "Smith"),
        "PO",
        "email@email.com",
        1,
        1,
        1,
      )
    }

    val crnStaffRestrictions = laoService.getCrnRestrictionsForUsers(crn, listOf("123", "456"))

    assert(crnStaffRestrictions.staffRestrictions.size == 2)
    assert(!crnStaffRestrictions.staffRestrictions.get(0).isExcluded)
    assert(crnStaffRestrictions.staffRestrictions.get(1).isExcluded)
    assert(crnStaffRestrictions.staffRestrictions.get(0).staffCode == "123")
    assert(crnStaffRestrictions.staffRestrictions.get(1).staffCode == "456")
  }

  @Test
  fun `returns correct restrictions some users are in the restricted To list `() = runTest {
    val crn = "X1234567"
    val deliusAccessRestrictionDetails = DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = listOf(
        DeliusApopUser("user1", "123"),
      ),
      excludedFrom = emptyList(),
      exclusionMessage = "",
      restrictionMessage = "",
    )

    coEvery {
      workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(
        crn,
      )
    } returns deliusAccessRestrictionDetails

    listOf("123", "456", "789").forEach { staffCode ->
      coEvery { workforceAllocationsToDeliusApiClient.getOfficerView(staffCode) } returns OfficerView(
        staffCode,
        Name("John", "Paul", "Smith"),
        "PO",
        "email@email.com",
        1,
        1,
        1,
      )
    }

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

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns deliusAccessRestrictionDetails

    assert(!laoService.isCrnRestricted("anybody", crn))
  }

  @Test
  fun `is restricted is when there are exclusions`() = runTest {
    val crn = "X1234567"

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "excludedUser", staffCode = "non apop")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    assert(laoService.isCrnRestricted("not", crn))
  }

  @Test
  fun `is forbidden when user is excluded `() = runTest {
    val crn = "X1234567"

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "excludedUser", staffCode = "non apop")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    assertThrows<NotAllowedForLAOException> { (laoService.isCrnRestricted("excludedUser", crn)) }
  }

  @Test
  fun `throws 403 when case is restricted and user not on list`() = runTest {
    val crn = "X1234567"

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = listOf(DeliusApopUser(username = "JamesBond", staffCode = "007")),
      excludedFrom = emptyList(),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    assertThrows<NotAllowedForLAOException> { (laoService.isCrnRestricted("NotOnAllowedList", crn)) }
  }

  @Test
  fun `throws 403 when the user is excluded`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "excludeduser", staffCode = "apop"), DeliusApopUser(username = "anotherexcludeduser", staffCode = "apop2")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    assertThrows<NotAllowedForLAOException> { (laoService.isCrnRestricted("excludeduser", crn)) }
  }

  @Test
  fun `correctly flags that a crn is LAO when there are restrictions `() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      excludedFrom = emptyList(),
      restrictedTo = listOf(DeliusApopUser(username = "allowedUser", staffCode = "apop"), DeliusApopUser(username = "anotheruser", staffCode = "apop2")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    assert(laoService.isCrnRestricted("allowedUser", crn))
  }

  @Test
  fun `correctly flags that a crn is LAO when there are exclusions`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "excludeduser", staffCode = "apop"), DeliusApopUser(username = "anotherexcludeduser", staffCode = "apop2")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    assert(laoService.isCrnRestricted("allowedUser", crn))
  }

  @Test
  fun `returns correct restriction status object for a case that has no restrictions`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = emptyList(),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = laoService.getCrnRestrictionStatus("user", crn)
    assert(restrictions.crn == crn)
    assert(!restrictions.isRedacted)
    assert(!restrictions.isRestricted)
  }

  @Test
  fun `returns correct restriction status object for a case that has some user excluded`() = runTest {
    val crn = "X1234567"

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = laoService.getCrnRestrictionStatus("alloweduser", crn)
    assert(restrictions.crn == crn)
    assert(!restrictions.isRedacted)
    assert(restrictions.isRestricted)
  }

  fun `returns correct restriction status object for a case that has apop user excluded`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns DeliusAccessRestrictionDetails(
      crn = crn,
      restrictedTo = emptyList(),
      excludedFrom = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
      exclusionMessage = "sorry",
      restrictionMessage = "sorry",
    )

    val restrictions = laoService.getCrnRestrictionStatus("fred", crn)
    assert(restrictions.crn == crn)
    assert(!restrictions.isRedacted)
    assert(restrictions.isRestricted)
  }

  @Test
  fun `returns correct restriction status object for a case that is restricted to certain users`() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns
      DeliusAccessRestrictionDetails(
        crn = crn,
        restrictedTo = listOf(DeliusApopUser(username = "fred", staffCode = "12345")),
        excludedFrom = emptyList(),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )
    val restrictions = laoService.getCrnRestrictionStatus("NotOnListOfAllowed", crn)
    assert(restrictions.crn == crn)
    assert(restrictions.isRestricted)
    assert(restrictions.isRedacted)
  }

  @Test
  fun `returns correct restriction status object for a case that is restricted to certain users including current user `() = runTest {
    val crn = "X1234567"
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn(crn) } returns
      DeliusAccessRestrictionDetails(
        crn = crn,
        restrictedTo = listOf(DeliusApopUser(username = "AllowedUser", staffCode = "12345")),
        excludedFrom = emptyList(),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )
    val restrictions = laoService.getCrnRestrictionStatus("AllowedUser", crn)
    assert(restrictions.crn == crn)
    assert(restrictions.isRestricted)
    assert(!restrictions.isRedacted)
  }

  @Test
  fun `returns correct restriction statuses object for a a list of cases`() = runTest {
    val crns = listOf("crn1", "crn2", "crn3", "crn4", "crn5", "crn6", "crn7", "crn8", "crn9", "crn10", "BadCrn")
    val currentUser = "ThisUser"

    val deliusUserAccess = DeliusUserAccess(
      access = listOf(
        DeliusCaseAccess(crn = "crn1", userRestricted = false, userExcluded = false), // no restrictions
        DeliusCaseAccess(crn = "crn2", userRestricted = false, userExcluded = false), // no restrictions
        DeliusCaseAccess(crn = "crn3", userRestricted = false, userExcluded = true), // excluded
        DeliusCaseAccess(crn = "crn4", userRestricted = true, userExcluded = false), // restricted but we are in list so not
        DeliusCaseAccess(crn = "crn5", userRestricted = false, userExcluded = true), // excluded
        DeliusCaseAccess(crn = "crn6", userRestricted = false, userExcluded = false), // no restriction
        DeliusCaseAccess(crn = "crn7", userRestricted = false, userExcluded = false), // no restrictions
        DeliusCaseAccess(crn = "crn8", userRestricted = false, userExcluded = true), // excluded
        DeliusCaseAccess(crn = "crn9", userRestricted = true, userExcluded = false), // restricted
        DeliusCaseAccess(crn = "crn10", userRestricted = false, userExcluded = true), // excluded
        DeliusCaseAccess(crn = "crn11", userRestricted = true, userExcluded = true), // excluded AND restricted
        DeliusCaseAccess(crn = "BadCrn", userRestricted = false, userExcluded = false), // NOt an existing crn Deius returns fal and false
      ),
    )

    // Unrestricted ones
    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn1") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn1",
        restrictedTo = emptyList(),
        excludedFrom = emptyList(),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn2") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn2",
        restrictedTo = emptyList(),
        excludedFrom = emptyList(),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn6") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn6",
        restrictedTo = emptyList(),
        excludedFrom = emptyList(),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn7") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn7",
        restrictedTo = emptyList(),
        excludedFrom = emptyList(),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn3") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn3",
        restrictedTo = emptyList(),
        excludedFrom = listOf(DeliusApopUser(username = currentUser, staffCode = "12343")), // not curent user
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn4") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn4",
        restrictedTo = listOf(DeliusApopUser(username = currentUser, staffCode = "12343")),
        excludedFrom = emptyList(), // APoP user
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

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn9") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn9",
        restrictedTo = listOf(DeliusApopUser(username = currentUser, staffCode = "12345")),
        excludedFrom = emptyList(),
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

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn11") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn11",
        restrictedTo = listOf(DeliusApopUser(username = currentUser, staffCode = "Notstaff")),
        excludedFrom = listOf(DeliusApopUser(username = currentUser, staffCode = "Notstaff")),
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("BadCrn") } throws WebClientResponseException(HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccess(crns) } returns
      deliusUserAccess

    val restrictions = laoService.getCrnRestrictions(currentUser, crns)

    val checkDeliusUserAccess = DeliusUserAccess(
      access = listOf(
        DeliusCaseAccess(crn = "crn1", userRestricted = false, userExcluded = false),
        DeliusCaseAccess(crn = "crn2", userRestricted = false, userExcluded = false),
        DeliusCaseAccess(crn = "crn3", userRestricted = true, userExcluded = true), // we are  exclusion list - so restricted
        DeliusCaseAccess(crn = "crn4", userRestricted = false, userExcluded = true), // restricted but we are in list so not restricted
        DeliusCaseAccess(crn = "crn5", userRestricted = false, userExcluded = true),
        DeliusCaseAccess(crn = "crn6", userRestricted = false, userExcluded = false),
        DeliusCaseAccess(crn = "crn7", userRestricted = false, userExcluded = false),
        DeliusCaseAccess(crn = "crn8", userRestricted = false, userExcluded = true), // We are not on exclusion list
        DeliusCaseAccess(crn = "crn9", userRestricted = false, userExcluded = true), // Not restricted because we are on the restricted list
        DeliusCaseAccess(crn = "crn10", userRestricted = false, userExcluded = true),
        DeliusCaseAccess(crn = "crn11", userRestricted = true, userExcluded = true), // user on both lists thus restricted
        DeliusCaseAccess(crn = "BadCrn", userRestricted = true, userExcluded = true), // CRN dos not exist

      ),
    )
    assert(restrictions.equals(checkDeliusUserAccess))
  }

  @Test
  fun `returns correct restriction statuses object for another  list of cases`() = runTest {
    val crns = listOf("crn1", "crn2")
    val currentUser = "ThisUser"
    val otherUser = "anotherUser"

    val deliusUserAccess = DeliusUserAccess(
      access = listOf(
        DeliusCaseAccess(crn = "crn1", userRestricted = true, userExcluded = false), // no restrictions
        DeliusCaseAccess(crn = "crn2", userRestricted = false, userExcluded = true), // no restrictions
      ),
    )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn1") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn1",
        restrictedTo = listOf(DeliusApopUser(username = currentUser.lowercase(), staffCode = "12343"), DeliusApopUser(username = "restrictedUser", staffCode = "12343")),
        excludedFrom = emptyList(), //  curent user
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccessRestrictionsByCrn("crn2") } returns
      DeliusAccessRestrictionDetails(
        crn = "crn2",
        restrictedTo = emptyList(),
        excludedFrom = listOf(DeliusApopUser(username = otherUser.uppercase(), staffCode = "42343")), // APoP user
        exclusionMessage = "sorry",
        restrictionMessage = "sorry",
      )

    coEvery { workforceAllocationsToDeliusApiClient.getUserAccess(crns) } returns
      deliusUserAccess

    val restrictions = laoService.getCrnRestrictions(currentUser, crns)

    val checkDeliusUserAccess = DeliusUserAccess(
      access = listOf(
        DeliusCaseAccess(crn = "crn1", userRestricted = false, userExcluded = true),
        DeliusCaseAccess(crn = "crn2", userRestricted = false, userExcluded = true),
      ),
    )
    assert(restrictions.equals(checkDeliusUserAccess))
  }
}
