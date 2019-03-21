package util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LoggerFormatter extends Formatter {

    public String format(LogRecord rec) {
        StringBuffer buffer = new StringBuffer(1000);
        buffer.append(rec.getSequenceNumber());
        buffer.append(" | ");
        buffer.append(rec.getLevel());
        buffer.append(" | ");
        buffer.append(calcDate(rec.getMillis()));
        buffer.append(" | ");
        buffer.append(rec.getMessage());
        buffer.append(" | ");
        buffer.append(rec.getSourceClassName());
        buffer.append(" | ");
        buffer.append(rec.getSourceMethodName());
        if (rec.getThrown() != null) {
            buffer.append(" | ");
            for (StackTraceElement st : rec.getThrown().getStackTrace()) {
                buffer.append("\n    at ");
                buffer.append(st.toString());
            }
        }
        buffer.append("\n");

        return buffer.toString();
    }

    public String calcDate(long millisecs) {
        SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss:SS");
        Date date = new Date(millisecs);
        return date_format.format(date);
    }

    public String getHead(Handler h) {
        return "\nStarting new session...\n";
    }

    public String getTail(Handler h) {
        return "\n";
    }
}