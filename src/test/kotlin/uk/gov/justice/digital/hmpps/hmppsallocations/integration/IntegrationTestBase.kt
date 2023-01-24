package uk.gov.justice.digital.hmpps.hmppsallocations.integration

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import com.microsoft.applicationinsights.core.dependencies.google.gson.Gson
import com.ninjasquad.springmockk.MockkBean
import io.mockk.justRun
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.matchers.Times.exactly
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.notFoundResponse
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.mockserver.model.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseDetailsIntegration
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseViewAddressIntegration
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.activeSentencedAndPreConvictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessmentResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.convictionNoSentenceResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.convictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.inactiveConvictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.multipleRegistrationResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderDetailsNoFixedAbodeResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.ogrsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.riskPredictorResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.roshResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.roshResponseNoOverallRisk
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveAndInactiveConvictionsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveConvictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveInductionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveRequirementResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.twoActiveConvictionsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusCaseViewAddressResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusCaseViewNoCourtReportResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.deliusCaseViewResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.documentsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius.fullDeliusCaseDetailsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.CalculationEventListener.CalculationEventData
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.CalculationEventListener.PersonReference
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.CalculationEventListener.PersonReferenceType
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsOffenderEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.LocalDate

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(PER_CLASS)
abstract class IntegrationTestBase {

  var assessRisksNeedsApi: ClientAndServer = startClientAndServer(8085)
  var offenderAssessmentApi: ClientAndServer = startClientAndServer(8072)
  var communityApi: ClientAndServer = startClientAndServer(8092)
  var hmppsTier: ClientAndServer = startClientAndServer(8082)
  var workforceAllocationsToDelius: ClientAndServer = startClientAndServer(8084)
  private var oauthMock: ClientAndServer = startClientAndServer(9090)
  private val gson: Gson = Gson()

  val firstSentenceDate: LocalDate = LocalDate.now().minusDays(4)
  val firstInitialAppointment: LocalDate = LocalDate.now().plusDays(1)

  val previouslyManagedCase = UnallocatedCaseEntity(
    null,
    "Hannah Francis",
    "J680660",
    "C2",
    LocalDate.now().minusDays(1),
    null,
    convictionId = 987654321,
    caseType = CaseTypes.CUSTODY,
    providerCode = "",
    teamCode = "TEAM1",
    convictionNumber = 4
  )

  fun insertCases() {
    repository.saveAll(
      listOf(
        UnallocatedCaseEntity(
          null, "Dylan Adam Armstrong", "J678910", "C1",
          firstSentenceDate, firstInitialAppointment,
          123456789,
          caseType = CaseTypes.CUSTODY,
          providerCode = "",
          teamCode = "TEAM1",
          sentenceLength = "5 Weeks",
          convictionNumber = 1
        ),
        UnallocatedCaseEntity(
          null,
          "Andrei Edwards",
          "J680648",
          "A1",
          LocalDate.now().minusDays(3),
          LocalDate.now().plusDays(2),
          convictionId = 23456789,
          caseType = CaseTypes.LICENSE,
          providerCode = "",
          teamCode = "TEAM1",
          sentenceLength = "36 Days",
          convictionNumber = 2
        ),
        UnallocatedCaseEntity(
          null,
          "William Jones",
          "X4565764",
          "C1",
          LocalDate.now().minusDays(3),
          LocalDate.now().plusDays(2),
          convictionId = 68793954,
          caseType = CaseTypes.COMMUNITY,
          providerCode = "",
          teamCode = "TEAM1",
          sentenceLength = "16 Months",
          convictionNumber = 3
        ),
        previouslyManagedCase,
        UnallocatedCaseEntity(
          null, "Dylan Adam Armstrong", "J678910", "C1",
          firstSentenceDate, firstInitialAppointment,
          56785493, CaseTypes.CUSTODY,
          providerCode = "",
          teamCode = "TEAM2",
          convictionNumber = 5
        ),
        UnallocatedCaseEntity(
          null,
          "Jim Doe",
          "C3333333",
          "B1",
          LocalDate.now().minusDays(3),
          null,
          convictionId = 86472147892,
          caseType = CaseTypes.COMMUNITY,
          providerCode = "",
          teamCode = "TEAM3",
          convictionNumber = 6
        )

      )
    )
  }

