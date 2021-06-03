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

import org.eclipse.jgit.lib.Repository;
import org.json.JSONException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import metrics.MetricsNumber;


public class MetricsCSV {
	
	private ArrayList<String> id;
	private static final Logger logger = Logger.getLogger(MetricsCSV.class.getName());
	private static final String CMD = "cmd /c cd ";
	private static Double[] pB = {1.0,1.0,0.667,0.667,0.667,0.778,1.25,1.271};
	private static Double[] pS = {1.4,1.4,1.4,1.31,1.285,1.356,1.404,1.378,1.352,1.491,1.582,1.582};
	private static String[] columnsCsv = {"Project","Version","Filename","Size","LOC_Touched","NR","LOC_Added", "Max_LOC_Added","Avg_LOC_Added","Churn","Max_Churn","Avg_Churn","ChgSetSize","Max_ChgSetSize","Avg_ChgSetSize","Bugginess"};
	private static String fileNameBuggy = "classTouchedByCommitBuggy";
	
	
	public MetricsCSV(String nameProj, String pathProject) {
				
				
		//get ticket ID from Jira
				try {
					id =(ArrayList<String>) RetrieveTicketsID.getTicketId(nameProj);
				} catch (JSONException|IOException e) {
					logger.log(Level.WARNING,"error getting ticket ID");
					e.printStackTrace();
				} 
				
				
				//Get all commits ID through git log and associated to ticket ID in Jira 
				for(int i=0; i< id.size(); i++) {
					try {
						
						IdCommit.commitString(id.get(i), pathProject, nameProj ,":");
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
	
	
	
	public static Boolean getBugginess(String nameProj,String classProj,Integer numVersion) throws JSONException, IOException, ParseException {
		//nameProj written in MAIUSC
						Integer iv = null;
						Integer fv;
						String[] info = classProj.split(" ");
						
						String avString = RetrieveTicketsID.getIV(info[0]);
						Integer av = stringToVersion(nameProj, avString);
						logger.log(Level.INFO,"rd not null and av: {0}", av);
						
						if(av == null) {
							logger.log(Level.INFO,"av null: calling proportion");
							
							fv = dateToVersion(info[2],nameProj);
							logger.log(Level.INFO,"fv when av is null: {0}", fv);
							Integer ov = dateToVersion(RetrieveTicketsID.creationDateTicket(info[0]),nameProj);
							logger.log(Level.INFO,"ov when av is null: {0}", ov);
							
							
							if(!fv.equals(ov)) {
								iv = (int) Math.round((double)fv - (double)(fv-ov)*pS[numVersion - 1]); 
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
        
        Double p = (double) 0;
        Integer count = 0;
        String s;
       
        try(BufferedReader rd = new BufferedReader(new FileReader(outname))){
        	while ((s = rd.readLine()) != null) {
        		if(version.equals(1))
        			break;
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
	
	//info[] = ticketID commitID commitDate
	public static void getClassFromCommitId(String[] info,String pathName,String namePrj) throws IOException, InterruptedException {
		
		Process p;
		
		String s;
		try(BufferedWriter wd = new BufferedWriter(new FileWriter(fileNameBuggy+namePrj.toLowerCase()+".txt", true))){	
			p = Runtime.getRuntime().exec(CMD +pathName+"&& git diff-tree --no-commit-id --name-only -r "+ info[1]);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ((s = br.readLine()) != null) {
					if(s.endsWith(".java")) {
						wd.append(info[0]+" "+info[1]+" "+info[2]+" ");
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
	
	public static void writeOnFile(BufferedWriter rw, String namePrj,List<String> lv,Multimap<String,Integer> map) throws IOException {
		
			
			String[] keys = map.keySet().stream().sorted().toArray(String[]::new);
			for(int j = 0; j<keys.length;j++) {
				 MetricsNumber mn = new MetricsNumber();
				 Repository rep = mn.setup(namePrj);	
				 List<Integer> metrics = mn.calculateMetrics(rep,Iterables.get(map.get(keys[j]),0),keys[j],lv);
				 if(metrics.get(0).equals(0)) {
					 logger.log(Level.INFO,"salto una classe");
					 continue;
				 }
				 	rw.append(namePrj.toUpperCase()+","+Iterables.get(map.get(keys[j]),0)+ ","+ keys[j] + ",");
					for(Integer i = 0; i<metrics.size(); i++) {
						rw.append(metrics.get(i) +",");
						logger.log(Level.INFO,"scrivo delle metriche");
					}
					
					if(Boolean.TRUE.equals(classAffectedByBugginess(keys[j],namePrj,Iterables.get(map.get(keys[j]),0)))) {
						rw.append("Yes");
					}else {
						rw.append("No");
					}
					
					rw.append("\n");
					rw.flush();
			 }
				
			
		}
				

	public static Boolean classAffectedByBugginess(String className,String nameProj,Integer numVersion) {
		
		
		try(BufferedReader rd = new BufferedReader(new FileReader(fileNameBuggy+ nameProj.toLowerCase()+".txt"))){
			
			String classProj;
			
			while ((classProj = rd.readLine()) != null) {
				if(classProj.contains(className)) { 
					  return getBugginess(nameProj,classProj,numVersion);
					}
					
				
			}
		} catch (IOException|JSONException|ParseException e) {
			
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static String classTouchedByCommitBug(String nameProj) {
		
		String s;
		
		try(BufferedReader rd = new BufferedReader(new FileReader(fileNameBuggy+ nameProj.toLowerCase()+".txt"))){
			
			while ((s = rd.readLine()) != null) {
				String[] info = s.split(" ");
				return info[info.length - 1];
				
			}
			
			
		}catch (IOException e) {
			
			e.printStackTrace();
		}
		
		return null;
			
	}

	public static void main(String[] args) {
		
		String namePrj = "STORM";
		//List<Double> pS = new ArrayList<>();
		String pathName = System.getProperties().getProperty("user.home")+File.separator+namePrj;
		
		//new MetricsCSV("STORM", pathName);
		
		List<String> lv = null;
		
		try {
			lv = CreationFileCSV.getInfoVersions("Storm",3);
			//System.out.println((lv.size())/2);
		} catch (JSONException|IOException e1) {
			e1.printStackTrace();
		}
		
		/*if(lv!= null) {
		
		for(Integer i= 0; i<((lv.size())/2)+1; i++) {	
			try {
				pS.add(MetricsCSV.proportionIncremental(namePrj, i+1));
			} catch (JSONException | IOException | ParseException e) {
			
				e.printStackTrace();
			}
		}
	}
		for(Double d: pS) {
			System.out.println("p:"+d);
		}
		*/
		
		try(BufferedWriter rw = new BufferedWriter(new FileWriter(namePrj+"_Dataset"+".txt", true))){	
			
			for(Integer i = 0; i<columnsCsv.length - 1; i++) {
				rw.append(columnsCsv[i] + ",");
				
			}
			rw.append(columnsCsv[columnsCsv.length-1]);
			rw.append("\n");
			
			if(lv != null) {	
				for(Integer j = 0;j<((lv.size())/2);j++) {
					
					Multimap<String,Integer> map = CreationFileCSV.toCsv(lv,pathName,"Storm",j+1);
					writeOnFile(rw, namePrj,lv,map);
				}
				
				
			}	
		} catch (IOException e1) {
			logger.log(Level.WARNING,"error during writing");
			e1.printStackTrace();
		}
			
	}
	
	
}


