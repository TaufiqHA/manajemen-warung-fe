import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;

public class TestDate {
    public static String getDateString(String rawTime) {
        if (rawTime == null || rawTime.trim().isEmpty()) return "Tanggal Tidak Diketahui";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            String safeRawTime = rawTime.length() >= 19 ? rawTime.substring(0, 19) : rawTime;
            Date date = parser.parse(safeRawTime);
            return formatter.format(date);
        } catch (Exception e) {
            try {
                SimpleDateFormat fallbackParser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat fallbackFormatter = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
                Date date = fallbackParser.parse(rawTime.substring(0, 10));
                return fallbackFormatter.format(date);
            } catch (Exception e2) {
                return rawTime;
            }
        }
    }

    public static void main(String[] args) {
        String rawTime1 = "2026-06-12T19:12:54Z";
        String parsed1 = getDateString(rawTime1);
        
        String rawTime2 = "2026-06-13T01:12:54Z";
        String parsed2 = getDateString(rawTime2);
        
        String rawTime3 = "2026-06-13T09:05:47Z";
        String parsed3 = getDateString(rawTime3);

        String todayLocalStr = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(new Date());
        String todayIsoStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        System.out.println("rawTime1 (2026-06-12T19:12:54Z) getDateString: '" + parsed1 + "'");
        System.out.println("rawTime2 (2026-06-13T01:12:54Z) getDateString: '" + parsed2 + "'");
        System.out.println("rawTime3 (2026-06-13T09:05:47Z) getDateString: '" + parsed3 + "'");
        
        System.out.println("todayLocalStr: '" + todayLocalStr + "'");
        System.out.println("todayIsoStr: '" + todayIsoStr + "'");

        System.out.println("startsWith 1? " + rawTime1.startsWith(todayIsoStr));
        System.out.println("startsWith 2? " + rawTime2.startsWith(todayIsoStr));
        System.out.println("startsWith 3? " + rawTime3.startsWith(todayIsoStr));
    }
}
