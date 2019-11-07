package au.org.libraryforall.updater.tests

import one.lfa.updater.inventory.vanilla.InventoryOPDSPlanning
import one.lfa.updater.opds.api.OPDSFile
import one.lfa.updater.opds.api.OPDSManifest
import org.joda.time.LocalDateTime
import org.junit.Assert
import org.junit.Test
import org.slf4j.Logger
import java.io.File
import java.net.URI
import java.util.UUID

abstract class InventoryOPDSPlanningContract  {

  abstract val logger : Logger

  @Test
  fun testMinimal()
  {
    val opdsManifest =
      OPDSManifest(
      baseURI = null,
      rootFile = URI.create("file/x"),
      updated = LocalDateTime.now(),
      searchIndex = null,
      id = UUID.randomUUID(),
      files = listOf<OPDSFile>(
        OPDSFile(
          file = URI.create("feeds/FF278B9EE2DDA78D7652F4D458101100F7987048C5421A3D1777400E718F57DA"),
          hash = "FF278B9EE2DDA78D7652F4D458101100F7987048C5421A3D1777400E718F57DA",
          hashAlgorithm = "SHA-256"
        )
      )
    )

    val plan =
      InventoryOPDSPlanning.planUpdate(
      manifestURI = URI.create("https://example.com/manifest.xml"),
      manifest = opdsManifest,
      catalogDirectory = File("/tmp/catalog")
    )

    plan.forEachIndexed { index, op ->
      this.logger.debug("op [{}]: {}", index, op)
    }

    Assert.assertNotEquals(0, plan.size)
  }
}