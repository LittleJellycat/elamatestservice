import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import io.circe.Json
import io.circe.Json.fromFields
import io.circe.generic.auto._
import io.circe.optics.JsonPath.root
import io.circe.parser.parse
import io.circe.syntax._

import scala.concurrent.{ExecutionContextExecutor, Future}

object StatsService {
  implicit val system: ActorSystem = ActorSystem("elama-test-service")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def getStats(ids: List[Int]): Future[String] = {
    val idTail = ids.map("id=" + _).mkString("&")

    for {
      priceResponse <- Http().singleRequest(requestTemplate("/prices", idTail))
      impressionResponse <- Http().singleRequest(requestTemplate("/impressions", idTail))
      priceContent <- Unmarshal(priceResponse.entity).to[String]
      impressionContent <- Unmarshal(impressionResponse.entity).to[String]
    } yield mergeToStats(parseToOption(priceContent), parseToOption(impressionContent))
  }

  private def parseToOption(str: String) = parse(str).toOption

  private def requestTemplate(destination: String, query: String) = HttpRequest(uri = Uri.from(
    scheme = "http",
    host = AppConfig.outerServiceHost,
    port = AppConfig.outerServicePort,
    path = destination,
    queryString = Some(query)))

  def mergeToStats(parsedPrice: Option[Json], parsedImpression: Option[Json]): String = {
    (objectOrNone(parsedPrice), objectOrNone(parsedImpression)) match {
      case (Some(prices), Some(impressions)) => fromFields(merge(prices.toMap, impressions.toMap)).toString()
      case _ => "Invalid data"
    }
  }

  private val getResult = root.results.json

  private def objectOrNone(maybeJson: Option[Json]) = for {
    json <- maybeJson
    resultJson <- getResult.getOption(json)
    asObject <- resultJson.asObject
  } yield asObject


  private def merge(prices: Map[String, Json], impressions: Map[String, Json]): Iterable[(String, Json)] =
    prices.map { case (id, price) =>
      impressions.get(id).map { impression =>
        val extractedPrice = price.asNumber.map(_.toDouble).getOrElse(0.0)
        val extractedImpression = impression.asNumber.flatMap(_.toInt).getOrElse(0)
        (id, Stats(extractedImpression, extractedPrice, extractedPrice * extractedImpression).asJson)
      }
    }.filter(_.isDefined).map(_.get)

  case class Stats(impressions: Int, price: Double, spent: Double)

}
