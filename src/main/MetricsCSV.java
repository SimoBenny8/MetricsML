package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;

import metrics.MetricsNumber;


public class MetricsCSV {
	
	private ArrayList<String> id;
	private static final Logger logger = Logger.getLogger(MetricsCSV.class.getName());
	private static final String CMD = "cmd /c cd ";
	private static Double[] p = {0.0,1.0,0.667,0.667,0.667,0.778,1.25,1.271};
	
	
	public MetricsCSV(String nameProj, String pathProject, Integer j) {
				
				
		//get ticket ID from Jira
				try {
					id =(ArrayList<String>) RetrieveTicketsID.getTicketId(nameProj,j);
				} catch (JSONException|IOException e) {
					logger.log(Level.WARNING,"error getting ticket ID");
					e.printStackTrace();
				} 
				
				
				//Get all commits ID through git log and associated to ticket ID in Jira 
				for(int i=0; i< id.size(); i++) {
					try {
						
						IdCommit.commitString(id.get(i), pathProject, nameProj ,":");
						//IdCommit.commitString(id.get(i), pathProject, nameProj ," ",versions.get(versions.size()-1));
						IdCommit.commitString(id.get(i), pathProject, nameProj ,"]");
						
					} catch (IOException e) {
						logger.log(Level.WARNING,"error getting all commit");
						e.printStackTrace();
					}
					
				}
				
					
					File file = new File(nameProj + "TotalCommit.txt");
					//Get all ".java" from every commit ID linked to a ticket in Jira
					try (Scanner scanner = new Scanner(file)){

						//now read the file line by line...
						while (scanner.hasNextLine()) {
							String line = scanner.nextLine();
							//line = ticketID commitID commitDate count
						//	logger.log(Level.INFO,"line: {0}", line);
							String[] info = line.split(" ");
							String lowerCase = nameProj.toLowerCase();
							String fileNameProj = lowerCase.substring(0,1).toUpperCase() + lowerCase.substring(1);
							getClassFromCommitId(info,pathProject, fileNameProj);
						}
					} catch(IOException | JSONException|InterruptedException e) {
						logger.log(Level.WARNING,"error getting all commit from file .txt" );
						e.printStackTrace();
						Thread.currentThread().interrupt();

					} 
		
	}
	
	
	
	public static Boolean getBugginess(String nameProj,Integer numVersion,String classProj) throws JSONException, IOException, ParseException {
		//nameProj written in MAIUSC
			
						Integer iv = null;
						String info[] = classProj.split(" ");
						Integer fv;
						String avString = RetrieveTicketsID.getIV(info[0]);
						Integer av = stringToVersion(nameProj, avString);
						logger.log(Level.INFO,"rd not null and av: {0}", av);
						//applying proportion moving window Integer.parseInt(info[3])
						if(av == null) {
							//logger.log(Level.INFO,"av null: calling proportion");
							//Integer p = MetricsCSV.proportionIncremental(nameProj,numVersion);
							fv = dateToVersion(info[2],nameProj);
							logger.log(Level.INFO,"fv when av is null: {0}", fv);
							Integer ov = dateToVersion(RetrieveTicketsID.creationDateTicket(info[0]),nameProj);
							logger.log(Level.INFO,"ov when av is null: {0}", ov);
							
							
							if(!fv.equals(ov)) {
								iv = (int) Math.round((double)fv - (double)(fv-ov)*p[numVersion - 1]);
								logger.log(Level.INFO,"iv calcolata: {0}", iv);
								
							}else {
								return false;
							}
						
						}
						else {
							
							
								//info[2] = date commit fix
								fv = dateToVersion(info[2],nameProj);
								logger.log(Level.INFO,"fv when av is not null: {0}", fv);
								Integer ov = dateToVersion(RetrieveTicketsID.creationDateTicket(info[0]),nameProj);
								logger.log(Level.INFO,"ov when av is not null: {0}", ov);
								if(!fv.equals(ov)) {
									if(ov == 1) {
									//case: injected version equal to opening version
										iv = ov;
										
									
									}else {
										//case: injected version != opening version
										iv = av;
								
									}
									
							}else {
								return false;
							}
								
						}
						
						
		return ((numVersion < fv) && (numVersion >= iv));
	}
	
	public static Double proportionIncremental(String nameProj, Integer version) throws IOException, JSONException, ParseException {
		
		String outname = nameProj + "TotalCommit.txt";
        
		//Integer onePercent = ((int) Math.ceil((double) count/100.0));
		//Integer totalOnePercent = ((int) Math.ceil((double) count/100.0));
		//logger.log(Level.INFO,"valore onePercent = {0}", onePercent);
        Double p = (double) 0;
        Integer count = 0;
        String s;
       
        try(BufferedReader rd = new BufferedReader(new FileReader(outname))){
        	while ((s = rd.readLine()) != null) {
       			String[] info = s.split(" ");
       			Integer dateToVersionCommit = dateToVersion(info[2],nameProj);
       			if(dateToVersionCommit <= version) {
       				String ivString = RetrieveTicketsID.getIV(info[0]);
       				Integer iv = stringToVersion(nameProj,ivString);
       				logger.log(Level.INFO,"calcoli proportion: iv = {0}", iv);
       				Integer fv = dateToVersion(info[2],nameProj);
       				logger.log(Level.INFO,"calcoli proportion: fv = {0}", fv);
       				Integer ov = dateToVersion(RetrieveTicketsID.creationDateTicket(info[0]),nameProj);
       				logger.log(Level.INFO,"calcoli proportion: ov = {0}", ov);
       				if(iv != null && !fv.equals(ov) && !fv.equals(iv)) {
       					count++;
       					p = p + ((double)(fv-iv)/(double)(fv-ov));
       				}
       			}
       			
       		}
       	}
        
        
        
        if(count != 0) {
        	return  (p/((double) count));
        }else { 
        	return 0.0;
        }
		
	}
	
