package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

@Service
class JpaBasedUpsertUnallocatedCaseService(
  private val repository: UnallocatedCasesRepository,
  @Qualifier("communityApiClient") private val communityApiClient: CommunityApiClient,
  private val enrichEventService: EnrichEventService
) : UpsertUnallocatedCaseService {
  override fun upsertUnallocatedCase(crn: String, convictionId: Long) {
    communityApiClient.getConviction(crn, convictionId)
      .block()?.sentence?.let { sentence ->
        enrichEventService.getTier(crn)?.let { tier ->
          val initialAppointment = enrichEventService.getInitialAppointmentDate(crn, sentence.startDate)
          val name = enrichEventService.getOffenderName(crn)
          val (status, previousConvictionDate, offenderManagerDetails) = enrichEventService.getProbationStatus(crn)

          val unallocatedCase = UnallocatedCaseEntity(
            null, name,
            crn, tier, sentence.startDate, initialAppointment, status.status, previousConvictionDate,
            offenderManagerSurname = offenderManagerDetails?.surname,
            offenderManagerForename = offenderManagerDetails?.forenames,
            offenderManagerGrade = offenderManagerDetails?.grade,
            convictionId = convictionId
          )

          repository.save(unallocatedCase)
        } ?: run {
          log.info("no tier for crn $crn so unable to allocate")
        }
      } ?: run {
      log.info("no sentence for crn $crn so unable to allocate")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
