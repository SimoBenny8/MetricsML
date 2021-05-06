package main;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONException;





public class CreationFileCSV {
	
	
	
	private static final Logger logger = Logger.getLogger(CreationFileCSV.class.getName());
	private static String[] projects = {"Bookkeeper"};
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
		
		if(GetReleaseInfo.getReleaseinCSV(nameProj.toUpperCase())) {
			
			try(BufferedReader csvReader = new BufferedReader(new FileReader(nameProj.toUpperCase()+"VersionInfo.csv"))){
			
				while ((s = csvReader.readLine()) != null) {
					String[] data = s.split(",");
					if(!data[data.length -1].equals("Date")) {
						if(position.equals(data.length - 1)) {
							String sub = data[position].substring(0,10);
							info.add(sub);
						}else {
							info.add(data[position]);
						}
					}
				}
				
			}catch(IOException e) {
				e.printStackTrace();
			}
			
			
			Integer halfInfo = ((info.size())/2);
			
			while(!halfInfo.equals(info.size())) {
				
				info.remove(info.size() - 1);
			}
		}
		return info;
	}
	
	public static void idVersionClass(String pathName,String namePrj,String datePrev,String dateNext) throws IOException, InterruptedException {
		//Pathclass contain only java file (ex: Name.java)
		//TODO: split string path classes before call this method
		Process p;
		String s;
		FileWriter result;
		String s2;
		Process p2 = null;
		
		
		if(dateNext.equals(""))
			p = Runtime.getRuntime().exec(CMD +pathName + namePrj+"&& git log  --before="+datePrev +" --date-order --abbrev-commit --pretty=format:"+"%h");
		else 
			p = Runtime.getRuntime().exec(CMD +pathName + namePrj+"&& git log  --before="+dateNext + "--after="+ datePrev +"--date-order --abbrev-commit --pretty=format:"+"%h");	
         
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		
		/*RevWalk walk = new RevWalk(rep);
		RevCommit commit = walk.parseCommit(objectIdOfCommit);*/
		
		result = new FileWriter("commitId"+namePrj+datePrev+".txt");
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
         
             	
	}
	
	public static void versioningClass(String dateV,String namePrj,String pathName,Integer version) throws IOException {
		//get commit id and write classes touch by a commit in a determinate version
		String s;
		Process p = null;
		String s2;
		Integer i = 0;
		Integer versAdded = version+1;
		
		
		try(BufferedWriter wd = new BufferedWriter(new FileWriter("class"+namePrj.toLowerCase()+versAdded.toString()+".txt", true))){
			
			try(BufferedReader rd = new BufferedReader(new FileReader("commitId"+namePrj+dateV+".txt"))){
				  Set<String> set = new HashSet<>();
					while ((s = rd.readLine()) != null) {
							p = Runtime.getRuntime().exec(CMD +pathName + namePrj+"&& git diff-tree --no-commit-id --name-only -r "+ s);	
							BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
								while ((s2 = br.readLine()) != null) {
										if(s2.endsWith(".java") && set.add(s2)) {
												wd.append(s2);
												wd.append("\n");
												//System.out.println("Class Found" + s2);
										}
								}
					}
					if(p != null) {
						p.waitFor();
						p.destroy();
					}
				
				}catch(IOException | InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt();

				}finally {
					Path path = Paths.get("commitId"+namePrj+dateV+".txt");
					Files.delete(path);
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
	}
	
	
	
	
	public static Boolean pathClasses(String namePrj,String pathName) {
		
		//return a file with a path of java classes of a Apache project
		
		
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
					s = str.substring(pathName.length()+namePrj.length() + 1);
					writer.write(s + System.lineSeparator());
			} 
		  
		}catch (IOException e) {
			e.printStackTrace();
		}
	
		return true;
	}
		
		
	public static void main(String[] args) throws IOException, InterruptedException {
		
		String pathName = "C:/Users/"+OsUtils.getUserName()+"/Documents/";
		String s;
		String className;
		Process p = null;
		
		List<String> lv = null;
		Integer versNum = null;
		
		for(String proj: projects) {
			
			String projectRepo = "https://github.com/apache/" + proj + ".git";
			
			try {
				Git.cloneRepository()
				  .setURI(projectRepo)
				  .setDirectory(new File(pathName)) 
				  .call();
			} catch (GitAPIException e2) {
				
				e2.printStackTrace();
			}


			try {
				lv = getInfoVersions(proj,3);
			} catch (JSONException|IOException e1) {
				e1.printStackTrace();
			} 
			
			if(lv != null) {
				for(Integer i = 0; i<lv.size(); i++) {
					if(i.equals(0)) {
						try {
							idVersionClass(pathName, proj, lv.get(i),"");
							System.out.println("Scritta versione iniziale");
						} catch (IOException | InterruptedException e) {
							 Thread.currentThread().interrupt();
							e.printStackTrace();
						}
				
					}else{
						try {
							idVersionClass(pathName, proj, lv.get(i),lv.get(i-1));
							System.out.println("Scritta versioni successive");
						} catch (IOException | InterruptedException e) {
							 Thread.currentThread().interrupt();	
							e.printStackTrace();
						}
				
					}
					
			
				}
				
				for(Integer i = 0; i<lv.size(); i++) {
					try {
						versioningClass(lv.get(i),proj,pathName,i);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
		
			}
		}
	
	}
}	



