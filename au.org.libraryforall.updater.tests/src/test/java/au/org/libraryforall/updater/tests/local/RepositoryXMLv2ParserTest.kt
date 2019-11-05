package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParserProviderType
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParsers
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLSerializerProviderType
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLSerializers
import au.org.libraryforall.updater.tests.RepositoryXMLv2ParserContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RepositoryXMLv2ParserTest : RepositoryXMLv2ParserContract() {

  override fun repositoryXMLSerializers(): RepositoryXMLSerializerProviderType =
    RepositoryXMLSerializers.createFromServiceLoader()

  override fun logger(): Logger =
    LoggerFactory.getLogger(RepositoryXMLv2ParserTest::class.java)

  override fun repositoryXMLParsers(): RepositoryXMLParserProviderType =
    RepositoryXMLParsers.createFromServiceLoader()

}
