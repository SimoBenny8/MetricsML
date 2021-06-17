package main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.json.JSONException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;





public class CreationFileCSV {
	
	
	
	private static final Logger logger = Logger.getLogger(CreationFileCSV.class.getName());
	private static String[] projects = {"Bookkeeper","Storm"};
	private static final String ERRORSTR = "Exception found";
	private static final String CMD = "cmd /c cd ";
	
	
	public static void removeVersion(String nameProj,List<String> info) {
		
		if(nameProj.equals("Bookkeeper")) {
			info.remove(3); //remove fourth version of bookkeeper
		}else {
			info.remove(4);
			info.remove(8);//ninth version of storm
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
			
			removeVersion(nameProj,info);
			
		}
		return info;
	}
	
	public static Multimap<String, Integer> idCommitToVersionProject(String commitId,String namePrj,Integer version) throws IOException{
		
		Multimap<String, Integer> map = ArrayListMultimap.create();
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		String pathName = System.getProperties().getProperty("user.home")+File.separator;
		String repoFolder = pathName+namePrj+"/.git";
		Repository repo = null;
		
		try {
			repo = builder.setGitDir(new File(repoFolder)).readEnvironment().findGitDir().build();
		} catch (IOException e1) {
		
			e1.printStackTrace();
		}
		
		if(repo != null) {
			try (Git git = new Git(repo)) {
				
					RevWalk walk = new RevWalk(repo);
					RevCommit commit = walk.parseCommit(repo.resolve(commitId));
					ObjectId treeId = commit.getTree().getId();
					try (TreeWalk treeWalk = new TreeWalk(repo)) {
						  treeWalk.reset(treeId);
						  treeWalk.setRecursive(true);
						  while (treeWalk.next()) {
						    String path = treeWalk.getPathString();
						    if (path.endsWith(".java")) {
						    	map.put(path,version);
						    }
						   
						  }
						}
					
					walk.close();
			} catch (RevisionSyntaxException | IOException e) {
				
			e.printStackTrace();
			}
		}	
			
			
		return map;
		
	}
			
	public static String idVersionClass(String pathName,String datePrev) throws IOException, InterruptedException {
		//Pathclass contain only java file (ex: Name.java)
		
		Process p;
		String s;
		
		
		p = Runtime.getRuntime().exec(CMD +pathName+"&& git log  --before="+datePrev +" --date-order");
         
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		
		 try {
	        	
         	while ((s = br.readLine()) != null) {
         		if(s.startsWith("commit")) {
         			String[] info = s.split(" ");
         			return info[1];
         			
         		}
         	}
         	p.waitFor();
            p.destroy();
          
     	 
     	 }catch (InterruptedException e) {
         	logger.log(Level.WARNING,ERRORSTR);
         	Thread.currentThread().interrupt();
     	 }
		
         return null;
             	
	}

	
	
	public static Multimap<String,Integer> toCsv(List<String> lv,String pathName,String proj,Integer version) throws IOException{
		
		List<String> commitId = new ArrayList<>();
		
		for(Integer i = 0; i<((lv.size())/2); i++) {
			
			try {
				commitId.add(idVersionClass(pathName, lv.get(i)));
			} catch (IOException | InterruptedException e) {
				 Thread.currentThread().interrupt();	
				e.printStackTrace();
			}
			
		}
		
		return idCommitToVersionProject(commitId.get(version-1),proj,version);
	}
		
		
	public static void main(String[] args){
		
		
		
	for(String proj: projects) {
			
			String pathName = System.getProperties().getProperty("user.home")+File.separator+proj;
			
			String projectRepo = "https://github.com/apache/" + proj + ".git";
		
			if (!Files.exists(Paths.get(pathName))) { 
				try {
					Git.cloneRepository()
					.setURI(projectRepo)
					.setDirectory(new File(pathName)) 
					.call();
				} catch (GitAPIException e2) {
				
					e2.printStackTrace();
				}
			}
			
				
		
			}
		
		
		}
	}


	



