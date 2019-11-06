package one.lfa.updater.repository.xml.api

import au.org.libraryforall.updater.repository.api.Repository
import java.io.Closeable

interface RepositoryXMLSerializerType : Closeable {

  fun serialize(repository: Repository)

}