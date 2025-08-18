package uk.gov.justice.digital.hmpps.hmppsallocations.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseDetail
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsProbationEstateApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.ProbationStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.client.RecentAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.client.RecentManager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.ProbationEstateRegionAndTeamOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.RegionOverview
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.TeamOverview

internal class OutOfAreaTransferServiceTest {

  private val mockHmppsProbationEstateApiClient: HmppsProbationEstateApiClient = mockk()

  private val region1 = RegionOverview(
    code = "REGION1",
    name = "Region 1",
  )
  private val region2 = RegionOverview(
    code = "REGION2",
    name = "Region 2",
  )
  private val region3 = RegionOverview(
    code = "REGION3",
    name = "Region 3",
  )

  private val team1 = TeamOverview(
    code = "TEAM1",
    name = "Team 1",
  )
  private val team2 = TeamOverview(
    code = "TEAM2",
    name = "Team 2",
  )
  private val team3 = TeamOverview(
    code = "TEAM3",
    name = "Team 3",
  )
  private val team4 = TeamOverview(
    code = "TEAM4",
    name = "Team 4",
  )
  private val team5 = TeamOverview(
    code = "TEAM5",
    name = "Team 5",
  )
  private val team6 = TeamOverview(
    code = "TEAM6",
    name = "Team 6",
  )

  private val stubbedUnallocatedCasesFromDelius = listOf(
    stubUnallocatedCasesFromDelius(
      crn = "11111",
      probationStatus = "CURRENTLY_MANAGED",
      communityPersonManagerTeamCode = team2.code,
    ),
    stubUnallocatedCasesFromDelius(
      crn = "22222",
      probationStatus = "CURRENTLY_MANAGED",
      communityPersonManagerTeamCode = team3.code,
    ),
    stubUnallocatedCasesFromDelius(
      crn = "33333",
      probationStatus = "CURRENTLY_MANAGED",
      communityPersonManagerTeamCode = team4.code,
    ),
    stubUnallocatedCasesFromDelius(
      crn = "44444",
      probationStatus = "CURRENTLY_MANAGED",
      communityPersonManagerTeamCode = team5.code,
    ),
    stubUnallocatedCasesFromDelius(
      crn = "55555",
      probationStatus = "CURRENTLY_MANAGED",
      communityPersonManagerTeamCode = team6.code,
    ),
    stubUnallocatedCasesFromDelius(
      crn = "66666",
      probationStatus = "PREVIOUSLY_MANAGED",
      communityPersonManagerTeamCode = "TEAM7",
    ),
    stubUnallocatedCasesFromDelius(
      crn = "77777",
      probationStatus = "NEW_TO_PROBATION",
      communityPersonManagerTeamCode = "TEAM8",
    ),
  )

  private fun stubUnallocatedCasesFromDelius(
    crn: String,
    probationStatus: String,
    communityPersonManagerTeamCode: String,
  ): DeliusCaseDetail = DeliusCaseDetail(
    name = mockk(),
    crn = crn,
    sentence = mockk(),
    initialAppointment = mockk(),
    event = mockk(),
    probationStatus = ProbationStatus(
      status = probationStatus,
      description = probationStatus,
    ),
    communityPersonManager = CommunityPersonManager(
      name = mockk(),
      grade = "",
      teamCode = communityPersonManagerTeamCode,
    ),
    mostRecentAllocatedEvent = RecentAllocatedEvent(
      number = "1",
      manager = RecentManager(
        code = "001",
        name = mockk(),
        teamCode = communityPersonManagerTeamCode,
        grade = "",
        allocated = false,
      ),
    ),
    type = "",
    handoverDate = mockk(),
  )

  @Test
  fun `must not return unallocated cases which get deleted during enrichment`() {
    runBlocking {
      coEvery {
        mockHmppsProbationEstateApiClient.getRegionsAndTeams(
          teamCodes = setOf(team2.code, team3.code, team4.code, team5.code, team6.code, team1.code),
        )
      } returns listOf(
        ProbationEstateRegionAndTeamOverview(
          region = region1,
          team = team1,
        ),
        ProbationEstateRegionAndTeamOverview(
          region = region1,
          team = team2,
        ),
        ProbationEstateRegionAndTeamOverview(
          region = region1,
          team = team3,
        ),
        ProbationEstateRegionAndTeamOverview(
          region = region2,
          team = team4,
        ),
        ProbationEstateRegionAndTeamOverview(
          region = region3,
          team = team5,
        ),
      )
      val results = OutOfAreaTransferService(mockHmppsProbationEstateApiClient)
        .getCasesThatAreCurrentlyManagedOutsideOfCurrentTeamsRegion(
          currentTeamCode = team1.code,
          unallocatedCasesFromDelius = stubbedUnallocatedCasesFromDelius,
        )

      assertEquals(2, results.size)
      assertEquals("33333", results.first().crn)
      assertEquals(team4.code, results.first().teamCode)
      assertEquals("44444", results[1].crn)
      assertEquals(team5.code, results[1].teamCode)
    }
  }
}
