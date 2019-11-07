package au.org.libraryforall.updater.tests.local

import au.org.libraryforall.updater.tests.InventoryOPDSPlanningContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InventoryOPDSPlanningTest : InventoryOPDSPlanningContract() {

  override val logger: Logger
    get() = LoggerFactory.getLogger(InventoryOPDSPlanningTest::class.java)

}
