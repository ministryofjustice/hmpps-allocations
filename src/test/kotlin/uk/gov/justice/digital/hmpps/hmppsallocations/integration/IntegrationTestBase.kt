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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessmentResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderManagerResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderManagerResponseNoGrade
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderSummaryResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveAndInactiveConvictionsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveConvictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveInductionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveRequirementResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleCourtReportResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.twoActiveConvictionsOneNoDateResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.twoActiveConvictionsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.CalculationEventListener
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsUnallocatedCase
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.UUID

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(PER_CLASS)
abstract class IntegrationTestBase {

  var offenderAssessmentApi: ClientAndServer = startClientAndServer(8072)
  var communityApi: ClientAndServer = startClientAndServer(8092)
  var hmppsTier: ClientAndServer = startClientAndServer(8082)
  private var oauthMock: ClientAndServer = startClientAndServer(9090)
  private val gson: Gson = Gson()

  @BeforeEach
  fun `clear queues and database`() {
    repository.deleteAll()
    hmppsDomainSqsClient.purgeQueue(PurgeQueueRequest(hmppsDomainQueue.queueUrl))
    tierCalculationSqsClient.purgeQueue(PurgeQueueRequest(tierCalculationQueue.queueUrl))
    communityApi.reset()
    hmppsTier.reset()
    offenderAssessmentApi.reset()
    setupOauth()
  }

  private val hmppsDomainQueue by lazy { hmppsQueueService.findByQueueId("hmppsdomainqueue") ?: throw MissingQueueException("HmppsQueue hmppsdomainqueue not found") }
  private val tierCalculationQueue by lazy { hmppsQueueService.findByQueueId("tiercalculationqueue") ?: throw MissingQueueException("HmppsQueue tiercalculationqueue not found") }
  private val hmppsDomainTopic by lazy { hmppsQueueService.findByTopicId("hmppsdomaintopic") ?: throw MissingQueueException("HmppsTopic hmppsdomaintopic not found") }
  protected val hmppsDomainSqsClient by lazy { hmppsDomainQueue.sqsClient }
  protected val tierCalculationSqsClient by lazy { tierCalculationQueue.sqsClient }
  protected val hmppsDomainSnsClient by lazy { hmppsDomainTopic.snsClient }
  protected val hmppsDomainTopicArn by lazy { hmppsDomainTopic.arn }

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

  protected fun jsonString(any: Any) = objectMapper.writeValueAsString(any) as String

  protected fun unallocatedCaseEvent(crn: String) = HmppsEvent(
    "ALLOCATION_REQUIRED", 0, "some event description", "http://dummy.com",
    ZonedDateTime.now().format(
      ISO_ZONED_DATE_TIME
    ),
    HmppsUnallocatedCase(crn)
  )

  protected fun tierCalculationEvent(
    crn: String
  ) = CalculationEventListener.CalculationEventData(crn, UUID.randomUUID())

  @AfterAll
  fun tearDownServer() {
    communityApi.stop()
    hmppsTier.stop()
    oauthMock.stop()
    offenderAssessmentApi.stop()
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

  protected fun twoActiveConvictionsOneNoDateResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(twoActiveConvictionsOneNoDateResponse())
    )
  }

  protected fun singleActiveAndInactiveConvictionsResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")
    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveAndInactiveConvictionsResponse())
    )
  }

  protected fun singleCourtReportResponse(crn: String, convictionId: Long) {
    val courtReportsRequest =
      request().withPath("/offenders/crn/$crn/convictions/$convictionId/courtReports")
    communityApi.`when`(courtReportsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleCourtReportResponse())
    )
  }

  protected fun noCourtReportResponse(crn: String, convictionId: Long) {
    val courtReportsRequest =
      request().withPath("/offenders/crn/$crn/convictions/$convictionId/courtReports")
    communityApi.`when`(courtReportsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody("[]")
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

  protected fun allDeliusResponses(crn: String) {
    singleActiveConvictionResponse(crn)
    singleActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
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
