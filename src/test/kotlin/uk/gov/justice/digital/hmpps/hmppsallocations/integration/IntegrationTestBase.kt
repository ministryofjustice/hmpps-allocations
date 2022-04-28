package uk.gov.justice.digital.hmpps.hmppsallocations.integration

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.core.dependencies.google.gson.Gson
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
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.mockserver.model.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.activeSentenacedAndPreConvictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessmentResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.convictionNoSentenceResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.convictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.documentsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.inactiveConvictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.multipleRegistrationResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderManagerResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderManagerResponseNoGrade
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderSummaryResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.ogrsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.riskPredictorResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.riskSummaryNoLevelResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.riskSummaryResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveAndInactiveConvictionsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveConvictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveInductionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveRequirementResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.twoActiveConvictionsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.CalculationEventListener
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsOffenderEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsUnallocatedCase
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.UUID

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(PER_CLASS)
abstract class IntegrationTestBase {

  var assessRisksNeedsApi: ClientAndServer = startClientAndServer(8085)
  var offenderAssessmentApi: ClientAndServer = startClientAndServer(8072)
  var communityApi: ClientAndServer = startClientAndServer(8092)
  var hmppsTier: ClientAndServer = startClientAndServer(8082)
  private var oauthMock: ClientAndServer = startClientAndServer(9090)
  private val gson: Gson = Gson()

  val firstSentenceDate: LocalDate = LocalDate.now().minusDays(4)
  val firstInitialAppointment: LocalDate = LocalDate.now().plusDays(1)
  final val previousConvictionEndDate: LocalDate = LocalDate.now().minusDays(60)

  val previouslyManagedCase = UnallocatedCaseEntity(
    null,
    "Hannah Francis",
    "J680660",
    "C2",
    LocalDate.now().minusDays(1),
    null,
    "Previously managed",
    previousConvictionEndDate,
    convictionId = 987654321,
    caseType = CaseTypes.CUSTODY
  )

  fun insertCases() {
    repository.saveAll(
      listOf(
        UnallocatedCaseEntity(
          null, "Dylan Adam Armstrong", "J678910", "C1",
          firstSentenceDate, firstInitialAppointment, "Currently managed",
          null, "Antonio", "LoSardo", "PO",
          123456789,
          caseType = CaseTypes.CUSTODY
        ),
        UnallocatedCaseEntity(
          null,
          "Andrei Edwards",
          "J680648",
          "A1",
          LocalDate.now().minusDays(3),
          LocalDate.now().plusDays(2),
          "New to probation",
          convictionId = 23456789,
          caseType = CaseTypes.LICENSE
        ),
        previouslyManagedCase,
        UnallocatedCaseEntity(
          null, "Dylan Adam Armstrong", "J678910", "C1",
          firstSentenceDate, firstInitialAppointment, "Currently managed",
          null, "Antonio", "LoSardo", "PO",
          56785493, CaseTypes.CUSTODY
        ),
        UnallocatedCaseEntity(
          null,
          "Jim Doe",
          "C3333333",
          "B1",
          LocalDate.now().minusDays(3),
          null,
          "New to probation",
          convictionId = 86472147892,
          caseType = CaseTypes.COMMUNITY
        )

      )
    )
  }

  @BeforeEach
  fun `clear queues and database`() {
    repository.deleteAll()
    hmppsDomainSqsClient.purgeQueue(PurgeQueueRequest(hmppsDomainQueue.queueUrl))
    tierCalculationSqsClient.purgeQueue(PurgeQueueRequest(tierCalculationQueue.queueUrl))
    hmppsOffenderSqsClient.purgeQueue(PurgeQueueRequest(hmppsOffenderQueue.queueUrl))
    hmppsOffenderSqsDlqClient.purgeQueue(PurgeQueueRequest(hmppsOffenderQueue.dlqUrl))
    communityApi.reset()
    hmppsTier.reset()
    offenderAssessmentApi.reset()
    assessRisksNeedsApi.reset()
    setupOauth()
  }

  private val hmppsDomainQueue by lazy { hmppsQueueService.findByQueueId("hmppsdomainqueue") ?: throw MissingQueueException("HmppsQueue hmppsdomainqueue not found") }
  private val tierCalculationQueue by lazy { hmppsQueueService.findByQueueId("tiercalculationqueue") ?: throw MissingQueueException("HmppsQueue tiercalculationqueue not found") }
  private val hmppsOffenderQueue by lazy { hmppsQueueService.findByQueueId("hmppsoffenderqueue") ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found") }

  private val hmppsDomainTopic by lazy { hmppsQueueService.findByTopicId("hmppsdomaintopic") ?: throw MissingQueueException("HmppsTopic hmppsdomaintopic not found") }
  private val hmppsOffenderTopic by lazy { hmppsQueueService.findByTopicId("hmppsoffendertopic") ?: throw MissingQueueException("HmppsTopic hmppsoffendertopic not found") }

  private val hmppsOffenderSqsDlqClient by lazy { hmppsOffenderQueue.sqsDlqClient as AmazonSQS }

  protected val hmppsDomainSqsClient by lazy { hmppsDomainQueue.sqsClient }
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
    hmppsOffenderSqsClient.getQueueAttributes(hmppsOffenderQueue.queueUrl, listOf("ApproximateNumberOfMessages", "ApproximateNumberOfMessagesNotVisible"))
      .let { (it.attributes["ApproximateNumberOfMessages"]?.toInt() ?: 0) + (it.attributes["ApproximateNumberOfMessagesNotVisible"]?.toInt() ?: 0) }

  protected fun countMessagesOnOffenderEventDeadLetterQueue(): Int =
    hmppsOffenderSqsDlqClient.getQueueAttributes(hmppsOffenderQueue.dlqUrl, listOf("ApproximateNumberOfMessages"))
      .let { it.attributes["ApproximateNumberOfMessages"]?.toInt() ?: 0 }

  protected fun jsonString(any: Any) = objectMapper.writeValueAsString(any) as String

  protected fun unallocatedCaseEvent(crn: String, convictionId: Long) = HmppsEvent(
    "ALLOCATION_REQUIRED", 0, "some event description", "http://dummy.com",
    ZonedDateTime.now().format(
      ISO_ZONED_DATE_TIME
    ),
    HmppsUnallocatedCase(crn, convictionId)
  )

  protected fun offenderEvent(crn: String, convictionId: Long) = HmppsOffenderEvent(crn, convictionId)

  protected fun tierCalculationEvent(
    crn: String
  ) = CalculationEventListener.CalculationEventData(crn, UUID.randomUUID())

  @AfterAll
  fun tearDownServer() {
    communityApi.stop()
    hmppsTier.stop()
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
      response().withContentType(APPLICATION_JSON).withBody(activeSentenacedAndPreConvictionResponse())
    )
  }

