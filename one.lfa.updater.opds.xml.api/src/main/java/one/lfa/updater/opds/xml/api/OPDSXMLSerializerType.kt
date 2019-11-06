package one.lfa.updater.opds.xml.api

import one.lfa.updater.opds.api.OPDSManifest
import java.io.Closeable

interface OPDSXMLSerializerType : Closeable {

  fun serialize(manifest: OPDSManifest)

}