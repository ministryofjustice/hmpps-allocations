package uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.AssessRisksNeedsApiExtension.Companion.assessRisksNeedsApi
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.riskPredictorResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.roshResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.roshResponseNoOverallRisk

class AssessRisksNeedsApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {
    lateinit var assessRisksNeedsApi: AssessRisksNeedsMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    assessRisksNeedsApi = AssessRisksNeedsMockServer()
  }

  override fun beforeEach(context: ExtensionContext?) {
    assessRisksNeedsApi.reset()
  }

  override fun afterAll(context: ExtensionContext?) {
    assessRisksNeedsApi.stop()
  }
}
class AssessRisksNeedsMockServer : ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8085
  }

  fun getRoshForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(roshResponse())
    )
  }

  fun getRoshNoLevelForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(roshResponseNoOverallRisk())
    )
  }

  fun getRoshNotFoundForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(MediaType.APPLICATION_JSON)
    )
  }

  fun notFoundOgrsForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/assessments")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(MediaType.APPLICATION_JSON)
    )
  }

  fun getRiskPredictorsForCrn(crn: String) {
    val riskRequest =
      HttpRequest.request().withPath("/risks/crn/$crn/predictors/rsr/history")

    assessRisksNeedsApi.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(riskPredictorResponse())
    )
  }
}
