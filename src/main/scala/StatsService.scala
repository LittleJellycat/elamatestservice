import java.util.concurrent.TimeUnit

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.circe.Json
import io.circe.Json.{fromDoubleOrNull, fromFields, fromString, obj}
import io.circe.optics.JsonPath.root
import io.circe.parser.parse

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try

object StatsService {
  def getStats(ids: List[Int]): String = {
    val idTail = ids.map("id=" + _).mkString("&")

    def requestTemplate(destination: String) = HttpRequest(uri = Uri.from(
      scheme = "http",
      host = AppConfig.outerServiceHost,
      port = AppConfig.outerServicePort,
      path = destination,
      queryString = Some(idTail)))

    val futureResult = for {
      priceResponse <- Http().singleRequest(requestTemplate("/prices"))
      impressionResponse <- Http().singleRequest(requestTemplate("/impressions"))
      priceContent <- Unmarshal(priceResponse.entity).to[String]
      impressionContent <- Unmarshal(impressionResponse.entity).to[String]
    } yield mergeToStats(
      parse(priceContent).getOrElse(Json.Null),
      parse(impressionContent).getOrElse(Json.Null))
      .toString()

    Try(Await.result(futureResult, Duration(5, TimeUnit.SECONDS)))
      .getOrElse("Data unavailable")
  }

  def mergeToStats(parsedPrice: Json, parsedImpression: Json): Json = {
    val getResult = root.results.json

    def objectOrNone = getResult.getOption(_).getOrElse(Json.Null).asObject

    (objectOrNone(parsedPrice), objectOrNone(parsedImpression)) match {
      case (Some(prices), Some(impressions)) => fromFields(merge(prices.toMap, impressions.toMap))
      case _ => fromString("Invalid data")
    }
  }

  def merge(prices: Map[String, Json], impressions: Map[String, Json]): Iterable[(String, Json)] =
    prices.map({ case (id, price) =>
      impressions.get(id).map({ impr =>
        (id, obj(
          ("impressions", impr),
          ("price", price),
          ("spent", fromDoubleOrNull(spent(price, impr)))
        ))
      })
    }).filter(_.isDefined).map(_.get)

  def spent(price: Json, impression: Json): Double = {
    val extractedPrice = price.asNumber.map(_.toDouble).getOrElse(0.0)
    val extractedImpression = impression.asNumber.map(_.toDouble).getOrElse(0.0)
    extractedPrice * extractedImpression
  }
}
