package au.org.libraryforall.updater.tests.local

import one.lfa.updater.repository.xml.api.RepositoryXMLParserProviderType
import one.lfa.updater.repository.xml.api.RepositoryXMLParsers
import one.lfa.updater.repository.xml.api.RepositoryXMLSerializerProviderType
import one.lfa.updater.repository.xml.api.RepositoryXMLSerializers
import au.org.libraryforall.updater.tests.RepositoryXMLv1ParserContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RepositoryXMLv1ParserTest : RepositoryXMLv1ParserContract() {

  override fun repositoryXMLSerializers(): RepositoryXMLSerializerProviderType =
    RepositoryXMLSerializers.createFromServiceLoader()

  override fun logger(): Logger =
    LoggerFactory.getLogger(RepositoryXMLv1ParserTest::class.java)

  override fun repositoryXMLParsers(): RepositoryXMLParserProviderType =
    RepositoryXMLParsers.createFromServiceLoader()

}
