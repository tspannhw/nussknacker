package pl.touk.nussknacker.engine.lite.components

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}
import io.circe.Json
import io.circe.Json.{fromInt, fromLong}
import org.apache.kafka.clients.producer.ProducerRecord
import org.everit.json.schema.{Schema => EveritSchema}
import org.scalatest.Inside
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import pl.touk.nussknacker.engine.api.CirceUtil
import pl.touk.nussknacker.engine.api.context.ProcessCompilationError.CustomNodeError
import pl.touk.nussknacker.engine.build.ScenarioBuilder
import pl.touk.nussknacker.engine.canonicalgraph.CanonicalProcess
import pl.touk.nussknacker.engine.lite.components.utils.JsonTestData._
import pl.touk.nussknacker.engine.lite.util.test.{KafkaConsumerRecord, LiteKafkaTestScenarioRunner}
import pl.touk.nussknacker.engine.schemedkafka.KafkaUniversalComponentTransformer._
import pl.touk.nussknacker.engine.schemedkafka.encode.ValidationMode
import pl.touk.nussknacker.engine.schemedkafka.schemaregistry.SchemaVersionOption
import pl.touk.nussknacker.engine.util.output.OutputValidatorErrorsMessageFormatter
import pl.touk.nussknacker.engine.util.test.TestScenarioRunner.RunnerResult
import pl.touk.nussknacker.engine.util.test.{RunResult, TestScenarioRunner}
import pl.touk.nussknacker.test.{SpecialSpELElement, ValidatedValuesDetailedMessage}

import java.util.UUID

class UnivseralSourceJsonFunctionalTest extends AnyFunSuite with Matchers with ScalaCheckDrivenPropertyChecks with Inside
  with TableDrivenPropertyChecks with ValidatedValuesDetailedMessage {

  import LiteKafkaComponentProvider._
  import LiteKafkaTestScenarioRunner._
  import SpecialSpELElement._
  import pl.touk.nussknacker.engine.spel.Implicits._

  private val sourceName = "my-source"
  private val sinkName = "my-sink"

  private val runner: LiteKafkaTestScenarioRunner = TestScenarioRunner.kafkaLiteBased().build()

  // TODO: add more tests for primitives, logical tests, unions and so on, add random tests - like in LiteKafkaAvroSchemaFunctionalTest

  test("should test end to end kafka json data at sink / source") {
    val testData = Table(
      ("config", "result"),
      //Primitive integer validations
      // FIXME handle minimum > MIN_VALUE && maximum < MAX_VALUE) as an Integer to make better interoperability between json and avro?
//      (sConfig(fromLong(Integer.MAX_VALUE.toLong + 1), longSchema, integerRangeSchema), invalidTypes("path 'Value' actual: 'Long' expected: 'Integer'")),
      (sConfig(fromInt(1), integerRangeSchema, longSchema), valid(fromInt(1))),
      (sConfig(fromLong(Integer.MAX_VALUE), integerRangeSchema, integerRangeSchema), valid(fromInt(Integer.MAX_VALUE))),
    )

    forAll(testData) { (config: ScenarioConfig, expected: Validated[_, RunResult[_]]) =>
      val results = runWithValueResults(config)
      results shouldBe expected
    }
  }

  private def runWithValueResults(config: ScenarioConfig) =
    runWithResults(config).map(_.mapSuccesses(r => CirceUtil.decodeJsonUnsafe[Json](r.value(), "invalid json string")))

  private def runWithResults(config: ScenarioConfig): RunnerResult[ProducerRecord[String, String]] = {
    val jsonScenario: CanonicalProcess = createScenario(config)
    runner.registerJsonSchema(config.sourceTopic, config.sourceSchema)
    runner.registerJsonSchema(config.sinkTopic, config.sinkSchema)

    val input = KafkaConsumerRecord[String, String](config.sourceTopic, config.inputData.toString())
    runner.runWithStringData(jsonScenario, List(input))
  }

  private def createScenario(config: ScenarioConfig) =
    ScenarioBuilder
      .streamingLite("check json validation")
      .source(sourceName, KafkaUniversalName,
        TopicParamName -> s"'${config.sourceTopic}'",
        SchemaVersionParamName -> s"'${SchemaVersionOption.LatestOptionName}'"
      )
      .emptySink(sinkName, KafkaUniversalName,
        TopicParamName -> s"'${config.sinkTopic}'",
        SchemaVersionParamName -> s"'${SchemaVersionOption.LatestOptionName}'",
        SinkKeyParamName -> "",
        SinkValueParamName -> s"${config.sinkDefinition}",
        SinkRawEditorParamName -> "true",
        SinkValidationModeParameterName -> s"'${config.validationModeName}'"
      )

  private def randomTopic = UUID.randomUUID().toString

  case class ScenarioConfig(topic: String, inputData: Json, sourceSchema: EveritSchema, sinkSchema: EveritSchema, sinkDefinition: String, validationMode: Option[ValidationMode]) {
    lazy val validationModeName: String = validationMode.map(_.name).getOrElse(ValidationMode.strict.name)
    lazy val sourceTopic = s"$topic-input"
    lazy val sinkTopic = s"$topic-output"
  }

  private def valid[T](data: T): Valid[RunResult[T]] =
    Valid(RunResult.success(data))

  private def invalidTypes(typeErrors: String*): Invalid[NonEmptyList[CustomNodeError]] =
    invalid(typeErrors.toList, Nil, Nil)

  private def invalid(typeFieldErrors: List[String], missingFieldsError: List[String], redundantFieldsError: List[String]): Invalid[NonEmptyList[CustomNodeError]] = {
    val finalMessage = OutputValidatorErrorsMessageFormatter.makeMessage(typeFieldErrors, missingFieldsError, redundantFieldsError)
    Invalid(NonEmptyList.one(CustomNodeError(sinkName, finalMessage, Some(SinkValueParamName))))
  }

  private def sConfig(inputData: Json, sourceSchema: EveritSchema, sinkSchema: EveritSchema, output: SpecialSpELElement = Input, validationMode: Option[ValidationMode] = None): ScenarioConfig =
    ScenarioConfig(randomTopic, inputData, sourceSchema, sinkSchema, output.value, validationMode)

}
