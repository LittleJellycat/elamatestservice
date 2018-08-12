import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Json
import io.circe.Json._
import io.circe.optics.JsonPath._
import io.circe.parser.parse

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}

object AppServer extends HttpApp {

  private val config: Config = ConfigFactory.load()
  private val host: String = config.getString("application.host")
  private val port: Int = config.getString("application.port").toInt
  private val outerServiceHost: String = config.getString("container.host")
  private val outerServicePort: Int = config.getString("container.port").toInt

  implicit val system: ActorSystem = ActorSystem("elama-test-service")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  override def routes: Route =
    path("stats") {
      get {
        parameters('id.as[Int].*) { ids =>
          ids.toList match {
            case Nil => complete("{}")
            case _ => complete(getStats(ids.toList))
          }
        }
      }
    }

  AppServer.startServer(host, port)


  def mergeToStats(parsedPrice: Json, parsedImpression: Json): Json = {
    val getResult = root.results.json
    val prices = getResult.getOption(parsedPrice).getOrElse(Json.Null)
    val impressions = getResult.getOption(parsedImpression).getOrElse(Json.Null)
    (prices.asObject, impressions.asObject) match {
      case (Some(prices), Some(impressions)) =>
        fromFields(

          prices.toList.zip(impressions.toList).map({ group =>
            val (price, impression) = group
            (
              price._1,
              obj(
                ("impressions", impression._2),
                ("price", price._2),
                ("spent", fromDoubleOrNull(spent(price._2, impression._2)))
              )
            )
          })
          //        prices.toMap.zip(impressions.toMap)
        )
      case _ => Json.Null
    }
  }

  def getStats(ids: List[Int]): String = {
    val idTail = ids.map("id=" + _).mkString("&")

    def requestTemplate(destination: String) = HttpRequest(uri = Uri.from(
      host = outerServiceHost,
      port = outerServicePort,
      path = destination,
      queryString = Some(idTail)))

    val futureResult = for {
      priceResponse <- Http().singleRequest(requestTemplate("prices"))
      impressionResponse <- Http().singleRequest(requestTemplate("impression"))
      priceContent <- Unmarshal(priceResponse.entity).to[String]
      impressionContent <- Unmarshal(impressionResponse.entity).to[String]
    } yield mergeToStats(parse(priceContent).getOrElse(Json.Null), parse(impressionContent).getOrElse(Json.Null))

    Await.result(futureResult.map(_.toString()), Duration(5, TimeUnit.SECONDS))
  }

  def spent(price: Json, impression: Json): Double = {
    val extractedPrice = price.asNumber.map(_.toDouble).getOrElse(0.0)
    val extractedImpression = impression.asNumber.map(_.toDouble).getOrElse(0.0)
    extractedPrice * extractedImpression
  }
}