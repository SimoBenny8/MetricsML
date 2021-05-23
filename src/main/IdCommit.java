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
	private static Integer count = 0;
	
	private IdCommit() {
	    throw new IllegalStateException("Utility class");
	}
	
	public static void commitString(String wordToSearch, String project,String fileName, String character) throws IOException{
		
		//character can be :, ,]
		String s;
        Process p;
        String outname = fileName + "TotalCommit.txt";
        FileWriter result;
        
        	
         p = Runtime.getRuntime().exec("cmd /c cd "+project+"&& git log --grep="+wordToSearch + character +" --date=iso-strict --name-status --abbrev-commit --reverse");
         BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
         result = new FileWriter(outname,true);
         
         try {
        	 
            	while ((s = br.readLine()) != null) {
            		if(s.startsWith(COMMIT)) {
            			count = count + 1;
            			s = s.substring(COMMIT.length());
            			result.write(wordToSearch);
            			result.write(s);
            		}else if(s.startsWith("Date:")) {
            			s = s.substring(7,18);
            			result.write(s);
            			result.write(" "+count.toString());
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
		
		Integer iv = (int) Math.round(4.5);
		logger.log(Level.INFO,"numero: {0}",iv);
    }
       

	

}


