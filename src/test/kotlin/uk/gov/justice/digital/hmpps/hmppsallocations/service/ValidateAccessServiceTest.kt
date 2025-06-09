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
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Dataset
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusTeams
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Lau
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Pdu
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ProbationEstateRegionAndTeamOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.Provider
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionList
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TeamOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TeamWithLau
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.UnallocatedEvents
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.NotAllowedForAccessException
import kotlin.collections.listOf

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
  fun `returns a 404 if probation estate not found`() = runTest {
    val crn = "X123456"
    val staffId = "KennySmith1"
    val convictionNumber = "1"
    val region = "N54"
    val teamCode = "N54ERET"
    coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn) } returns null
    coEvery { probationEstateApiClient.getRegionsAndTeams(any()) } returns emptyList()
    coEvery { regionsService.getRegionsByUser(staffId) } returns RegionList(listOf(region))
    assertThrows<EntityNotFoundException> { validateAccessService.validateUserAccess(staffId, crn, convictionNumber) }
  }

  @Test
  fun `returns a 404 if regions service returns empty list`() = runTest {
    val crn = "X123456"
    val staffId = "KennySmith1"
    val convictionNumber = "1"
    val region = "N54"
    val teamCode = "N54ERET"
    coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn) } returns null
    coEvery { probationEstateApiClient.getRegionsAndTeams(any()) } returns emptyList()
    coEvery { regionsService.getRegionsByUser(staffId) } returns RegionList(emptyList())
    assertThrows<EntityNotFoundException> { validateAccessService.validateUserAccess(staffId, crn, convictionNumber) }
  }

  @Test
  fun `returns true for allowed access`() = runTest {
    val crn = "X123456"
    val teamCode = "N54ERET"
    val region = "N54"
    val staffId = "KennySmith1"
    val convictionNumber = "1"
    coEvery { workforceAllocationsToDeliusApiClient.getUnallocatedEvents(crn) } returns UnallocatedEvents(
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
    coEvery { probationEstateApiClient.getRegionsAndTeams(setOf(teamCode)) } returns listOf(
      ProbationEstateRegionAndTeamOverview(RegionOverview(region, "Test Region"), TeamOverview(teamCode, "Test Team")),
    )
    coEvery { regionsService.getRegionsByUser(staffId) } returns RegionList(listOf(region))

    val actualResult = validateAccessService.validateUserAccess(staffId, crn, convictionNumber)

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
    coEvery { probationEstateApiClient.getRegionsAndTeams(setOf(teamCode)) } returns listOf(
      ProbationEstateRegionAndTeamOverview(RegionOverview(region, "Test Region"), TeamOverview(teamCode, "Test Team")),
    )
    coEvery { regionsService.getRegionsByUser(staffId) } returns RegionList(listOf(otherRegion))

    assertThrows<NotAllowedForAccessException> { validateAccessService.validateUserAccess(staffId, crn, convictionNumber) }
  }

  @Test
  fun `throws exception for no access to region`() = runTest {
    val region = "N54"
    val otherRegion = "N55"
    val staffId = "KennySmith2"
    coEvery { workforceAllocationsToDeliusApiClient.getTeamsByUsername(any()) } returns DeliusTeams(
      listOf(Dataset(region, "hello"), Dataset(otherRegion, "goodbye")),
      emptyList<TeamWithLau>(),
    )

    assertThrows<NotAllowedForAccessException> { validateAccessService.validateUserRegionAccess(staffId, "InvalidRegion") }
  }

  @Test
  fun `returns true when access to region`() = runTest {
    val region = "N54"
    val otherRegion = "N55"
    val staffId = "KennySmith2"

    coEvery { workforceAllocationsToDeliusApiClient.getTeamsByUsername(any()) } returns DeliusTeams(
      listOf(Dataset(region, "hello"), Dataset(otherRegion, "goodbye")),
      emptyList<TeamWithLau>(),
    )
    assert(validateAccessService.validateUserRegionAccess(staffId, region))
  }

  @Test
  fun `throws exception when no access to pdu`() = runTest {
    val region = "N54"
    val unallowedRegion = "XX55"
    val otherRegion = "N55"
    val staffId = "KennySmith2"
    val pdu = "PDU1"

    coEvery { workforceAllocationsToDeliusApiClient.getTeamsByUsername(any()) } returns DeliusTeams(
      listOf(
        Dataset(region, "Mids"),
        Dataset(otherRegion, "East"),
        Dataset("N55", "West"),
      ),
      listOf(
        TeamWithLau("a1", "N53 desc", Lau("lau1", "lauDesc", Pdu("NotThisOne", "flintstone", Provider(region, "Mids")))),
        TeamWithLau("a2", "Camelot", Lau("lau1", "lauDesc", Pdu("NorThisOne", "flintstone", Provider(region, "East")))),
        TeamWithLau("a3", "Camelot", Lau("lau3", "Lakes", Pdu(pdu, "flintstone", Provider(unallowedRegion, "East")))),
        TeamWithLau("Na455", "Camelot", Lau("lau3", "Lakes", Pdu("NorThisOne", "flintstone", Provider(region, "West")))),
      ),
    )

    assertThrows<NotAllowedForAccessException> { validateAccessService.validateUserAccess(staffId, "InvalidRegion") }
  }

  @Test
  fun `returns true when access allowed for pdu`() = runTest {
    val region = "N54"
    val unallowedRegion = "XX55"
    val otherRegion = "N55"
    val staffId = "KennySmith2"
    val pdu = "PDU1"

    coEvery { workforceAllocationsToDeliusApiClient.getTeamsByUsername(any()) } returns DeliusTeams(
      listOf(
        Dataset(region, "Mids"),
        Dataset(otherRegion, "East"),
        Dataset("N55", "West"),
      ),
      listOf(
        TeamWithLau("a1", "N53 desc", Lau("lau1", "lauDesc", Pdu("NotThisOne", "flintstone", Provider(unallowedRegion, "Mids")))),
        TeamWithLau("a2", "Camelot", Lau("lau1", "lauDesc", Pdu("NorThisOne", "flintstone", Provider(unallowedRegion, "East")))),
        TeamWithLau("a3", "Camelot", Lau("lau3", "Lakes", Pdu(pdu, "flintstone", Provider(region, "East")))),
        TeamWithLau("Na455", "Camelot", Lau("lau3", "Lakes", Pdu("NorThisOne", "flintstone", Provider(unallowedRegion, "West")))),
      ),
    )

    assert(validateAccessService.validateUserAccess(staffId, pdu))
  }
}
