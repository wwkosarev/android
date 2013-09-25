package com.ultivox.uvoxplayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import android.content.Context;

public class LogCatReader  {
    
	static final DateFormat LOG_DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    
    static final Pattern LOG_ENTRY_PATTERN = Pattern.compile(
            "(?:^--.*$(?:\\n|\\r\\n))?" 							// берем строку
            + "^\\[ " 												// начальные квадратные скобки
            + "(\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d)"	// дата
            + " +"													// все пробелы
            + "(\\d+):" 											// PID
            + "0x([\\da-fA-F]+) "									// TID 
            + "([UDVIWEFS])/" 										// уровень
            + "(.*) ]$(?:\\n|\\r\\n)" 								// конечные квадратные скобки
            + "(^(?:.|(?:\\n|\\r\\n))*?)(?:^$(?:\\n|\\r\\n))+" 		// само сообщение
            , 
            Pattern.MULTILINE);
    
    static final int 
            GROUP_DATE = 1,
            GROUP_PID = 2,
            GROUP_TID = 3,
            GROUP_PRIORITY = 4,
            GROUP_TAG = 5,
            GROUP_MESSAGE = 6;
    

    private Scanner logScanner;
    private Context motherContext;

    public LogCatReader(Context con, File logFile) throws FileNotFoundException {
        logScanner = new Scanner(logFile);
        motherContext = con;
    }
    
    public LogEntry nextEntry()  {
        
        // Сканируем до следующей записи
    	try {
        if(logScanner.findWithinHorizon(LOG_ENTRY_PATTERN, 0) == null)
            return null;
    	} catch (OutOfMemoryError  e) {
    		e.printStackTrace();
    	}
        MatchResult match = logScanner.match();
        Calendar date = Calendar.getInstance();
        Calendar rightNow = Calendar.getInstance();
        // Парсим запись
        try 
        	{ date.setTime(LOG_DATE_FORMAT.parse(match.group(GROUP_DATE))); }
        catch (ParseException e) 
        	{ throw new AssertionError("Can't happen."); }
        if (date.get(Calendar.MONTH)<=rightNow.get(Calendar.MONTH)) {
        	date.set(Calendar.YEAR, rightNow.get(Calendar.YEAR));
        } else {
        	date.set(Calendar.YEAR, rightNow.get(Calendar.YEAR)-1);
        }
        int pid = Integer.parseInt(match.group(GROUP_PID));
        int tid = Integer.parseInt(match.group(GROUP_TID), 16);
        Character level = match.group(GROUP_PRIORITY).charAt(0);
        String tag = match.group(GROUP_TAG);
        String message = match.group(GROUP_MESSAGE);
        PIDHashMap currPHM = new PIDHashMap(motherContext);
        String app = currPHM.getAppName(pid);
        
        // Обрезаем все лишние концы строк до конца записи
        if(message.endsWith("\r\n"))
            message = message.substring(0, message.length() - 2);
        else if(message.endsWith("\n"))
            message = message.substring(0, message.length() - 1);
        
        return new LogEntry(pid, tid, date.getTimeInMillis(), level, app, tag, message);

    }
}
