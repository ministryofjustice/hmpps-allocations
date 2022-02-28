package uk.gov.justice.digital.hmpps.hmppsallocations.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "unallocated.cases.officer")
data class CaseOfficerConfigProperties(
  var includes: List<String>
)
