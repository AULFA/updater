package one.lfa.updater.repository.api

import com.google.common.base.Preconditions
import java.util.regex.Pattern

/**
 * A SHA-256 hash.
 */

data class Hash(val text: String) {

  init {
    Preconditions.checkArgument(
      pattern.matcher(text).matches(),
      "Input value ${text} must match pattern $patternText")
  }

  companion object {
    const val patternText = "[a-f0-9]{64}"
    val pattern = Pattern.compile(patternText)
  }
}