  protected fun unallocatedConvictionResponse(crn: String, convictionId: Long) {
    unallocatedConvictionResponse(crn, convictionId, "STFFCDEU")
  }

  protected fun unallocatedConvictionResponse(crn: String, convictionId: Long, staffCode: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(convictionResponse(staffCode))
    )
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
      response().withContentType(APPLICATION_JSON).withStatusCode(HttpStatus.NOT_FOUND.value())
    )
  }

  protected fun singleActiveRequirementResponse(crn: String, convictionId: Long) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions/$convictionId/requirements").withQueryStringParameter(Parameter("activeOnly", "true"))

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

  protected fun offenderSummaryResponse(crn: String) {
    val summaryRequest =
      request().withPath("/offenders/crn/$crn")

    communityApi.`when`(summaryRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(offenderSummaryResponse())
    )
  }

  protected fun noActiveInductionResponse(crn: String) {
    val inductionRequest =
      request().withPath("/offenders/crn/$crn/contact-summary/inductions")

    communityApi.`when`(inductionRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody("[]")
    )
  }

  protected fun notFoundInductionResponse(crn: String) {
    val inductionRequest =
      request().withPath("/offenders/crn/$crn/contact-summary/inductions")

    communityApi.`when`(inductionRequest, exactly(1)).respond(
      response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(APPLICATION_JSON)
    )
  }

  protected fun tierCalculationResponse(crn: String): HttpRequest {
    val request = request().withPath("/crn/$crn/tier")
    hmppsTier.`when`(request).respond(
      response().withContentType(APPLICATION_JSON).withBody("{\"tierScore\":\"B3\"}")
    )
    return request!!
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

  protected fun singleActiveAndInactiveConvictionsResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")
    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveAndInactiveConvictionsResponse())
    )
  }

  protected fun documentsResponse(crn: String, convictionId: Long) {
    val preSentenceReportRequest =
      request().withPath("/offenders/crn/$crn/documents/grouped")
    communityApi.`when`(preSentenceReportRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(documentsResponse(convictionId))
    )
  }

  protected fun noDocumentsResponse(crn: String, convictionId: Long) {
    val preSentenceReportRequest =
      request().withPath("/offenders/crn/$crn/documents/grouped")
    communityApi.`when`(preSentenceReportRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody("{\"documents\": [], \"convictions\":[]}")
    )
  }

  protected fun getStaffWithGradeFromDelius(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/allOffenderManagers")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(offenderManagerResponse())
    )
  }

  protected fun getStaffWithoutGradeFromDelius(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/allOffenderManagers")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(offenderManagerResponseNoGrade())
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
      request().withPath("/offenders/crn/$crn/assessments/summary").withQueryStringParameter(Parameter("assessmentStatus", "COMPLETE"))

    offenderAssessmentApi.`when`(needsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(assessmentResponse())
    )
  }

  protected fun notFoundAssessmentForCrn(crn: String) {
    val needsRequest =
      request().withPath("/offenders/crn/$crn/assessments/summary").withQueryStringParameter(Parameter("assessmentStatus", "COMPLETE"))
    offenderAssessmentApi.`when`(needsRequest, exactly(1)).respond(
      response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(APPLICATION_JSON)
    )
  }

  protected fun getRiskSummaryForCrn(crn: String) {
    val riskRequest =
      request().withPath("/risks/crn/$crn/summary")

    assessRisksNeedsApi.`when`(riskRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(riskSummaryResponse())
    )
  }

  protected fun getRiskSummaryNoLevelForCrn(crn: String) {
    val riskRequest =
      request().withPath("/risks/crn/$crn/summary")

    assessRisksNeedsApi.`when`(riskRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(riskSummaryNoLevelResponse())
    )
  }

  protected fun notFoundRiskSummaryForCrn(crn: String) {
    val riskRequest =
      request().withPath("/risks/crn/$crn/summary")

    assessRisksNeedsApi.`when`(riskRequest, exactly(1)).respond(
      response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(APPLICATION_JSON)
    )
  }

  protected fun notFoundOgrsForCrn(crn: String) {
    val riskRequest =
      request().withPath("/offenders/crn/$crn/assessments")

    assessRisksNeedsApi.`when`(riskRequest, exactly(1)).respond(
      response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(APPLICATION_JSON)
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
    val documentRequest = request().withPath("/offenders/crn/$crn/documents/$documentId")
    communityApi.`when`(documentRequest, exactly(1)).respond(
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

  protected fun allDeliusResponses(crn: String) {
    singleActiveConvictionResponseForAllConvictions(crn)
    unallocatedConvictionResponse(crn, 123456789)
    singleActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    getStaffWithGradeFromDelius(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)
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
