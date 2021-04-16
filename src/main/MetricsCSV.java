package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.json.JSONException;


public class MetricsCSV {
	
	private static ArrayList<String> id;
	
	public static boolean getBugginess(String nameProj, String pathProject, Integer j, String pathClass) {
		//nameProj written in MAIUSC
		try {
			id = (ArrayList<String>) RetrieveTicketsID.getIdCommit(nameProj,j);
		} catch (JSONException|IOException e) {
			
			e.printStackTrace();
		} 
		
		for(int i=0; i<id.size(); i++) {
			try {
				IdCommit.commitString(id.get(i), pathProject, nameProj , i,":");
				IdCommit.commitString(id.get(i), pathProject, nameProj , i," ");
				IdCommit.commitString(id.get(i), pathProject, nameProj , i,"]");
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			
		}
		
		File file = new File("Student.txt");

		try (Scanner scanner = new Scanner(file)){

		    //now read the file line by line...
		    while (scanner.hasNextLine()) {
		        String line = scanner.nextLine();
		        if(line.contains(pathClass)) { 
		            return true;
		        }
		    }
		} catch(FileNotFoundException e) { 
		    
		}
		
		
		return false;
	}

	public static void main(String[] args) {
	

	}

}
