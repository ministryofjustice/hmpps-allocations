package uk.gov.justice.digital.hmpps.hmppsallocations.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class DefaultGradeMapperTest {

  private val gradeMapper = DefaultGradeMapper()

  @ParameterizedTest
  @MethodSource("deliusGradeMap")
  fun `delius code should map to`(deliusCode: String?, expectedResult: String?) {
    assertThat(gradeMapper.deliusToStaffGrade(deliusCode)).isEqualTo(expectedResult)
  }

  companion object {
    @JvmStatic
    fun deliusGradeMap(): Stream<Arguments> {
      return Stream.of(
        arguments("PSQ", "PSO"),
        arguments("PSP", "PQiP"),
        arguments("PSM", "PO"),
        arguments("PSC", "SPO"),
        arguments("J", null),
        arguments(null, null)
      )
    }
  }
}
