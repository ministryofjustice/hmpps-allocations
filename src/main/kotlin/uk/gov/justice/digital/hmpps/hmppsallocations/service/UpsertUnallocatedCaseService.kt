package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OrderManager
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Sentence
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.projection.ConvictionIdentifiers
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDate
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class UpsertUnallocatedCaseService(
  private val repository: UnallocatedCasesRepository,
  @Qualifier("communityApiClient") private val communityApiClient: CommunityApiClient,
  private val enrichEventService: EnrichEventService,
  private val caseTypeEngine: CaseTypeEngine,
  private val telemetryService: TelemetryService,

) {
  @Transactional
  fun upsertUnallocatedCase(crn: String, convictionIdentifiers: ConvictionIdentifiers) {
    updateExistingCase(getUnallocatedCase(crn, convictionIdentifiers))?.let {
      if (isNewAllocationDemand(it))
        repository.save(it).also { savedEntity ->
          telemetryService.trackAllocationDemandRaised(savedEntity)
        }
    }
  }

  private fun isNewAllocationDemand(it: UnallocatedCaseEntity) =
    it.id == null

  private fun updateExistingCase(unallocatedCaseEntity: UnallocatedCaseEntity): UnallocatedCaseEntity? =
    communityApiClient.getConviction(unallocatedCaseEntity.crn, unallocatedCaseEntity.convictionId)
      .block()?.let { conviction ->
        val currentOrderManager = conviction.orderManagers.maxByOrNull { it.dateStartOfAllocation ?: LocalDateTime.MIN }
        if (isUnallocated(conviction, currentOrderManager)) {
          val sentence = conviction.sentence!!
          log.info("${unallocatedCaseEntity.crn} is in unallocated")
          enrichEventService.getTier(unallocatedCaseEntity.crn)?.let { tier ->
            val initialAppointment =
              enrichEventService.getInitialAppointmentDate(unallocatedCaseEntity.crn, sentence.startDate)
            val name = enrichEventService.getOffenderName(unallocatedCaseEntity.crn)
            val activeConvictions = enrichEventService.getActiveSentencedConvictions(unallocatedCaseEntity.crn)
            val (status, offenderManagerDetails) = enrichEventService.getProbationStatus(
              unallocatedCaseEntity.crn,
              activeConvictions
            )
            unallocatedCaseEntity.sentenceDate = sentence.startDate
            unallocatedCaseEntity.initialAppointment = initialAppointment
            unallocatedCaseEntity.tier = tier
            unallocatedCaseEntity.name = name
            unallocatedCaseEntity.status = status.status
            unallocatedCaseEntity.offenderManagerSurname = offenderManagerDetails?.surname
            unallocatedCaseEntity.offenderManagerForename = offenderManagerDetails?.forenames
            unallocatedCaseEntity.offenderManagerGrade = offenderManagerDetails?.grade
            unallocatedCaseEntity.caseType =
              caseTypeEngine.getCaseType(activeConvictions, unallocatedCaseEntity.convictionNumber)
            unallocatedCaseEntity.teamCode = currentOrderManager!!.teamCode
            unallocatedCaseEntity.providerCode = currentOrderManager.probationAreaCode
            unallocatedCaseEntity.sentenceLength = getSentenceLength(sentence)

            return unallocatedCaseEntity
          }
        } else {
          log.info("Case for crn ${unallocatedCaseEntity.crn} is not requiring allocation")
          unallocatedCaseEntity.id?.let {
            // previously unallocated now allocated
            repository.deleteById(it)
            telemetryService.trackUnallocatedCaseAllocated(unallocatedCaseEntity)
          }
          return null
        }
      } ?: unallocatedCaseEntity.id?.let {
      log.info("Case for crn ${unallocatedCaseEntity.crn} is not found")
      repository.deleteById(it)
      return null
    }

  private fun getUnallocatedCase(crn: String, convictionIdentifiers: ConvictionIdentifiers): UnallocatedCaseEntity =
    repository.findCaseByCrnAndConvictionNumber(crn, convictionIdentifiers.convictionNumber) ?: UnallocatedCaseEntity(
      name = "",
      crn = crn,
      tier = "",
      sentenceDate = LocalDate.now(),
      status = "",
      convictionId = convictionIdentifiers.convictionId,
      caseType = CaseTypes.UNKNOWN,
      providerCode = "PC1",
      convictionNumber = convictionIdentifiers.convictionNumber
    )

  private fun getSentenceLength(sentence: Sentence): String? =
    sentence.originalLengthUnits?.let { "${sentence.originalLength} $it" }

  private fun isUnallocated(conviction: Conviction, currentOrderManager: OrderManager?): Boolean {
    return currentOrderManager?.isUnallocated ?: false && conviction.active && conviction.sentence != null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
