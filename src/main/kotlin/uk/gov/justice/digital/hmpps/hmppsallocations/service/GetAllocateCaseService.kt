package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.WorkloadApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.OfficerOverviewAllocateCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.PotentialAllocateCase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.GradeMapper

@Service
class GetAllocateCaseService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository,
  private val workloadApiClient: WorkloadApiClient,
  private val gradeMapper: GradeMapper
) {

  fun getImpactOfAllocation(crn: String, convictionId: Long, offenderManagerCode: String): PotentialAllocateCase? =
    unallocatedCasesRepository.findCaseByCrnAndConvictionId(crn, convictionId)?.let {
      return workloadApiClient.getPotentialCaseLoad(it.tier, it.caseType, offenderManagerCode)
        .map { offenderManagerWorkload ->
          PotentialAllocateCase.from(it, offenderManagerWorkload, gradeMapper.workloadToStaffGrade(offenderManagerWorkload.grade))
        }
        .block()
    }

  fun getOfficerOverview(crn: String, convictionId: Long, offenderManagerCode: String): OfficerOverviewAllocateCase? =
    unallocatedCasesRepository.findCaseByCrnAndConvictionId(crn, convictionId)?.let {
      return workloadApiClient.getOffenderManagerOverview(offenderManagerCode)
        .map { offenderManagerOverview ->
          OfficerOverviewAllocateCase.from(it, offenderManagerOverview, gradeMapper.workloadToStaffGrade(offenderManagerOverview.grade))
        }
        .block()
    }
}
