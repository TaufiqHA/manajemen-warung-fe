import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

fun getDateString(rawTime: String): String {
    if (rawTime.isBlank()) return "Tanggal Tidak Diketahui"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val safeRawTime = if (rawTime.length >= 19) rawTime.substring(0, 19) else rawTime
        val date = parser.parse(safeRawTime) 
        
        if (date != null) formatter.format(date) else rawTime
    } catch (e: Exception) {
        try {
            val fallbackParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fallbackFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            val date = fallbackParser.parse(rawTime.substring(0, 10))
            if (date != null) fallbackFormatter.format(date) else rawTime
        } catch (e2: Exception) {
            rawTime
        }
    }
}

fun main() {
    val rawTime = "2026-06-12T19:12:54Z" // 02:12 am next day in UTC+7, 03:12 am next day in UTC+8
    val parsed = getDateString(rawTime)
    val todayLocalStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(1718244794000L)) // mock today June 13
    
    println("getDateString: '$parsed'")
    println("todayLocalStr: '$todayLocalStr'")
    println("Equals? ${parsed == todayLocalStr}")
}
