package au.org.libraryforall.updater.repository.xml.spi

import java.net.URI

data class SPISchemaResolutionMappings(
  val mappings: Map<URI, SPISchemaDefinition>)
