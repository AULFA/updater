package one.lfa.updater.xml.spi

import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.ext.EntityResolver2
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException

/**
 * A hardened entity resolver that can resolve resources from a set of
 * given schemas or from any descendant of a given directory. The resolver
 * prevents path traversal attacks by refusing to resolve resources outside
 * of the given directory.
 */

class SPIHardenedDispatchingResolver private constructor(
  private val baseDirectory: File?,
  private val schemas: SPISchemaResolutionMappings
) : EntityResolver2 {

  private val logger = LoggerFactory.getLogger(SPIHardenedDispatchingResolver::class.java)

  @Throws(SAXException::class)
  override fun getExternalSubset(
    name: String?,
    baseURI: String?): InputSource? {

    this.logger.debug("getExternalSubset: {} {}", name, baseURI)
    return null
  }

  @Throws(SAXException::class, IOException::class)
  override fun resolveEntity(
    name: String?,
    publicID: String?,
    baseURI: String?,
    systemID: String?): InputSource {

    this.logger.debug("resolveEntity: {} {} {} {}", name, publicID, baseURI, systemID)

    val schema =
      this.schemas.mappings.values.find { def -> def.fileIdentifier == systemID }

    if (schema != null) {
      val location = schema.location
      this.logger.debug("resolving {} from internal resources -> {}", systemID, location)
      return createSource(location.openStream(), location.toString())
    }

    val lineSeparator = System.lineSeparator()
    try {
      val uri = URI(systemID)
      val scheme = uri.scheme
      val base = this.baseDirectory

      if (!isResolvable(scheme)) {
        throw SAXException(
          StringBuilder(128)
            .append("Refusing to resolve a non-file URI.")
            .append(lineSeparator)
            .append("  Base: ")
            .append(base)
            .append(lineSeparator)
            .append("  URI: ")
            .append(uri)
            .append(lineSeparator)
            .toString())
      }

      if (base == null) {
        throw SAXException(
          StringBuilder(128)
            .append("Refusing to allow access to the filesystem.")
            .append(lineSeparator)
            .append("  Input URI: ")
            .append(uri)
            .append(lineSeparator)
            .toString())
      }

      this.logger.debug("resolving {} from filesystem", systemID)

      val resolved = File(base, systemID).absoluteFile
      if (!resolved.startsWith(base)) {
        throw SAXException(
          StringBuilder(128)
            .append("Refusing to allow access to files above the base directory.")
            .append(lineSeparator)
            .append("  Base: ")
            .append(base)
            .append(lineSeparator)
            .append("  Path: ")
            .append(resolved)
            .append(lineSeparator)
            .toString())
      }

      if (!resolved.isFile) {
        throw FileNotFoundException(resolved.toString())
      }

      return createSource(FileInputStream(resolved), resolved.toString())
    } catch (e: URISyntaxException) {
      throw SAXException(
        StringBuilder(128)
          .append("Refusing to resolve an unparseable URI.")
          .append(lineSeparator)
          .append("  Base: ")
          .append(this.baseDirectory)
          .append(lineSeparator)
          .append("  URI: ")
          .append(systemID)
          .append(lineSeparator)
          .toString(),
        e)
    }

  }

  override fun resolveEntity(
    publicID: String?,
    systemID: String?): InputSource =
    throw UnsupportedOperationException("Simple entity resolution not supported")

  companion object {

    /**
     * Create a new resolver. The resolver will resolve schemas from the given
     * schema mappings, and will optionall resolve other file resources from the
     * given base directory. If no base directory is provided, no resolution of
     * resources from the filesystem will occur.
     *
     * @param baseDirectory The base directory used to resolve resources, if any
     * @param schemas        A set of schema mappings
     *
     * @return A new resolver
     */

    fun create(
      baseDirectory: File?,
      schemas: SPISchemaResolutionMappings): SPIHardenedDispatchingResolver {
      return SPIHardenedDispatchingResolver(baseDirectory, schemas)
    }

    /*
     * It's necessary to explicitly set a system ID for the input
     * source, or Xerces XIncludeHandler.searchForRecursiveIncludes()
     * method will raise a null pointer exception when it tries to
     * call equals() on a null system ID.
     */

    private fun createSource(
      stream: InputStream,
      system_id: String): InputSource {
      val source = InputSource(stream)
      source.systemId = system_id
      return source
    }

    private fun isResolvable(scheme: String?): Boolean {
      return "file" == scheme || scheme == null
    }
  }
}
