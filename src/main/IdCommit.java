package main;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;


public class IdCommit {
	
	private static final Logger logger = Logger.getLogger(IdCommit.class.getName());
	private static final String ERRORSTR = "Exception found";
	private static final String COMMIT = "commit";
	
	private IdCommit() {
	    throw new IllegalStateException("Utility class");
	}
	
	public static void commitString(String wordToSearch, String project,String fileName, int i, String character) throws IOException{
		
		//character can be :, ,]
		String s;
        Process p;
        String outname = fileName + "TotalCommit.txt";
        FileWriter result;
        	
         p = Runtime.getRuntime().exec("cmd /c cd "+project+"&& git log --grep="+wordToSearch + character +" --date=iso-strict --name-status --stat HEAD --abbrev-commit");
         BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
         if(i != 0) {
        	 result = new FileWriter(outname,true);
         }else {
        	 result = new FileWriter(outname);
         }
         try {
        	
            	while ((s = br.readLine()) != null) {
            		if(s.startsWith(COMMIT)) {
            			s = s.substring(COMMIT.length());
            			result.write(wordToSearch);
            			result.write(s);
            			result.append("\n");
            		}
                }	
                p.waitFor();
                p.destroy();
             
        	 
        	 }catch (InterruptedException e) {
            	logger.log(Level.WARNING,ERRORSTR);
            	Thread.currentThread().interrupt();
        	 }finally {
        		 result.close();
        	 }
         
         }
         
        
    
		
	public static void main (String[] args) {
   	 try {
		commitString("BOOKKEEPER-1","C:\\Users\\Simone Benedetti\\Documents\\Programmazione JAVA\\Bookkeeper","Bookkeeper",1,":");
	} catch (IOException e) {
		
		e.printStackTrace();
	}
    }
       

	

}


