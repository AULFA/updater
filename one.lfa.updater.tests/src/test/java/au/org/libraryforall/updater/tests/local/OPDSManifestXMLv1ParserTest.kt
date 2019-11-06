package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.tests.OPDSManifestXMLv1ParserContract
import one.lfa.updater.opds.xml.api.OPDSXMLParserProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLParsers
import one.lfa.updater.opds.xml.api.OPDSXMLSerializerProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLSerializers
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OPDSManifestXMLv1ParserTest : OPDSManifestXMLv1ParserContract() {

  override fun xmlSerializers(): OPDSXMLSerializerProviderType =
    OPDSXMLSerializers.createFromServiceLoader()

  override fun logger(): Logger =
    LoggerFactory.getLogger(OPDSManifestXMLv1ParserTest::class.java)

  override fun xmlParsers(): OPDSXMLParserProviderType =
    OPDSXMLParsers.createFromServiceLoader()

}
