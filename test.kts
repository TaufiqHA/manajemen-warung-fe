import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

try {
    val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
    val date = formatter.parse("20260531-1109")
    println("Parsed date: $date")
} catch (e: Exception) {
    println("Exception: ${e.message}")
}
