

class LoggerFormatter extends Formatter {
  
    String format(LogRecord rec) {
        StringBuffer buffer = new StringBuffer(1000);
        buffer.append(rec.getSequenceNumber());
        buffer.append(" | ");
        buffer.append(rec.getLevel());
        buffer.append(" | ");
        buffer.append(calcDate(rec.getMillis()));
        buffer.append(" | ");
        buffer.append(formatMessage(rec));
        buffer.append(" | ");
        buffer.append(rec.getSourceClassName());
        buffer.append(" | ");
        buffer.append(rec.getSourceMethodName());
        buffer.append("\n");

        return buffer.toString();
    }

    String calcDate(long millisecs) {
        SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss:SS");
        Date date = new Date(millisecs);
        return date_format.format(date);
    }
    
    String getHead(Handler h){
      return "\nStarting new session...\n";
    }
    
    String getTail(Handler h){
      return "\n";
    }
}
