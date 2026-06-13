import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

val rawTime = "2026-06-13T01:12:54Z"
val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}
val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
val safeRawTime = if (rawTime.length >= 19) rawTime.substring(0, 19) else rawTime
val date = parser.parse(safeRawTime)
val res = if (date != null) formatter.format(date) else rawTime

val todayLocalStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())

println("getDateString: '$res'")
println("todayLocalStr: '$todayLocalStr'")
println("Equal? ${res == todayLocalStr}")
