package au.org.libraryforall.updater.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import au.org.libraryforall.updater.app.boot.BootViewController
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import one.lfa.updater.apkinstaller.api.APKInstallerType
import one.lfa.updater.services.api.Services
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

  private val logger = LoggerFactory.getLogger(MainActivity::class.java)

  private lateinit var router: Router

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    this.logger.debug("MainActivity: requestCode: {}", requestCode)
    this.logger.debug("MainActivity: resultCode:  {}", resultCode)
    this.logger.debug("MainActivity: data:        {}", data)

    Services.serviceDirectory()
      .requireService(APKInstallerType::class.java)
      .reportStatus(requestCode, resultCode)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.main_activity)

    val container =
      this.findViewById<View>(R.id.main_container) as ViewGroup

    this.router = Conductor.attachRouter(this, container, savedInstanceState)
    if (!this.router.hasRootController()) {
      this.router.setRoot(RouterTransaction.with(BootViewController()))
    }
  }

  override fun onBackPressed() {
    if (!this.router.handleBack()) {
      super.onBackPressed()
    }
  }

  companion object {
    const val TARGET_PARAMETER_ID =
      "au.org.libraryforall.updater.app.MainActivity.target"
  }
}
