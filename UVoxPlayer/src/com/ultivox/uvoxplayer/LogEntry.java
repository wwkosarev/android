package com.ultivox.uvoxplayer;

import java.text.DateFormat;
import java.util.Calendar;

public final class LogEntry {

    @SuppressWarnings("unused")
	private static final DateFormat DATE_FORMATTER = 
        DateFormat.getDateTimeInstance();
    
    public final long date;   //в миллисекундах
    public final long pId;
    public final long tId;
    public final Character logLevel;
    public final String appName;
    public final String tag;
    public final String message;
    

    public LogEntry(long pid, long tid, long date, Character level, 
    		String appname, String tag, String message) {
        if(level == null)
            throw new NullPointerException("level was null");
        if(tag == null)
            throw new NullPointerException("tag was null");
        if(message == null)
            throw new NullPointerException("message was null");
        
        this.pId = pid;
        this.tId = tid;
        this.date = date;
        this.logLevel = level;
        this.appName = appname;
        this.tag = tag;
        this.message = message;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (date ^ (date >>> 32));
        result = prime * result
                + ((logLevel == null) ? 0 : logLevel.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + (int) (pId ^ (pId >>> 32));
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        result = prime * result + ((tag == null) ? 0 : appName.hashCode());
        result = prime * result + (int) (tId ^ (tId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LogEntry other = (LogEntry) obj;
        if (date != other.date)
            return false;
        if (logLevel == null) {
            if (other.logLevel != null)
                return false;
        }
        else if (!logLevel.equals(other.logLevel))
            return false;
        if (message == null) {
            if (other.message != null)
                return false;
        }
        else if (!message.equals(other.message))
            return false;
        if (pId != other.pId)
            return false;
        if (tag == null) {
            if (other.tag != null)
                return false;
        }
        else if (!tag.equals(other.tag))
            return false;
        if (appName == null) {
            if (other.appName != null)
                return false;
        }
        else if (!appName.equals(other.appName))
            return false;
        if (tId != other.tId)
            return false;
        return true;
    }

    @Override
    public String toString() {
    	
    	Calendar calTime = Calendar.getInstance();
    	calTime.setTimeInMillis(date);
        return String.format("LogEntry [date=%s, logLevel=%s, " +
                "processId=%s, threadId=%s, application=%s, tag=%s, message=%s]",
                calTime.toString(), logLevel, pId, tId, appName,
                tag, message);
    }
}
