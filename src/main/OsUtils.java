package main;

public class OsUtils{
	
	private OsUtils() {
	    throw new IllegalStateException("Utility class");
	  }
	
   private static String os = null;
   
   public static String getOsName()
   {
      if(os == null) {
    	  
    	  os = System.getProperty("os.name"); 
      }
      return os;
   }
   
   public static boolean isWindows(){
      
	   return getOsName().startsWith("Windows");
   }
   
   public static String getUserName() {
	   
	   return System.getProperty("user.name");
   }


}