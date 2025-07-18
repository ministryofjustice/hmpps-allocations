package uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.InitialAppointment
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.client.Staff
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

class WorkforceAllocationsToDeliusApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

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

  private val currentMangedByTeam1CaseDetails = CaseDetailsIntegration(
    crn = "J678910",
    eventNumber = "1",
    initialAppointment = InitialAppointment(LocalDate.of(2022, 10, 11), Staff(Name("Beverley", "Rose", "Smith"))),
    probationStatus = "CURRENTLY_MANAGED",
    probationStatusDescription = "Currently managed",
    communityPersonManager = CommunityPersonManager(Name("Beverley", null, "Smith"), "SPO", "TEAM1"),
    handoverDate = null,
  )

  private val currentMangedByTeam2CaseDetails = CaseDetailsIntegration(
    crn = "X6666222",
    eventNumber = "1",
    initialAppointment = InitialAppointment(LocalDate.of(2023, 12, 18), Staff(Name("Beverley", "Rose", "Smith"))),
    probationStatus = "CURRENTLY_MANAGED",
    probationStatusDescription = "Currently managed",
    communityPersonManager = CommunityPersonManager(Name("Joe", null, "Bloggs"), "SPO", "TEAM2"),
    handoverDate = null,
  )

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

  fun setuserAccessToCases(caseAccessList: List<Triple<String, Boolean, Boolean>>) {
    val request = HttpRequest.request()
      .withPath("/users/limited-access")
      .withMethod("POST")
      .withBody(
        jacksonObjectMapper()
          .writeValueAsString(caseAccessList.map { it.first }),
      )

    workforceAllocationsToDelius.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
        .withBody(deliusUserAccessResponse(caseAccessList)),
    )
  }

  fun setuserAccessToCases(caseAccessList: List<Triple<String, Boolean, Boolean>>, username: String) {
    val request = HttpRequest.request()
      .withPath("/users/limited-access/$username")
      .withMethod("POST")
      .withBody(
        jacksonObjectMapper()
          .writeValueAsString(caseAccessList.map { it.first }),
      )

    workforceAllocationsToDelius.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
        .withBody(deliusUserAccessResponse(caseAccessList)),
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

  fun setupTeam1CaseDetails(vararg extraCaseDetailsIntegrations: CaseDetailsIntegration) {
    deliusCaseDetailsResponse(
      currentMangedByTeam1CaseDetails,
      currentMangedByTeam2CaseDetails,
      CaseDetailsIntegration(
        crn = "J680648",
        eventNumber = "2",
        initialAppointment = null,
        probationStatus = "PREVIOUSLY_MANAGED",
        probationStatusDescription = "Previously managed",
        communityPersonManager = CommunityPersonManager(Name("Janie", null, "Jones"), "PO", "TEAM1"),
        handoverDate = null,
      ),
      CaseDetailsIntegration(
        crn = "X4565764",
        eventNumber = "3",
        initialAppointment = InitialAppointment(LocalDate.now(), Staff(Name("Beverley", null, "Smith"))),
        probationStatus = "NEW_TO_PROBATION",
        probationStatusDescription = "New to probation",
        communityPersonManager = CommunityPersonManager(Name("Beverley", null, "Smith"), "SPO", "TEAM1"),
        handoverDate = null,
      ),
      CaseDetailsIntegration(
        crn = "J680660",
        eventNumber = "4",
        initialAppointment = InitialAppointment(LocalDate.now(), Staff(Name("Beverley", "Rose", "Smith"))),
        probationStatus = "PREVIOUSLY_MANAGED",
        probationStatusDescription = "Previously managed",
        communityPersonManager = null,
        handoverDate = null,
      ),
      *extraCaseDetailsIntegrations,
    )
  }

  fun setExcludedUsersByCrn(crns: List<String>) {
    crns.forEach { setExcludedUsersByCrn(it) }
  }

  fun setExcludedUsersByCrn(crn: String) {
    val request = HttpRequest.request()
      .withPath("/person/$crn/limited-access/all")
      .withMethod("GET")

    workforceAllocationsToDelius.`when`(request).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
        .withBody(
          """{
    "crn": "$crn",
    "excludedFrom": [
        {
            "username": "Test2",
            "staffCode": "TomJones"
        },
        {
            "username": "Test3",
            "staffCode": "TS4A127"
        }
    ],
    "restrictedTo": [],
    "exclusionMessage": "You are excluded from viewing this offender record. Please contact a system administrator",
    "restrictionMessage": "This is a restricted offender record. Please contact a system administrator"
}""",

        ),
    )
  }

  fun setExcludedUsersByCrn(crn: String, staffCode: String, username: String = "Test2") {
    val request = HttpRequest.request()
      .withPath("/person/$crn/limited-access/all")
      .withMethod("GET")

    workforceAllocationsToDelius.`when`(request).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
        .withBody(
          """{
    "crn": "$crn",
    "excludedFrom": [
        {
            "username": "$username",
            "staffCode": "$staffCode"
        }
    ],
    "restrictedTo": [],
    "exclusionMessage": "You are excluded from viewing this offender record. Please contact a system administrator",
    "restrictionMessage": "This is a restricted offender record. Please contact a system administrator"
}""",

        ),
    )
  }

  fun setExcludedAndRestrictedUsersCrn(crn: String, staffCode: String, username: String) {
    val request = HttpRequest.request()
      .withPath("/person/$crn/limited-access/all")
      .withMethod("GET")

    workforceAllocationsToDelius.`when`(request).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
        .withBody(
          """{
    "crn": "$crn",
    "excludedFrom": [
        {
            "username": "$username",
            "staffCode": "$staffCode"
        }
    ],
    "restrictedTo": [
            {
            "username": "$username",
            "staffCode": "$staffCode"
        }
    ],
    "exclusionMessage": "You are excluded from viewing this offender record. Please contact a system administrator",
    "restrictionMessage": "This is a restricted offender record. Please contact a system administrator"
}""",

        ),
    )
  }

  fun setRestrictedUsersByCrn(crn: String, staffCode: String, username: String) {
    val request = HttpRequest.request()
      .withPath("/person/$crn/limited-access/all")
      .withMethod("GET")

    workforceAllocationsToDelius.`when`(request).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
        .withBody(
          """{
    "crn": "$crn",
    "excludedFrom": [],
    "restrictedTo": [
           {
            "username": "$username",
            "staffCode": "$staffCode"
        }
    ],
    "exclusionMessage": "You are excluded from viewing this offender record. Please contact a system administrator",
    "restrictionMessage": "This is a restricted offender record. Please contact a system administrator"
}""",

        ),
    )
  }

  fun setNotExcludedUsersByCrn(crn: String) {
    val request = HttpRequest.request()
      .withPath("/person/$crn/limited-access/all")
      .withMethod("GET")

    workforceAllocationsToDelius.`when`(request).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
        .withBody(
          """{
    "crn": "$crn",
    "excludedFrom": [],
    "restrictedTo": [],
    "exclusionMessage": "N/A",
    "restrictionMessage": "N/A"
}""",

        ),
    )
  }

  fun setApopUsers() {
    val request = HttpRequest.request()
      .withPath("/users")
      .withMethod("GET")

    workforceAllocationsToDelius.`when`(request).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
        .withBody("""[{"username": "Test2", "staffCode": "Fred"}]"""),
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

  fun caseDetailsResponseWhereCurrentlyManagedBySameTeam() {
    deliusCaseDetailsResponse(currentMangedByTeam1CaseDetails)
  }

  fun caseDetailsResponseWhereCurrentlyManagedByDifferentTeam() {
    deliusCaseDetailsResponse(currentMangedByTeam2CaseDetails)
  }

  fun caseDetailsResponseIsInternalServerError(): HttpRequest {
    val request = HttpRequest.request()
      .withPath("/allocation-demand")
      .withMethod(HttpMethod.POST.name())

    workforceAllocationsToDelius.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
        .withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()),
    )
    return request
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

  fun allocatedEventResponse(crn: String, convictionNumber: Int) {
    val request = HttpRequest.request()
      .withPath("/allocation-completed/order-manager")
      .withQueryStringParameter("crn", crn)
      .withQueryStringParameter("eventNumber", convictionNumber.toString())
    workforceAllocationsToDelius.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusAllocatedEventResponse()),
    )
  }
}
