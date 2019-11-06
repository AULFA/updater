package one.lfa.updater.xml.spi

import java.net.URI

data class SPISchemaResolutionMappings(
  val mappings: Map<URI, SPISchemaDefinition>)
