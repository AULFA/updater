package au.org.libraryforall.updater.main

import android.os.Handler
import android.os.Looper

object UIThread {

  fun execute(runnable: () -> Unit) {
    Handler(Looper.getMainLooper()).post(runnable)
  }

  fun executeLater(runnable: () -> Unit, milliseconds: Long) {
    Handler(Looper.getMainLooper()).postDelayed(runnable, milliseconds)
  }

}