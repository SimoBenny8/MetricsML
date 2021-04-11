package main;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetricsCSV {
	
	private static String pathBookkeeper = "C:/Users/Simone Benedetti/Documents/Programmazione JAVA/Bookkeeper";
	private static String pathStorm = "C:\\Users\\Simone Benedetti\\Documents\\Programmazione JAVA\\Storm";
	
	public static void createCsv(String nameProj) {
		PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(nameProj + "Metrics.csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(pw != null) {
        	StringBuilder builder = new StringBuilder();
        	String columnNamesList = "Version,ClassName,Bugginess";
        	// No need give the headers Like: id, Name on builder.append
        	builder.append(columnNamesList +"\n");
        	builder.append("1"+",");
        	builder.append("Chola");
        	builder.append('\n');
        	pw.write(builder.toString());
        	pw.close();
        	System.out.println("done!");
        }
		
	}
	
	
	public static void pathClasses(String path,String namePrj) {
		
		List<String> result = null;
		
		Path dir = Paths.get(path);
		
		try (Stream<Path> walk = Files.walk(dir,Integer.MAX_VALUE,FileVisitOption.FOLLOW_LINKS)) {
           
            result = walk.filter(s -> s.toString().endsWith(".java")).map(String::valueOf).collect(Collectors.toList());
            
            
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		
		try (FileWriter writer = new FileWriter(namePrj+"Output.txt")){
			String s;
			for(String str: result) {
					s = str.substring(57+namePrj.length());
				writer.write(s + System.lineSeparator());
			} 
		  
		}catch (IOException e) {
			e.printStackTrace();
		}
	
		
	}
		
		
		
	
	
	
	

	public static void main(String[] args) {
		
		//pathClasses(pathBookkeeper,"Bookkeeper");
		pathClasses(pathStorm,"Storm");
	}
	
}


