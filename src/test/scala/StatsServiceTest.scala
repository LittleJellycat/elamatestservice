import com.stephenn.scalatest.circe.JsonMatchers
import io.circe.Json
import io.circe.parser.parse
import org.scalatest._


class StatsServiceTest extends FlatSpec with Matchers with JsonMatchers {

  it should "successfully merge two valid jsons" in {
    val pricesJsonString: Json = parse(
      """
        |{
        |  "results": {
        |  "1": 4.2,
        |  "2": 3.75
        |  }
        |}
      """.stripMargin).getOrElse(Json.Null)

    val impressionsJsonString: Json = parse(
      """
        |{
        |  "results": {
        |  "1": 4,
        |  "2": 50
        |  }
        |}
      """.stripMargin).getOrElse(Json.Null)

    val statsJson =
      """{
        |"1": {
        |   "impressions": 4,
        |   "price": 4.2,
        |   "spent": 16.8
        |},
        | "2": {
        |   "impressions": 50,
        |   "price": 3.75,
        |   "spent": 187.5
        | }
        |}
      """.stripMargin

    val merged = StatsService.mergeToStats(Some(pricesJsonString), Some(impressionsJsonString))
    merged should matchJson(statsJson)
  }

  it should "intersect jsons" in {
    val pricesJsonString: Json = parse(
      """
        |{
        |  "results": {
        |  "1": 4.2,
        |  "2": 3.75
        |  }
        |}
      """.stripMargin).getOrElse(Json.Null)

    val impressionsJsonString: Json = parse(
      """
        |{
        |  "results": {
        |  "3": 4,
        |  "2": 50
        |  }
        |}
      """.stripMargin).getOrElse(Json.Null)

    val statsJson: String =
      """{
        | "2": {
        |   "impressions": 50,
        |   "price": 3.75,
        |   "spent": 187.5
        | }
        |}
      """.stripMargin

    val merged = StatsService.mergeToStats(Some(pricesJsonString), Some(impressionsJsonString))
    merged should matchJson(statsJson)
  }

  it should "return empty json at disjoint sets" in {
    val pricesJsonString: Json = parse(
      """
        |{
        |  "results": {
        |  "1": 4.2,
        |  "2": 3.75
        |  }
        |}
      """.stripMargin).getOrElse(Json.Null)

    val impressionsJsonString: Json = parse(
      """
        |{
        |  "results": {
        |  "3": 4,
        |  "4": 50
        |  }
        |}
      """.stripMargin).getOrElse(Json.Null)

    val statsJson: String =
      """{
        |}
      """.stripMargin

    val merged = StatsService.mergeToStats(Some(pricesJsonString), Some(impressionsJsonString))
    merged should matchJson(statsJson)
  }

  it should "merge shuffled keys" in {

    val pricesJsonString: Json = parse(
      """
        |{
        |  "results": {
        |  "1": 4.2,
        |  "2": 3.75
        |  }
        |}
      """.stripMargin).getOrElse(Json.Null)

    val impressionsShuffledJsonString: Json = parse(
      """
        |{
        |  "results": {
        |  "2": 50,
        |  "1": 4
        |  }
        |}
      """.stripMargin).getOrElse(Json.Null)

    val statsJson =
      """{
        |"1": {
        |   "impressions": 4,
        |   "price": 4.2,
        |   "spent": 16.8
        |},
        | "2": {
        |   "impressions": 50,
        |   "price": 3.75,
        |   "spent": 187.5
        | }
        |}
      """.stripMargin

    val merged = StatsService.mergeToStats(Some(pricesJsonString), Some(impressionsShuffledJsonString))
    merged should matchJson(statsJson)
  }

  it should "return error at invalid json" in {
    val nullJson = Json.Null
    val merged = StatsService.mergeToStats(Some(nullJson), Some(nullJson))
    merged shouldBe "Invalid data"
  }
}
