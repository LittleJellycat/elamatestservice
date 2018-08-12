import com.typesafe.config.{Config, ConfigFactory}

object AppConfig {
  private val config: Config = ConfigFactory.load()
  val host: String = config.getString("application.host")
  val port: Int = config.getString("application.port").toInt
  val outerServiceHost: String = config.getString("container.host")
  val outerServicePort: Int = config.getString("container.port").toInt
}
