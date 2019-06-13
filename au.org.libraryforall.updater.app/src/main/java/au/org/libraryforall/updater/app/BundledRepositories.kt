package au.org.libraryforall.updater.app

import android.content.Context
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser
import java.net.URI
import java.util.UUID

object BundledRepositories {

  /**
   * Parse bundled repositories from XML resources.
   */

  fun fromXMLResources(context: Context): List<BundledRepository> {
    return fromXMLParser(context.resources.getXml(R.xml.bundled_repositories))
  }

  /**
   * Parse bundled repositories from the given XML parser.
   */

  fun fromXMLParser(parser: XmlResourceParser): List<BundledRepository> {
    val repositories = mutableListOf<BundledRepository>()
    while (true) {
      when (parser.next()) {
        XmlPullParser.END_DOCUMENT ->
          return repositories.toList()

        XmlPullParser.START_TAG ->
          when (parser.name) {
            "bundled-repositories" -> Unit
            "bundled-repository" -> {
              val source =
                parser.getAttributeValue(null, "source")
              val uuid =
                parser.getAttributeValue(null, "uuid")
              val title =
                parser.getAttributeValue(null, "title")
              repositories.add(BundledRepository(
                uri = URI(source),
                requiredUUID = UUID.fromString(uuid),
                title = title
              ))
            }
          }

        else -> Unit
      }
    }
  }

}
