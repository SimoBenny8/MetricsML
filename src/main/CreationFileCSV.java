package main;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONException;

public class CreationFileCSV {
	
	
	
	private static final Logger logger = Logger.getLogger(CreationFileCSV.class.getName());
	private static String projB = "Bookkeeper";
	private static String projS = "Storm";
	private static final String ERRORSTR = "Exception found";
	private static final String CMD = "cmd /c cd ";
	
	public static void createCsv(String nameProj){
		
		
		PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(nameProj + "Metrics.csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(pw != null) {
        	StringBuilder builder = new StringBuilder();
        	String columnNamesList = "Name Project,Version,ClassName,Bugginess";
        	// No need give the headers Like: id, Name on builder.append
        	builder.append(columnNamesList +"\n");
        	
        	
        	builder.append(nameProj+",");
        	builder.append("Chola");
        	builder.append('\n');
        	pw.write(builder.toString());
        	pw.close();
        	System.out.println("done!");
        }
		
	}
	
	
	public static List<String> getInfoVersions(String nameProj,Integer position) throws JSONException, IOException{
		//position 2 = version name
		//position 3 = date version
		String s;
		List<String> info = new ArrayList<>();
		
		if(GetReleaseInfo.getReleaseinCSV(nameProj)) {
			
			try(BufferedReader csvReader = new BufferedReader(new FileReader(nameProj+"VersionInfo.csv"))){
			
				while ((s = csvReader.readLine()) != null) {
					String[] data = s.split(",");
					info.add(data[position]);
				}
				
			}catch(IOException e) {
				e.printStackTrace();
			}
			
			
			Integer halfInfo = (info.size())/2;
			
			while(halfInfo.equals(info.size())) {
				
				info.remove(info.size() - 1);
			}
		}
		return info;
	}
	
	public static Boolean versionClass(String pathName, String pathClass,String namePrj,String datePrev,String dateNext) throws IOException, InterruptedException {
		//Pathclass contain only java file (ex: Name.java)
		//TODO: split string path classes before call this method
		Process p;
		String s;
		FileWriter result;
		String s2;
		Process p2 = null;
		
		
		if(dateNext.equals(""))
			p = Runtime.getRuntime().exec(CMD +pathName + namePrj+"&& git log --before="+datePrev +" --date-order --abbrev-commit --pretty=format:"+"%h");
		else
			p = Runtime.getRuntime().exec(CMD +pathName + namePrj+"&& git log --before="+dateNext + "--after="+ datePrev +"--date-order --abbrev-commit --pretty=format:"+"%h");	
         
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		
		result = new FileWriter("commitId"+namePrj+".txt");
		 try {
	        	
         	while ((s = br.readLine()) != null) {
         		result.write(s);
    			result.append("\n");
         	}
         	p.waitFor();
            p.destroy();
          
     	 
     	 }catch (InterruptedException e) {
         	logger.log(Level.WARNING,ERRORSTR);
         	Thread.currentThread().interrupt();
     	 }finally {
    		 result.close();
    	 }
         
		 try(BufferedReader csvReader = new BufferedReader(new FileReader("commitId"+namePrj+".txt"))){
				
				while ((s = csvReader.readLine()) != null) {
					p2 = Runtime.getRuntime().exec(CMD +PATHNAME + namePrj+"&& git diff-tree --no-commit-id --name-only -r "+ s);	
			         BufferedReader br2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
			         System.out.println("Inizio p2");
			         while ((s2 = br2.readLine()) != null) {
			         			System.out.println("Sto dentro p2");
								if(s2.endsWith(pathClass))
									return true;
					}
				}
				if(p2 != null) {
					p2.waitFor();
					p2.destroy();
				}
				
			}catch(IOException e) {
				e.printStackTrace();
			}finally {
				Path path = Paths.get("commitId"+namePrj+".txt");
				Files.delete(path);
			}
             	
           
		return false;
	}
	

	
	
	public static void pathClasses(String namePrj) {
		
		//return a file with a path of java classes of a Apache project
		String pathName = "C:/Users/Simone Benedetti/Documents/Programmazione JAVA/";
		
		List<String> result = null;
		Path dir = Paths.get(pathName +namePrj);
		
		try (Stream<Path> walk = Files.walk(dir,Integer.MAX_VALUE,FileVisitOption.FOLLOW_LINKS)) {
            result = walk.filter(s -> s.toString().endsWith(".java")).map(String::valueOf).collect(Collectors.toList());
                
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		try (FileWriter writer = new FileWriter(namePrj+"Output.txt")){
			String s;
			for(String str: result) {
					s = str.substring(PATHNAME.length()+namePrj.length());
					writer.write(s + System.lineSeparator());
			} 
		  
		}catch (IOException e) {
			e.printStackTrace();
		}
	
		
	}
		
		
	public static void main(String[] args) throws InterruptedException{
		
		
		
		Boolean b = null;
		try {
			b = versionClass("RollingTopWords.java", projS, "2011-10-01","");
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
		System.out.println("Risultato: "+ b.toString());
	}
	
}


