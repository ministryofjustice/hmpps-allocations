package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.projection.ConvictionIdOnly

@Repository
interface UnallocatedCasesRepository : CrudRepository<UnallocatedCaseEntity, Long> {
  fun existsByCrn(crn: String): Boolean
  fun findByCrn(crn: String): List<UnallocatedCaseEntity>
  fun findFirstCaseByCrn(crn: String): UnallocatedCaseEntity?
  fun findCaseByCrnAndConvictionId(crn: String, convictionId: Long): UnallocatedCaseEntity?
  fun findConvictionIdsByCrn(crn: String): List<ConvictionIdOnly>
}
