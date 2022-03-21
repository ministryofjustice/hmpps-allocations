package uk.gov.justice.digital.hmpps.hmppsallocations.client.domain

import com.fasterxml.jackson.annotation.JsonCreator

data class Documents @JsonCreator constructor(
  val documents: List<Document>,
  val convictions: List<DocumentConvictions>
)

data class DocumentConvictions @JsonCreator constructor(
  val convictionId: String,
  val documents: List<Document>
)
