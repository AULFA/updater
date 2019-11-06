package one.lfa.updater.repository.xml.api

import one.lfa.updater.repository.api.Repository
import java.io.Closeable

interface RepositoryXMLSerializerType : Closeable {

  fun serialize(repository: Repository)

}