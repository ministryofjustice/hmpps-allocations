package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseDetail
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsProbationEstateApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.CrnAndTeamCode
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusProbationStatus

@Service
class OutOfAreaTransferService(
  private val hmppsProbationEstateApiClient: HmppsProbationEstateApiClient,
) {
  suspend fun isCaseCurrentlyManagedOutsideOfCurrentTeamsRegion(
    currentTeamCode: String,
    unallocatedCasesFromDelius: DeliusCaseDetail,
  ): Boolean = getCasesThatAreCurrentlyManagedOutsideOfCurrentTeamsRegion(
    currentTeamCode,
    listOf(unallocatedCasesFromDelius),
  ).firstOrNull() != null
  suspend fun getCasesThatAreCurrentlyManagedOutsideOfCurrentTeamsRegion(
    currentTeamCode: String,
    unallocatedCasesFromDelius: List<DeliusCaseDetail>,
  ): List<CrnAndTeamCode> {
    val currentlyManagedCasesCrnAndTeamCodes = unallocatedCasesFromDelius
      .filter {
        DeliusProbationStatus.CURRENTLY_MANAGED.name == it.probationStatus.status &&
          it.communityPersonManager?.teamCode != null
      }
      .map {
        CrnAndTeamCode(
          crn = it.crn,
          teamCode = it.communityPersonManager?.teamCode,
        )
      }
    val currentlyManagedCasesTeamCodes = currentlyManagedCasesCrnAndTeamCodes
      .mapNotNull {
        it.teamCode
      }.toSet()

    if (currentlyManagedCasesTeamCodes.isNotEmpty()) {
      val teamCodesToInvestigate = currentlyManagedCasesTeamCodes.plus(setOf(currentTeamCode))
      val teamsInDifferentRegion = getTeamCodesInDifferentRegion(
        currentTeamCode,
        teamCodesToInvestigate,
      )
      return currentlyManagedCasesCrnAndTeamCodes
        .filter { teamsInDifferentRegion?.contains(it.teamCode) ?: false }
    }
    return emptyList()
  }

  private suspend fun getTeamCodesInDifferentRegion(
    currentTeamCode: String,
    teamCodesToInvestigate: Set<String>,
  ): List<String>? = if (teamCodesToInvestigate.size > 1) {
    hmppsProbationEstateApiClient.getRegionsAndTeams(
      teamCodes = teamCodesToInvestigate,
    )?.let { regionAndTeams ->
      val currentTeamRegionCode = regionAndTeams
        .filter { it.team.code == currentTeamCode }
        .map { it.region.code }
        .firstOrNull()

      val teamsInDifferentRegion = regionAndTeams
        .filter { it.region.code != currentTeamRegionCode }
        .map { it.team.code }

      teamsInDifferentRegion
    }
  } else {
    null
  }
}
