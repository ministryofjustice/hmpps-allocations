package uk.gov.justice.digital.hmpps.hmppsallocations.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.flow.asFlow
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
    unallocatedCasesFromDelius: Flow<DeliusCaseDetail>,
  ): Flow<Pair<String, String?>> {
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
      val teamsInDifferentRegions = hmppsProbationEstateApiClient.getRegionsAndTeams(
        teamCodes = currentlyManagedCasesTeamCodes.plus(setOf(currentTeamCode)),
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
      return currentlyManagedCasesCrnAndTeamCodePairs
        .filter { teamsInDifferentRegions?.contains(it.second) ?: false }
    }
    return emptyList<Pair<String, String>>()
      .asFlow()
  }
}