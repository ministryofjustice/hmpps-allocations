package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseDetail
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OffenderManagerDetails
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.CURRENTLY_MANAGED
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.NEW_TO_PROBATION
import uk.gov.justice.digital.hmpps.hmppsallocations.service.ProbationStatusType.PREVIOUSLY_MANAGED
import java.time.LocalDate

@Service
class EnrichEventService(
  @Qualifier("communityApiClient") private val communityApiClient: CommunityApiClient,
  @Qualifier("workforceAllocationsToDeliusApiClientUserEnhanced") private val workforceAllocationToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  @Qualifier("hmppsTierApiClient") private val hmppsTierApiClient: HmppsTierApiClient,
  private val unallocatedCasesRepository: UnallocatedCasesRepository
) {

  fun getInitialAppointmentDate(crn: String, contactsFromDate: LocalDate): LocalDate? {
    return communityApiClient.getInductionContacts(crn, contactsFromDate)
      .mapNotNull { contacts ->
        contacts.maxByOrNull { c -> c.contactStart }?.contactStart?.toLocalDate()
      }
      .block()
  }

  fun getOffenderName(crn: String): String {
    return communityApiClient.getOffenderDetails(crn)
      .map { "${it.firstName} ${it.surname}" }
      .block()!!
  }

  fun getTier(crn: String): String? {
    return hmppsTierApiClient.getTierByCrn(crn)
  }

  fun getActiveSentencedConvictions(crn: String): List<Conviction> {
    return communityApiClient.getActiveConvictions(crn).filter { conviction -> conviction.sentence != null }
      .collectList()
      .block() ?: emptyList()
  }

  fun getProbationStatus(crn: String, activeConvictions: List<Conviction>): ProbationStatus {
    val offenderManager = communityApiClient.getOffenderManagerName(crn)
      .block()!!
    return when {
      activeConvictions.size > 1 && !offenderManager.isUnallocated -> {
        ProbationStatus(CURRENTLY_MANAGED, offenderManagerDetails = OffenderManagerDetails.from(offenderManager))
      }
      else -> {
        val inactiveConvictions = communityApiClient.getInactiveConvictions(crn).block() ?: emptyList()
        return when {
          inactiveConvictions.isNotEmpty() -> {
            val mostRecentInactiveConvictionEndDate =
              inactiveConvictions.filter { c -> c.sentence?.terminationDate != null }
                .map { c -> c.sentence!!.terminationDate!! }
                .maxByOrNull { it }
            ProbationStatus(
              PREVIOUSLY_MANAGED,
              mostRecentInactiveConvictionEndDate,
              OffenderManagerDetails.from(offenderManager)
            )
          }
          else -> ProbationStatus(
            NEW_TO_PROBATION,
            offenderManagerDetails = OffenderManagerDetails.from(offenderManager)
          )
        }
      }
    }
  }

  fun getAllConvictionIdsAssociatedToCrn(crn: String): Flux<Long> =
    Flux.merge(
      Flux.fromIterable(unallocatedCasesRepository.findConvictionIdsByCrn(crn)).map { it.getConvictionId() },
      communityApiClient.getActiveConvictions(crn)
        .map { conviction -> conviction.convictionId }
    ).distinct()

  @Cacheable(CacheConfiguration.INDUCTION_APPOINTMENT_CACHE_NAME)
  fun enrichInductionAppointment(unallocatedCaseEntities: List<UnallocatedCaseEntity>): Flux<UnallocatedCaseEntity> {

    val casesWithInitialAppt: List<DeliusCaseDetail> = workforceAllocationToDeliusApiClient.getInductionContacts(unallocatedCaseEntities).block().cases

    return Flux.fromIterable(
      unallocatedCaseEntities
        .filter { unallocatedCasesRepository.existsById(it.id!!) }
        .map {
          it.initialAppointment = casesWithInitialAppt.firstOrNull { i -> (i.crn == it.crn) && (i.event.number == it.convictionNumber.toString()) }?.initialAppointment?.date
          it
        }
    )
  }
}

data class ProbationStatus(
  val status: ProbationStatusType,
  val previousConvictionDate: LocalDate? = null,
  val offenderManagerDetails: OffenderManagerDetails? = null
)

enum class ProbationStatusType(
  val status: String
) {
  CURRENTLY_MANAGED("Currently managed"),
  PREVIOUSLY_MANAGED("Previously managed"),
  NEW_TO_PROBATION("New to probation")
}
