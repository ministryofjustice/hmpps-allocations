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
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseDetailsIntegration
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseViewAddressIntegration
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension.Companion.workforceAllocationsToDelius
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.communityapi.deliusUserAccessResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusAllocatedEventResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusCaseViewAddressResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusCaseViewNoCourtReportResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusCaseViewResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusProbationRecordNoEventsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusProbationRecordSingleActiveEventResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusProbationRecordSingleInactiveEventResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusRiskResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusRiskResponseNoRegistrationsNoOgrs
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusUnallocatedEventsNoActiveEventsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusUnallocatedEventsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.fullDeliusCaseDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.impactResponse
import java.time.LocalDate

class WorkforceAllocationsToDeliusApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {
    lateinit var workforceAllocationsToDelius: WorkforceAllocationsToDeliusMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    workforceAllocationsToDelius = WorkforceAllocationsToDeliusMockServer()
  }
  override fun beforeEach(context: ExtensionContext?) {
    workforceAllocationsToDelius.reset()
  }
  override fun afterAll(context: ExtensionContext?) {
    workforceAllocationsToDelius.stop()
  }
}
class WorkforceAllocationsToDeliusMockServer : ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8084
  }

  private fun deliusCaseDetailsResponse(vararg caseDetailsIntegrations: CaseDetailsIntegration) {
    val initialAppointmentRequest =
      HttpRequest.request().withPath("/allocation-demand")

    workforceAllocationsToDelius.`when`(initialAppointmentRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
        .withBody(fullDeliusCaseDetailsResponse(*caseDetailsIntegrations)),
    )
  }

  fun userHasAccess(crn: String, restricted: Boolean = false, excluded: Boolean = false) {
    val request = HttpRequest.request()
      .withPath("/users/limited-access")
      .withMethod("POST")
      .withBody("[\"$crn\"]")

    workforceAllocationsToDelius.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
        .withBody(deliusUserAccessResponse(crn, restricted, excluded)),
    )
  }

  fun setupTeam1CaseDetails() {
    deliusCaseDetailsResponse(
      CaseDetailsIntegration(
        "J678910",
        "1",
        LocalDate.of(2022, 10, 11),
        "Currently managed",
        CommunityPersonManager(Name("Beverley", null, "Smith"), "SPO"),
      ),
      CaseDetailsIntegration(
        "J680648",
        "2",
        null,
        "Previously managed",
        CommunityPersonManager(Name("Janie", null, "Jones"), "PO"),
      ),
      CaseDetailsIntegration(
        "X4565764",
        "3",
        LocalDate.now(),
        "New to probation",
        CommunityPersonManager(Name("Beverley", null, "Smith"), "SPO"),
      ),
      CaseDetailsIntegration(
        "J680660",
        "4",
        LocalDate.now(),
        "Previously managed",
        null,
      ),
    )
  }

  fun errorDeliusCaseDetailsResponse() {
    val initialAppointmentRequest =
      HttpRequest.request().withPath("/allocation-demand")

    workforceAllocationsToDelius.`when`(
      initialAppointmentRequest,
      Times.exactly(1),
    ).respond(HttpResponse.response().withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()))
  }

  fun documentsResponse(crn: String) {
    val preSentenceReportRequest =
      HttpRequest.request().withPath("/offenders/$crn/documents")
    workforceAllocationsToDelius.`when`(preSentenceReportRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.documentsResponse()),
    )
  }

  fun documentsErrorResponse(crn: String) {
    val preSentenceReportRequest =
      HttpRequest.request().withPath("/offenders/$crn/documents")
    workforceAllocationsToDelius.`when`(preSentenceReportRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()),
    )
  }

  fun getDocument(crn: String, documentId: String) {
    val documentRequest = HttpRequest.request().withPath("/offenders/$crn/documents/$documentId")
    workforceAllocationsToDelius.`when`(documentRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withHeader(HttpHeaders.CONTENT_TYPE, "application/msword;charset=UTF-8")
        .withHeader(HttpHeaders.ACCEPT_RANGES, "bytes")
        .withHeader(HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate")
        .withHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample_word_doc.doc\"")
        .withHeader(HttpHeaders.DATE, "Fri, 05 Jan 2018 09:50:45 GMT")
        .withHeader(HttpHeaders.ETAG, "9514985635950")
        .withHeader(HttpHeaders.LAST_MODIFIED, "Wed, 03 Jan 2018 13:20:35 GMT")
        .withHeader(HttpHeaders.CONTENT_LENGTH, "20992")
        .withBody(ClassPathResource("sample_word_doc.doc").file.readBytes()),
    )
  }

  fun caseViewResponse(crn: String, convictionNumber: Int) {
    val caseViewRequest = HttpRequest.request().withPath("/allocation-demand/$crn/$convictionNumber/case-view")
    workforceAllocationsToDelius.`when`(caseViewRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusCaseViewResponse()),
    )
  }

  fun caseViewNoCourtReportResponse(crn: String, convictionNumber: Int) {
    val caseViewRequest = HttpRequest.request().withPath("/allocation-demand/$crn/$convictionNumber/case-view")
    workforceAllocationsToDelius.`when`(caseViewRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(deliusCaseViewNoCourtReportResponse()),
    )
  }

  fun caseViewWithMainAddressResponse(crn: String, convictionNumber: Int) {
    val caseViewRequest = HttpRequest.request().withPath("/allocation-demand/$crn/$convictionNumber/case-view")
    workforceAllocationsToDelius.`when`(caseViewRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(deliusCaseViewAddressResponse(CaseViewAddressIntegration("Sheffield Towers", "22", "Sheffield Street", "Sheffield", "Yorkshire", "S2 4SU", false, false, "Supported Housing", "2022-08-25"))),
    )
  }

  fun caseViewWithNoFixedAbodeResponse(crn: String, convictionNumber: Int) {
    val caseViewRequest = HttpRequest.request().withPath("/allocation-demand/$crn/$convictionNumber/case-view")
    workforceAllocationsToDelius.`when`(caseViewRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(deliusCaseViewAddressResponse(CaseViewAddressIntegration(noFixedAbode = true, typeVerified = false, typeDescription = "Homeless - rough sleeping", startDate = "2022-08-25"))),
    )
  }

  fun probationRecordSingleInactiveEventReponse(crn: String, convictionNumber: Int) {
    val probationRecordRequest = HttpRequest.request().withPath("/allocation-demand/$crn/$convictionNumber/probation-record")
    workforceAllocationsToDelius.`when`(probationRecordRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(deliusProbationRecordSingleInactiveEventResponse(crn, convictionNumber)),
    )
  }
  fun probationRecordSingleActiveEventReponse(crn: String, convictionNumber: Int) {
    val probationRecordRequest = HttpRequest.request().withPath("/allocation-demand/$crn/$convictionNumber/probation-record")
    workforceAllocationsToDelius.`when`(probationRecordRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(deliusProbationRecordSingleActiveEventResponse(crn, convictionNumber)),
    )
  }

  fun probationRecordNoEventsResponse(crn: String, convictionNumber: Int) {
    val probationRecordRequest = HttpRequest.request().withPath("/allocation-demand/$crn/$convictionNumber/probation-record")
    workforceAllocationsToDelius.`when`(probationRecordRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusProbationRecordNoEventsResponse(crn, convictionNumber)),
    )
  }

  fun riskResponse(crn: String) {
    val riskRequest = HttpRequest.request().withPath("/allocation-demand/$crn/risk")
    workforceAllocationsToDelius.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusRiskResponse()),
    )
  }

  fun riskResponseNoRegistrationsNoOgrs(crn: String) {
    val riskRequest = HttpRequest.request().withPath("/allocation-demand/$crn/risk")
    workforceAllocationsToDelius.`when`(riskRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusRiskResponseNoRegistrationsNoOgrs()),
    )
  }

  fun unallocatedEventsResponse(crn: String) {
    val request = HttpRequest.request().withPath("/allocation-demand/$crn/unallocated-events")
    workforceAllocationsToDelius.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusUnallocatedEventsResponse()),
    )
  }

  fun unallocatedEventsNoActiveEventsResponse(crn: String) {
    val request = HttpRequest.request().withPath("/allocation-demand/$crn/unallocated-events")
    workforceAllocationsToDelius.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusUnallocatedEventsNoActiveEventsResponse()),
    )
  }

  fun unallocatedEventsNotFoundResponse(crn: String) {
    val request = HttpRequest.request().withPath("/allocation-demand/$crn/unallocated-events")
    workforceAllocationsToDelius.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(404),
    )
  }

  fun getImpactResponse(crn: String, staffCode: String) {
    val impactRequest =
      HttpRequest.request()
        .withPath("/allocation-demand/impact").withQueryStringParameter("crn", crn).withQueryStringParameter("staff", staffCode)

    workforceAllocationsToDelius.`when`(impactRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(impactResponse(crn, staffCode)),
    )
  }

  fun getImpactNotFoundResponse(crn: String, staffCode: String) {
    val impactRequest =
      HttpRequest.request()
        .withPath("/allocation-demand/impact").withQueryStringParameter("crn", crn).withQueryStringParameter("staff", staffCode)

    workforceAllocationsToDelius.`when`(impactRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(404)
        .withContentType(MediaType.APPLICATION_JSON).withBody("{\"foo\":\"bar\"}"),
    )
  }

  fun allocatedEventResponse(crn: String) {
    val request = HttpRequest.request()
      .withPath("/allocation-completed/manager")
      .withQueryStringParameter("crn", crn)
    workforceAllocationsToDelius.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusAllocatedEventResponse()),
    )
  }
}
