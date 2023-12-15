package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseDetail
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsProbationEstateApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusProbationStatus

@Service
class OutOfAreaTransferService(
  private val hmppsProbationEstateApiClient: HmppsProbationEstateApiClient,
) {
  suspend fun getCasesThatAreCurrentlyManagedOutsideOfThisTeamsRegion(
    currentTeamCode: String,
    unallocatedCasesFromDelius: List<DeliusCaseDetail>,
  ): List<Pair<String, String?>> {
    val currentlyManagedCasesCrnAndTeamCodePairs = unallocatedCasesFromDelius
      .filter {
        DeliusProbationStatus.CURRENTLY_MANAGED.name == it.probationStatus.status &&
          it.communityPersonManager?.teamCode != null
      }
      .map { it.crn to it.communityPersonManager?.teamCode }

    val currentlyManagedCasesTeamCodes = currentlyManagedCasesCrnAndTeamCodePairs
      .mapNotNull {
        it.second
      }.toSet()

    if (currentlyManagedCasesTeamCodes.isNotEmpty()) {
      val teamCodesToInvestigate = currentlyManagedCasesTeamCodes.plus(setOf(currentTeamCode))
      val teamsInDifferentRegion = getTeamCodesInDifferentRegion(
        currentTeamCode,
        teamCodesToInvestigate,
      )
      return currentlyManagedCasesCrnAndTeamCodePairs
        .filter { teamsInDifferentRegion?.contains(it.second) ?: false }
    }
    return emptyList<Pair<String, String>>()
  }

  private suspend fun getTeamCodesInDifferentRegion(
    currentTeamCode: String,
    teamCodesToInvestigate: Set<String>,
  ) = if (teamCodesToInvestigate.size > 1) {
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
