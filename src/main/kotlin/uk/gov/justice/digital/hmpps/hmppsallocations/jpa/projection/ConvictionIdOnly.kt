package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.projection

import org.springframework.beans.factory.annotation.Value

interface ConvictionIdOnly {
  @Value("#{target.convictionId}")
  fun getConvictionId(): Long
}
