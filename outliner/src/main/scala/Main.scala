import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

// Client-Server spec: "https://spec.matrix.org/v1.14/client-server-api/"
val clientServerFile = "/home/jim/Downloads/matrix-client-server-1.14.html"

// Server-Server spec: "https://spec.matrix.org/v1.14/server-server-api/"
val serverServerFile = "/home/jim/Downloads/matrix-server-server-1.14.html"

@main def main (args: String*): Unit =
  val browser = JsoupBrowser()
  val doc = browser.parseFile(serverServerFile)
  var sectionCount = 0
  var subsectionCount = 0

  for elem <- doc >> elementList("h2[id], h3[id], span.http-api-method") do
    elem.tagName match
      case "h2" =>
        sectionCount += 1
        subsectionCount = 0
        println(s"- [ ] $sectionCount ${elem.text}")
      case "h3" =>
        subsectionCount += 1
        println(s"    - [ ] $sectionCount.$subsectionCount ${elem.text}")
      case "span" =>
        val endpoint = elem.siblings.head
        val deprecated =
          if (endpoint.select(".deprecated-inline").nonEmpty) {
            " _DEPRECATED_"
          } else {
            ""
          }
        println(s"        - [ ] `${elem.text} ${endpoint.text}`$deprecated")
