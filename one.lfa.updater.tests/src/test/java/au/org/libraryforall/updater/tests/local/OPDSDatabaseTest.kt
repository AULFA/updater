package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.tests.OPDSDatabaseContract
import one.lfa.updater.opds.database.api.OPDSDatabaseStringsType
import one.lfa.updater.opds.database.api.OPDSDatabaseType
import one.lfa.updater.opds.database.vanilla.OPDSDatabase
import one.lfa.updater.opds.xml.api.OPDSXMLParserProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLSerializerProviderType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class OPDSDatabaseTest : OPDSDatabaseContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(OPDSDatabaseTest::class.java)

  override fun open(
    strings: OPDSDatabaseStringsType,
    parsers: OPDSXMLParserProviderType,
    serializers: OPDSXMLSerializerProviderType,
    directory: File
  ): OPDSDatabaseType {
    return OPDSDatabase.open(
      strings = strings,
      parsers = parsers,
      serializers = serializers,
      directory = directory
    )
  }

}