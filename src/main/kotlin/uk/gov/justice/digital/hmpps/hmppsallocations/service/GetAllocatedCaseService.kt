package uk.gov.justice.digital.hmpps.hmppsallocations.service

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.DeliusAllocatedCaseView
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.AllocatedCaseDetails

@Service
class GetAllocatedCaseService(
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced")
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  @Qualifier("laoService")
  private val laoService: LaoService,
  private val tierApiClient: HmppsTierApiClient,
) {
  suspend fun getCase(userName: String, crn: String): AllocatedCaseDetails? {
    val deliusAllocatedCaseView: DeliusAllocatedCaseView = workforceAllocationsToDeliusApiClient
      .getAllocatedDeliusCaseView(crn).awaitSingle()

    val laoDetails = laoService.getCrnRestrictionStatus(userName, crn)
    val tier = tierApiClient.getTierByCrn(crn)

    return AllocatedCaseDetails.from(
      deliusAllocatedCaseView,
      false,
      tier!!,
      crn,
    )
  }
}
