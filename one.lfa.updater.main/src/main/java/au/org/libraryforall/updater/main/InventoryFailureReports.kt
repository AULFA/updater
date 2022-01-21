package au.org.libraryforall.updater.main

import android.content.Context
import one.lfa.updater.inventory.api.InventoryFailureReport
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormatterBuilder
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.Properties
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object InventoryFailureReports {

  val timestampFormat =
    DateTimeFormatterBuilder()
      .appendYear(4, 8)
      .appendMonthOfYear(2)
      .appendDayOfMonth(2)
      .appendLiteral("T")
      .appendHourOfDay(2)
      .appendMinuteOfHour(2)
      .appendSecondOfMinute(2)
      .appendLiteral("_")
      .appendMillisOfSecond(6)
      .toFormatter()

  @Throws(IOException::class)
  fun writeToStorage(
    context: Context,
    time: Instant,
    report: InventoryFailureReport): File {
    val outputDirectory = context.getExternalFilesDir("Reports")
    if (outputDirectory != null) {
      outputDirectory.mkdirs()
      val outputFile = File(outputDirectory, time.toString(timestampFormat) + ".xml")
      FileOutputStream(outputFile).use { stream ->
        writeToStream(stream, time, report)
      }
      return outputFile
    }

    throw IOException("Output directory is not available on external storage!")
  }

  fun writeToStream(
    outputStream: OutputStream,
    time: Instant,
    report: InventoryFailureReport
  ) {
    val document = write(time, report)
    val factory = TransformerFactory.newInstance()
    val transformer = factory.newTransformer()
    val outFormat = Properties()
    outFormat.setProperty(OutputKeys.INDENT, "yes")
    outFormat.setProperty(OutputKeys.METHOD, "xml")
    outFormat.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
    outFormat.setProperty(OutputKeys.VERSION, "1.0")
    outFormat.setProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.outputProperties = outFormat

    val domSource = DOMSource(document)
    val result = StreamResult(outputStream)
    transformer.transform(domSource, result)
    outputStream.flush()
  }

  fun write(
    time: Instant,
    report: InventoryFailureReport
  ): Document {
    val documentBuilderFactory = DocumentBuilderFactoryImpl()
    val documentBuilder = documentBuilderFactory.newDocumentBuilder()
    val document = documentBuilder.newDocument()

    val root = document.createElement("failure-report")
    document.appendChild(root)
    val header = document.createElement("attributes")
    root.appendChild(header)

    val elementTitle = document.createElement("attribute")
    elementTitle.setAttribute("name", "Title")
    elementTitle.setAttribute("value", report.title)
    header.appendChild(elementTitle)

    val elementTime = document.createElement("attribute")
    elementTime.setAttribute("name", "Time")
    elementTime.setAttribute("value", time.toString())
    header.appendChild(elementTime)

    val attributes = report.attributes.toMutableMap()
    attributes["Android.Board"] = android.os.Build.BOARD
    attributes["Android.Bootloader"] = android.os.Build.BOOTLOADER
    attributes["Android.Brand"] = android.os.Build.BRAND
    attributes["Android.Device"] = android.os.Build.DEVICE
    attributes["Android.Display"] = android.os.Build.DISPLAY
    attributes["Android.Fingerprint"] = android.os.Build.FINGERPRINT
    attributes["Android.Hardware"] = android.os.Build.HARDWARE
    attributes["Android.Host"] = android.os.Build.HOST
    attributes["Android.Id"] = android.os.Build.ID
    attributes["Android.Manufacturer"] = android.os.Build.MANUFACTURER
    attributes["Android.Model"] = android.os.Build.MODEL
    attributes["Android.SDK"] = android.os.Build.VERSION.SDK
    attributes["Android.Tags"] = android.os.Build.TAGS
    attributes["Android.Type"] = android.os.Build.TYPE
    attributes["Android.User"] = android.os.Build.USER

    for (key in attributes.keys) {
      val value = attributes[key]
      val elementAttr = document.createElement("attribute")
      elementAttr.setAttribute("name", key)
      elementAttr.setAttribute("value", value)
      header.appendChild(elementAttr)
    }

    val elementSteps = document.createElement("steps")
    root.appendChild(elementSteps)

    for (step in report.taskSteps) {
      val elementStep = document.createElement("step")
      elementSteps.appendChild(elementStep)

      val elementDesc = document.createElement("description")
      elementDesc.appendChild(document.createCDATASection(step.description))
      elementStep.appendChild(elementDesc)

      val elementRes = document.createElement("resolution")
      elementRes.appendChild(document.createCDATASection(step.resolution))
      elementStep.appendChild(elementRes)

      if (step.exception != null) {
        val elementExes = document.createElement("exceptions")
        elementStep.appendChild(elementExes)

        var exceptionCurrent: Throwable? = step.exception
        while (exceptionCurrent != null) {
          val elementEx = document.createElement("exception")
          elementExes.appendChild(elementEx)
          val byteArrayOutputStream = ByteArrayOutputStream()
          exceptionCurrent.printStackTrace(PrintStream(byteArrayOutputStream))
          elementEx.appendChild(document.createCDATASection(
            String(byteArrayOutputStream.toByteArray(), Charset.forName("UTF-8"))
          ))
          exceptionCurrent = exceptionCurrent.cause
        }
      }
    }

    return document
  }
}
