package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.tests.OPDSManifestXMLv1SerializerContract
import one.lfa.updater.opds.xml.api.OPDSXMLSerializerProviderType
import one.lfa.updater.opds.xml.api.OPDSXMLSerializers
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OPDSManifestXMLv1SerializerTest : OPDSManifestXMLv1SerializerContract() {

  override fun xmlSerializers(): OPDSXMLSerializerProviderType =
    OPDSXMLSerializers.createFromServiceLoader()

  override fun logger(): Logger =
    LoggerFactory.getLogger(OPDSManifestXMLv1SerializerTest::class.java)

}