  @BeforeEach
  fun `clear queues and database`() {
    repository.deleteAll()
    tierCalculationSqsClient.purgeQueue(PurgeQueueRequest(tierCalculationQueue.queueUrl))
    hmppsOffenderSqsClient.purgeQueue(PurgeQueueRequest(hmppsOffenderQueue.queueUrl))
    hmppsOffenderSqsDlqClient.purgeQueue(PurgeQueueRequest(hmppsOffenderQueue.dlqUrl))
    communityApi.reset()
    hmppsTier.reset()
    workforceAllocationsToDelius.reset()
    offenderAssessmentApi.reset()
    assessRisksNeedsApi.reset()
    justRun { telemetryClient.trackEvent(any(), any(), any()) }
    setupOauth()
  }

  private val tierCalculationQueue by lazy {
    hmppsQueueService.findByQueueId("tiercalculationqueue")
      ?: throw MissingQueueException("HmppsQueue tiercalculationqueue not found")
  }
  private val hmppsOffenderQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsoffenderqueue")
      ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found")
  }

  private val hmppsDomainTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsdomaintopic")
      ?: throw MissingQueueException("HmppsTopic hmppsdomaintopic not found")
  }
  private val hmppsOffenderTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsoffendertopic")
      ?: throw MissingQueueException("HmppsTopic hmppsoffendertopic not found")
  }

  private val hmppsOffenderSqsDlqClient by lazy { hmppsOffenderQueue.sqsDlqClient as AmazonSQS }

  protected val tierCalculationSqsClient by lazy { tierCalculationQueue.sqsClient }
  protected val hmppsOffenderSqsClient by lazy { hmppsOffenderQueue.sqsClient }

  protected val hmppsDomainSnsClient by lazy { hmppsDomainTopic.snsClient }
  protected val hmppsDomainTopicArn by lazy { hmppsDomainTopic.arn }

  protected val hmppsOffenderSnsClient by lazy { hmppsOffenderTopic.snsClient }
  protected val hmppsOffenderTopicArn by lazy { hmppsOffenderTopic.arn }

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  protected lateinit var repository: UnallocatedCasesRepository

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @MockkBean
  lateinit var telemetryClient: TelemetryClient

  internal fun HttpHeaders.authToken(roles: List<String> = emptyList()) {
    this.setBearerAuth(
      jwtAuthHelper.createJwt(
        subject = "SOME_USER",
        roles = roles,
        clientId = "some-client"
      )
    )
  }

  protected fun countMessagesOnOffenderEventQueue(): Int =
    hmppsOffenderSqsClient.getQueueAttributes(
      hmppsOffenderQueue.queueUrl,
      listOf("ApproximateNumberOfMessages", "ApproximateNumberOfMessagesNotVisible")
    )
      .let {
        (
          it.attributes["ApproximateNumberOfMessages"]?.toInt()
            ?: 0
          ) + (it.attributes["ApproximateNumberOfMessagesNotVisible"]?.toInt() ?: 0)
      }

  protected fun countMessagesOnOffenderEventDeadLetterQueue(): Int =
    hmppsOffenderSqsDlqClient.getQueueAttributes(hmppsOffenderQueue.dlqUrl, listOf("ApproximateNumberOfMessages"))
      .let { it.attributes["ApproximateNumberOfMessages"]?.toInt() ?: 0 }

  protected fun jsonString(any: Any) = objectMapper.writeValueAsString(any) as String

  protected fun offenderEvent(crn: String) = HmppsOffenderEvent(crn)

  protected fun tierCalculationEvent(crn: String) = CalculationEventData(
    PersonReference(listOf(PersonReferenceType("CRN", crn)))
  )

  @AfterAll
  fun tearDownServer() {
    communityApi.stop()
    hmppsTier.stop()
    workforceAllocationsToDelius.stop()
    oauthMock.stop()
    offenderAssessmentApi.stop()
    assessRisksNeedsApi.stop()
    repository.deleteAll()
  }

  fun setupOauth() {
    val response = response().withContentType(APPLICATION_JSON)
      .withBody(gson.toJson(mapOf("access_token" to "ABCDE", "token_type" to "bearer")))
    oauthMock.`when`(request().withPath("/auth/oauth/token")).respond(response)
  }

  protected fun singleActiveConvictionResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions").withQueryStringParameter(Parameter("activeOnly", "true"))

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveConvictionResponse())
    )
  }

  protected fun activeSentenacedAndPreConvictionResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions").withQueryStringParameter(Parameter("activeOnly", "true"))

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(activeSentencedAndPreConvictionResponse())
    )
  }

  protected fun unallocatedConvictionResponse(crn: String, convictionId: Long, staffCode: String = "STFFCDEU") {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(convictionResponse(staffCode))
    )
  }

  protected fun deliusCaseDetailsResponse(vararg caseDetailsIntegrations: CaseDetailsIntegration) {
    val initialAppointmentRequest =
      request().withPath("/allocation-demand")

    workforceAllocationsToDelius.`when`(initialAppointmentRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON)
        .withBody(fullDeliusCaseDetailsResponse(*caseDetailsIntegrations))
    )
  }

  protected fun errorDeliusCaseDetailsResponse() {
    val initialAppointmentRequest =
      request().withPath("/allocation-demand")

    workforceAllocationsToDelius.`when`(initialAppointmentRequest, exactly(1)).respond(notFoundResponse())
  }

  protected fun allocatedConvictionResponse(crn: String, convictionId: Long) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(convictionResponse("STFFCDE"))
    )
  }

  protected fun convictionWithNoSentenceResponse(crn: String, convictionId: Long) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(convictionNoSentenceResponse("STFFCDEU"))
    )
  }

  protected fun inactiveConvictionResponse(crn: String, convictionId: Long) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(inactiveConvictionResponse("STFFCDEU"))
    )
  }

  protected fun notFoundConvictionResponse(crn: String, convictionId: Long) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withStatusCode(NOT_FOUND.value())
    )
  }

  protected fun notFoundAllConvictionResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withStatusCode(NOT_FOUND.value())
    )
  }

  protected fun notFoundActiveConvictionsResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions").withQueryStringParameter(Parameter("activeOnly", "true"))

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withStatusCode(NOT_FOUND.value())
    )
  }

  protected fun singleActiveRequirementResponse(crn: String, convictionId: Long) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions/$convictionId/requirements").withQueryStringParameters(
        Parameter("activeOnly", "true"),
        Parameter("excludeSoftDeleted", "true")
      )

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveRequirementResponse())
    )
  }

  protected fun singleActiveConvictionResponseForAllConvictions(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveConvictionResponse())
    )
  }

  protected fun singleActiveInductionResponse(crn: String) {
    val inductionRequest =
      request().withPath("/offenders/crn/$crn/contact-summary/inductions")

    communityApi.`when`(inductionRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveInductionResponse())
    )
  }

  protected fun noActiveInductionResponse(crn: String) {
    val inductionRequest =
      request().withPath("/offenders/crn/$crn/contact-summary/inductions")

    communityApi.`when`(inductionRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody("[]")
    )
  }

  protected fun offenderDetailsResponse(crn: String) {
    val summaryRequest =
      request().withPath("/offenders/crn/$crn/all")

    communityApi.`when`(summaryRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(offenderDetailsResponse())
    )
  }

  protected fun offenderDetailsForbiddenResponse(crn: String) {
    val summaryRequest =
      request().withPath("/offenders/crn/$crn/all")

    communityApi.`when`(summaryRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withStatusCode(403)
    )
  }

  protected fun offenderDetailsNoFixedAbodeResponse(crn: String) {
    val summaryRequest =
      request().withPath("/offenders/crn/$crn/all")

    communityApi.`when`(summaryRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(offenderDetailsNoFixedAbodeResponse())
    )
  }

  protected fun tierCalculationResponse(crn: String): HttpRequest {
    val request = request().withPath("/crn/$crn/tier")
    hmppsTier.`when`(request).respond(
      response().withContentType(APPLICATION_JSON).withBody("{\"tierScore\":\"B3\"}")
    )
    return request
  }

  protected fun twoActiveConvictionsResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(twoActiveConvictionsResponse())
    )
  }

  protected fun noConvictionsResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody("[]")
    )
  }

  protected fun singleActiveAndInactiveConvictionsResponse(crn: String, staffCode: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")
    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveAndInactiveConvictionsResponse(staffCode))
    )
  }

  protected fun documentsResponse(crn: String) {
    val preSentenceReportRequest =
      request().withPath("/offenders/$crn/documents")
    workforceAllocationsToDelius.`when`(preSentenceReportRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(documentsResponse())
    )
  }

  protected fun documentsErrorResponse(crn: String) {
    val preSentenceReportRequest =
      request().withPath("/offenders/$crn/documents")
    workforceAllocationsToDelius.`when`(preSentenceReportRequest, exactly(1)).respond(
      response().withStatusCode(NOT_FOUND.value())
    )
  }

  protected fun noDocumentsResponse(crn: String) {
    val preSentenceReportRequest =
      request().withPath("/offenders/$crn/documents")
    workforceAllocationsToDelius.`when`(preSentenceReportRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody("[]")
    )
  }

  protected fun getRegistrationsFromDelius(crn: String) {
    val registrationsRequest =
      request().withPath("/offenders/crn/$crn/registrations")

    communityApi.`when`(registrationsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(multipleRegistrationResponse())
    )
  }

  protected fun noRegistrationsFromDelius(crn: String) {
    val registrationsRequest =
      request().withPath("/offenders/crn/$crn/registrations")

    communityApi.`when`(registrationsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody("{}")
    )
  }

  protected fun getAssessmentsForCrn(crn: String) {
    val needsRequest =
      request().withPath("/offenders/crn/$crn/assessments/summary")
        .withQueryStringParameter(Parameter("assessmentStatus", "COMPLETE"))

    offenderAssessmentApi.`when`(needsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(assessmentResponse())
    )
  }

  protected fun notFoundAssessmentForCrn(crn: String) {
    val needsRequest =
      request().withPath("/offenders/crn/$crn/assessments/summary")
        .withQueryStringParameter(Parameter("assessmentStatus", "COMPLETE"))
    offenderAssessmentApi.`when`(needsRequest, exactly(1)).respond(
      response().withStatusCode(NOT_FOUND.value()).withContentType(APPLICATION_JSON)
    )
  }

  protected fun getRoshForCrn(crn: String) {
    val riskRequest =
      request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(roshResponse())
    )
  }

  protected fun getRoshNoLevelForCrn(crn: String) {
    val riskRequest =
      request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(roshResponseNoOverallRisk())
    )
  }

  protected fun getRoshNotFoundForCrn(crn: String) {
    val riskRequest =
      request().withPath("/risks/crn/$crn/widget")

    assessRisksNeedsApi.`when`(riskRequest, exactly(1)).respond(
      response().withStatusCode(NOT_FOUND.value()).withContentType(APPLICATION_JSON)
    )
  }

  protected fun notFoundOgrsForCrn(crn: String) {
    val riskRequest =
      request().withPath("/offenders/crn/$crn/assessments")

    assessRisksNeedsApi.`when`(riskRequest, exactly(1)).respond(
      response().withStatusCode(NOT_FOUND.value()).withContentType(APPLICATION_JSON)
    )
  }

  protected fun getRiskPredictorsForCrn(crn: String) {
    val riskRequest =
      request().withPath("/risks/crn/$crn/predictors/rsr/history")

    assessRisksNeedsApi.`when`(riskRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(riskPredictorResponse())
    )
  }

  protected fun getOgrsForCrn(crn: String) {
    val ogrsRequest =
      request().withPath("/offenders/crn/$crn/assessments")

    communityApi.`when`(ogrsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(ogrsResponse())
    )
  }

  protected fun getDocument(crn: String, documentId: String) {
    val documentRequest = request().withPath("/offenders/$crn/documents/$documentId")
    workforceAllocationsToDelius.`when`(documentRequest, exactly(1)).respond(
      response()
        .withHeader(HttpHeaders.CONTENT_TYPE, "application/msword;charset=UTF-8")
        .withHeader(HttpHeaders.ACCEPT_RANGES, "bytes")
        .withHeader(HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate")
        .withHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample_word_doc.doc\"")
        .withHeader(HttpHeaders.DATE, "Fri, 05 Jan 2018 09:50:45 GMT")
        .withHeader(HttpHeaders.ETAG, "9514985635950")
        .withHeader(HttpHeaders.LAST_MODIFIED, "Wed, 03 Jan 2018 13:20:35 GMT")
        .withHeader(HttpHeaders.CONTENT_LENGTH, "20992")
        .withBody(ClassPathResource("sample_word_doc.doc").file.readBytes())
    )
  }

  protected fun caseViewResponse(crn: String, convictionNumber: Int) {
    val caseViewRequest = request().withPath("/allocation-demand/$crn/$convictionNumber/case-view")
    workforceAllocationsToDelius.`when`(caseViewRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(deliusCaseViewResponse())
    )
  }

  protected fun caseViewNoCourtReportResponse(crn: String, convictionNumber: Int) {
    val caseViewRequest = request().withPath("/allocation-demand/$crn/$convictionNumber/case-view")
    workforceAllocationsToDelius.`when`(caseViewRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(deliusCaseViewNoCourtReportResponse())
    )
  }

  protected fun caseViewWithMainAddressResponse(crn: String, convictionNumber: Int) {
    val caseViewRequest = request().withPath("/allocation-demand/$crn/$convictionNumber/case-view")
    workforceAllocationsToDelius.`when`(caseViewRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(deliusCaseViewAddressResponse(CaseViewAddressIntegration("Sheffield Towers", "22", "Sheffield Street", "Sheffield", "Yorkshire", "S2 4SU", false, false, "Supported Housing", "2022-08-25")))
    )
  }

  protected fun caseViewWithNoFixedAbodeResponse(crn: String, convictionNumber: Int) {
    val caseViewRequest = request().withPath("/allocation-demand/$crn/$convictionNumber/case-view")
    workforceAllocationsToDelius.`when`(caseViewRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(deliusCaseViewAddressResponse(CaseViewAddressIntegration(noFixedAbode = true, typeVerified = false, typeDescription = "Homeless - rough sleeping", startDate = "2022-08-25")))
    )
  }

  private fun getNumberOfMessagesCurrentlyOnQueue(client: AmazonSQS, queueUrl: String): Int? {
    val queueAttributes = client.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  protected fun whenCalculationQueueIsEmpty() {
    await untilCallTo {
      getNumberOfMessagesCurrentlyOnQueue(
        tierCalculationSqsClient,
        tierCalculationQueue.queueUrl
      )
    } matches { it == 0 }
  }

  protected fun whenCalculationMessageHasBeenProcessed() {
    await untilCallTo {
      getNumberOfMessagesCurrentlyNotVisibleOnQueue(
        tierCalculationSqsClient,
        tierCalculationQueue.queueUrl
      )
    } matches { it == 0 }
  }

  private fun getNumberOfMessagesCurrentlyNotVisibleOnQueue(client: AmazonSQS, queueUrl: String): Int? {
    val queueAttributes = client.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessagesNotVisible"))
    return queueAttributes.attributes["ApproximateNumberOfMessagesNotVisible"]?.toInt()
  }
}
