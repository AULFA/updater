package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParserProviderType
import au.org.libraryforall.updater.repository.xml.api.RepositoryXMLParsers
import au.org.libraryforall.updater.tests.RepositoryXMLParserContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RepositoryXMLParserTest : RepositoryXMLParserContract() {

  override fun logger(): Logger =
    LoggerFactory.getLogger(RepositoryXMLParserTest::class.java)

  override fun repositoryXMLParsers(): RepositoryXMLParserProviderType =
    RepositoryXMLParsers.createFromServiceLoader()

}
