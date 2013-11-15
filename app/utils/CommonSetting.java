package utils;

import play.Play;
import play.Logger;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import java.util.Enumeration;

/**
 * 一般的な設定
 */
public class CommonSetting {
	
	private static final String CONSOLE_ENCODING    = "flect.console.encoding";
	
	public static void setup() {
		setupConsole();
		System.setProperty("mail.mime.encodefilename", Boolean.TRUE.toString());
		System.setProperty("mail.mime.decodefilename", Boolean.TRUE.toString());
	}
	
	/**
	 * コンソールの出力エンコーディング変更
	 */
	private static boolean consoleSetuped;
	private static void setupConsole() {
		String enc = Play.configuration.getProperty(CONSOLE_ENCODING);
		if (!consoleSetuped && enc != null && Play.mode.isDev()) {
			try {
				System.setOut(new PrintStream(System.out, true, enc));
				System.setErr(new PrintStream(System.err, true, enc));
			} catch (UnsupportedEncodingException e) {
				Logger.error(e.toString(), e);
			}
			org.apache.log4j.Logger root = LogManager.getRootLogger();
			Enumeration it = root.getAllAppenders();
			while (it.hasMoreElements()) {
				Object o = it.nextElement();
				if (o instanceof ConsoleAppender) {
					ConsoleAppender ca = ((ConsoleAppender)o);
					ca.setEncoding(enc);
					ca.activateOptions();
				}
			}
			consoleSetuped = true;
		}
	}
	
}