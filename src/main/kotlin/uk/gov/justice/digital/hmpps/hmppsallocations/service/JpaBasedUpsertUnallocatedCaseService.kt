package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.config.CaseOfficerConfigProperties
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.Conviction
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OrderManager
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import java.time.LocalDate
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class JpaBasedUpsertUnallocatedCaseService(
  private val repository: UnallocatedCasesRepository,
  @Qualifier("communityApiClient") private val communityApiClient: CommunityApiClient,
  private val enrichEventService: EnrichEventService,
  private val caseOfficerConfigProperties: CaseOfficerConfigProperties,
  private val caseTypeEngine: CaseTypeEngine,
  private val telemetryService: TelemetryService,

) : UpsertUnallocatedCaseService {
  @Transactional
  override fun upsertUnallocatedCase(crn: String, convictionId: Long) {
    updateExistingCase(getUnallocatedCase(crn, convictionId))?.let {
      if (it.id == null)
        repository.save(it)
    }
  }

  private fun updateExistingCase(unallocatedCaseEntity: UnallocatedCaseEntity): UnallocatedCaseEntity? = communityApiClient.getConviction(unallocatedCaseEntity.crn, unallocatedCaseEntity.convictionId)
    .block()?.let { conviction ->
      val currentOrderManager = conviction.orderManagers.maxByOrNull { it.dateStartOfAllocation ?: LocalDateTime.MIN }
      if (isUnallocated(conviction, currentOrderManager)) {
        val sentence = conviction.sentence!!
        enrichEventService.getTier(unallocatedCaseEntity.crn)?.let { tier ->
          val initialAppointment = enrichEventService.getInitialAppointmentDate(unallocatedCaseEntity.crn, sentence.startDate)
          val name = enrichEventService.getOffenderName(unallocatedCaseEntity.crn)
          val activeConvictions = enrichEventService.getActiveConvictions(unallocatedCaseEntity.crn)
          val (status, previousConvictionDate, offenderManagerDetails) = enrichEventService.getProbationStatus(unallocatedCaseEntity.crn, activeConvictions)
          val caseType = caseTypeEngine.getCaseType(activeConvictions, unallocatedCaseEntity.convictionId)
          unallocatedCaseEntity.sentenceDate = sentence.startDate
          unallocatedCaseEntity.initialAppointment = initialAppointment
          unallocatedCaseEntity.tier = tier
          unallocatedCaseEntity.name = name
          unallocatedCaseEntity.status = status.status
          unallocatedCaseEntity.previousConvictionDate = previousConvictionDate
          unallocatedCaseEntity.offenderManagerSurname = offenderManagerDetails?.surname
          unallocatedCaseEntity.offenderManagerForename = offenderManagerDetails?.forenames
          unallocatedCaseEntity.offenderManagerGrade = offenderManagerDetails?.grade
          unallocatedCaseEntity.caseType = caseType
          unallocatedCaseEntity.teamCode = currentOrderManager!!.teamCode
          unallocatedCaseEntity.providerCode = currentOrderManager.probationAreaCode
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

  private fun getUnallocatedCase(crn: String, convictionId: Long): UnallocatedCaseEntity = repository.findCaseByCrnAndConvictionId(crn, convictionId)
    ?: UnallocatedCaseEntity.Builder()
      .name("")
      .crn(crn)
      .tier("")
      .sentenceDate(LocalDate.now())
      .status("")
      .convictionId(convictionId)
      .caseType(CaseTypes.UNKNOWN)
      .providerCode("PC1")
      .build()

  private fun isUnallocated(conviction: Conviction, currentOrderManager: OrderManager?): Boolean {
    return caseOfficerConfigProperties.includes.contains(currentOrderManager?.staffCode) && conviction.active && conviction.sentence != null
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
