package cop5556fa20.runtime;

import java.util.ArrayList;

public class LoggedIO {
	
	public static final ArrayList<Object> globalLog = new ArrayList<Object>();
	
	public final static String className = "cop5556fa20/runtime/LoggedIO";	
	
	public final static String stringToScreenSig = "(Ljava/lang/String;)V";
	public static void stringToScreen(String s) {
		globalLog.add(s);
		System.out.println(s);
	}
	
	public final static String intToScreenSig = "(I)V";
	public static void intToScreen(int n) {
		globalLog.add(Integer.valueOf(n));
		System.out.println(n);
	}
	
	public static void clearGlobalLog() {
		globalLog.clear();
	}


}
