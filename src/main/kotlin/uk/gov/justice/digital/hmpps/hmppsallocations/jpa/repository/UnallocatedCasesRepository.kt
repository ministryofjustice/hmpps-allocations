package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.projection.ConvictionIdOnly
import java.util.Optional

@Repository
interface UnallocatedCasesRepository : CrudRepository<UnallocatedCaseEntity, Long> {
  fun findFirstByCrn(crn: String): Optional<UnallocatedCaseEntity>
  fun findFirstCaseByCrn(crn: String): UnallocatedCaseEntity?
  fun findCaseByCrnAndConvictionId(crn: String, convictionId: Long): UnallocatedCaseEntity?
  fun findConvictionIdsByCrn(crn: String): List<ConvictionIdOnly>
}
