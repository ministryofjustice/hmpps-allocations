package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsProbationEstateApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ActiveEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ProbationEstateRegionAndTeamOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionList
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TeamOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.UnallocatedEvents
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForAccessException

class ValidateAccessServiceTest {
  @MockK
  lateinit var workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient

  @MockK
  lateinit var probationEstateApiClient: HmppsProbationEstateApiClient

  @MockK
  lateinit var regionsService: GetRegionsService

  @InjectMockKs
  lateinit var validateAccessService: ValidateAccessService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `returns true for allowed access`() = runTest {
    val crn = "X123456"
    val teamCode = "N54ERET"
    val region = "N54"
    val staffId = "KennySmith1"
    val convictionNumber = "1"
    coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(any()) } returns UnallocatedEvents(
      crn,
      Name("Bob", "Smith", "Jones"),
      listOf(
        ActiveEvent(
          "1",
          teamCode,
          "PVI",
        ),
      ),
    )
    coEvery { probationEstateApiClient.getRegionsAndTeams(any()) } returns listOf(
      ProbationEstateRegionAndTeamOverview(RegionOverview(region, "Test Region"), TeamOverview(teamCode, "Test Team")),
    )
    coEvery { regionsService.getRegionsByUser(any()) } returns RegionList(listOf(region))

    val actualResult = validateAccessService.validateUserAccess(crn, staffId, convictionNumber)

    assert(actualResult)
  }

  @Test
  fun `throws exception for no access`() = runTest {
    val crn = "X123456"
    val teamCode = "N54ERET"
    val region = "N54"
    val otherRegion = "N55"
    val staffId = "KennySmith2"
    val convictionNumber = "1"
    coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(any()) } returns UnallocatedEvents(
      crn,
      Name("Bob", "Smith", "Jones"),
      listOf(
        ActiveEvent(
          "1",
          teamCode,
          "PVI",
        ),
      ),
    )
    coEvery { probationEstateApiClient.getRegionsAndTeams(any()) } returns listOf(
      ProbationEstateRegionAndTeamOverview(RegionOverview(region, "Test Region"), TeamOverview(teamCode, "Test Team")),
    )
    coEvery { regionsService.getRegionsByUser(any()) } returns RegionList(listOf(otherRegion))

    assertThrows<NotAllowedForAccessException> { validateAccessService.validateUserAccess(crn, staffId, convictionNumber) }
  }
}