	public static Integer stringToVersion(String nameProj, String ver) throws JSONException, IOException {
		
		if(ver != null) {
			List<String> versions = CreationFileCSV.getInfoVersions(nameProj,2);
			for(Integer i = 0; i<versions.size(); i++) {
				if(versions.get(i).equals(ver)) {
					
					return i+1;
				}
			}
		}
		return null;
	}
	
	public static Integer dateToVersion(String date,String namePrj) throws ParseException, JSONException, IOException {
		//From a commidDate, return the version number associated
		
		SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd");
		Date d1 = sdformat.parse(date);
		List<String> dateVersions = CreationFileCSV.getInfoVersions(namePrj,3);
		for (int i = 0; i< dateVersions.size(); i++) {
			Date d2 = sdformat.parse(dateVersions.get(i));
			//if d1 < d2
			if(d1.compareTo(d2) < 0) {
				
				return i+1;
			}
		}
		
		return 0;
	}
	
	//info[] = ticketID commitID commitDate count
	public static void getClassFromCommitId(String[] info,String pathName,String namePrj) throws IOException, InterruptedException {
		
		Process p;
		
		String s;
		try(BufferedWriter wd = new BufferedWriter(new FileWriter("classTouchedByCommitBuggy"+namePrj.toLowerCase()+".txt", true))){	
			p = Runtime.getRuntime().exec(CMD +pathName+"&& git diff-tree --no-commit-id --name-only -r "+ info[1]);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ((s = br.readLine()) != null) {
					if(s.endsWith(".java")) {
						wd.append(info[0]+" "+info[1]+" "+info[2]+" "+info[3]+" ");
						wd.append(s + "\n");
					}
				}
				p.waitFor();
				p.destroy();
		}catch(IOException e) {
			logger.log(Level.WARNING,"error during getClassFromCommitId method");
			e.printStackTrace();
		}
		
	}
	
	public static void writeOnFile(BufferedWriter rw, String namePrj) {
		
		String s;
		
		try(BufferedReader rd = new BufferedReader(new FileReader("class"+namePrj.toLowerCase()+".txt"))){
			while ((s = rd.readLine()) != null) {
				String[] info = s.split(" ");
				String[] infoClass = info[1].split("/");
				System.out.println(infoClass[infoClass.length - 1]);
				MetricsNumber mn = new MetricsNumber();
				Integer[] metrics = mn.calculateMetrics(namePrj,Integer.parseInt(info[0]),info[1]);
				
				rw.append(info[0]+ ","+ info[1] + ","+metrics[0] +","+metrics[1]+","+metrics[2]+","+metrics[3]+",");
				rw.append(classAffectedByBugginess(infoClass[infoClass.length - 1],"BOOKKEEPER", Integer.parseInt(info[0])).toString());
				rw.append("\n");
				rw.flush();
			}
	
	
			}catch (IOException e) {
	
				e.printStackTrace();
			}
	}
	
	public static Boolean classAffectedByBugginess(String className, String nameProj,Integer numVersion) {
		
		try(BufferedReader rd = new BufferedReader(new FileReader("classTouchedByCommitBuggy"+ nameProj.toLowerCase()+".txt"))){
			
			String classProj;
			
			while ((classProj = rd.readLine()) != null) {
				if(classProj.contains(className)) { 
				  return getBugginess(nameProj, numVersion, classProj);
				}
			}
		} catch (IOException|JSONException|ParseException e) {
			
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static String classTouchedByCommitBug(String nameProj) {
		
		String s;
		
		try(BufferedReader rd = new BufferedReader(new FileReader("classTouchedByCommitBuggy"+ nameProj.toLowerCase()+".txt"))){
			
			while ((s = rd.readLine()) != null) {
				String[] info = s.split(" ");
				return info[info.length - 1];
				
			}
			
			
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
			
	}

	public static void main(String[] args) {
		
		String namePrj = "BOOKKEEPER";
		
		//p = new Double[8];
		
			//new MetricsCSV("BOOKKEEPER", "C:\\Users\\Simone Benedetti\\Documents\\Bookkeeper", 0);
	/*for(Integer i= 0; i<8; i++) {	
		try {
			p[i] = MetricsCSV.proportionIncremental(namePrj, i+1);
		} catch (JSONException | IOException | ParseException e) {
			
			e.printStackTrace();
		}
	}
		
		
	for(Integer i= 0; i<8; i++) {
		logger.log(Level.INFO,"calcolo proportion: p = {0}", p[i]);
	}*/
	
		
			try(BufferedWriter rw = new BufferedWriter(new FileWriter("classAndMetrics.txt", true))){	
				
				writeOnFile(rw, namePrj);
				
			} catch (IOException e1) {
				System.out.println("ERRORE IN SCRITTURA");
				e1.printStackTrace();
			}
			

		

	}

}
