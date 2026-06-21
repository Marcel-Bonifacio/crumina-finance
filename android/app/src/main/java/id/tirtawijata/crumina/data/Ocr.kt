package id.tirtawijata.crumina.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** On-device receipt OCR: recognize text in a picked image and guess the total amount. */
object Ocr {
    private val numberRe = Regex("\\d[\\d.,]{1,}")

    fun scan(context: Context, uri: Uri, onResult: (Double?) -> Unit) {
        val image = try { InputImage.fromFilePath(context, uri) } catch (e: Exception) { onResult(null); return }
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener { vt -> onResult(bestAmount(vt.text)) }
            .addOnFailureListener { onResult(null) }
    }

    // Receipt totals are usually the largest currency-like number.
    private fun bestAmount(text: String): Double? =
        numberRe.findAll(text).mapNotNull { parse(it.value) }.filter { it >= 100 }.maxOrNull()

    private fun parse(token: String): Double? {
        val t = token.trim().trim('.', ',')
        return when {
            Regex("^\\d{1,3}(\\.\\d{3})+$").matches(t) -> t.replace(".", "").toDoubleOrNull()
            Regex("^\\d{1,3}(,\\d{3})+(\\.\\d{1,2})?$").matches(t) -> t.replace(",", "").toDoubleOrNull()
            Regex("^\\d+,\\d{1,2}$").matches(t) -> t.replace(",", ".").toDoubleOrNull()
            else -> t.replace(",", "").replace(" ", "").toDoubleOrNull()
        }
    }
}
