package metrics;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.json.JSONException;

import main.CreationFileCSV;

public class MetricsNumber {
	
	private Integer locAdded = 0;
	private static String pathName = System.getProperties().getProperty("user.home")+File.separator;
	private Integer numRevision = 0;
	private Integer maxLocAdded = 0;
	private Integer churn = 0;
	private Integer maxChurn = 0;
	private Integer chgSetSize = 0;
	private Integer maxChgSetSize = 0;
	private Integer locDeleted = 0;
	private Integer locTouched = 0;
	private Integer loc = 0;
	private Integer numCommit = 0;
	
	public Repository setup(String nameProj) {
		
		
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		String repoFolder = pathName+nameProj+"/.git";
		Repository repo = null;
		
		try {
			repo = builder.setGitDir(new File(repoFolder)).readEnvironment().findGitDir().build();
		} catch (IOException e1) {
		
			e1.printStackTrace();
		}
		
		return repo;
	}
	
	public synchronized void updateEditType(Edit edit) {
		switch (edit.getType()) {
		case INSERT:
			this.locAdded += edit.getLengthB();
			if(this.maxLocAdded < edit.getLengthB()) {
				this.maxLocAdded = edit.getLengthB();
			}
			break;

		case DELETE:
			this.locDeleted += edit.getLengthA();
			break;

		case REPLACE:
			int diff = edit.getLengthA() - edit.getLengthB();
			if (diff > 0) {
				this.locAdded += edit.getLengthA();
				if(this.maxLocAdded < edit.getLengthA()) {
					this.maxLocAdded = edit.getLengthA();
				}
				this.locDeleted += edit.getLengthB();
			} else {
				this.locDeleted += edit.getLengthA();
				this.locAdded += edit.getLengthB();
				if(this.maxLocAdded < edit.getLengthB()) {
					this.maxLocAdded = edit.getLengthB();
				}
			}
			break;

		case EMPTY:
			break;
	}
	}
	
	
	public synchronized void updateMetrics(DiffEntry entry,DiffFormatter diffFormatter) throws IOException {
		
		FileHeader fileHeader = diffFormatter.toFileHeader( entry );
		List<? extends HunkHeader> hunks = fileHeader.getHunks();
		for( HunkHeader hunk : hunks ) {
			EditList edits = hunk.toEditList();
				for (Edit edit : edits) {
					this.locTouched += 1;
					updateEditType(edit);
				}
		}
		this.churn = this.churn + (this.locAdded - this.locDeleted);
		if(this.maxChurn < (this.locAdded - this.locDeleted)) {
			this.maxChurn = (this.locAdded - this.locDeleted);
		}
		
	}
	
	public synchronized List<DiffEntry> commitTree(Git git,ObjectReader reader,RevCommit commit, RevCommit commitOld,DiffFormatter diff) throws IOException{
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
		oldTreeIter.reset( reader, commitOld.getTree() );
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
		newTreeIter.reset( reader, commit.getTree() );

		
		diff.setRepository( git.getRepository() );
		diff.setContext( 0 );
		return diff.scan( oldTreeIter, newTreeIter );
		
	}
	
	public synchronized void updateOtherMetrics(List<DiffEntry> entries,Repository repo,RevCommit commit,String className) throws IOException {
		this.numCommit ++;
		this.numRevision ++;
		this.chgSetSize += entries.size();
		if(this.maxChgSetSize < entries.size()) {
			this.maxChgSetSize = entries.size();
		}
		if (this.numCommit.equals(1)) {
			this.loc = countLinesOfFileInCommit(repo,commit,className);
		}
	}
	
	public synchronized List<Integer> calculateMetrics(Repository repo,Integer version,String className,List<String> dateVersion) {
		
		List<Integer> metrics = new ArrayList<>();
		
			try (Git git = new Git(repo)) {
				
					RevWalk walk = new RevWalk(repo);
					List<RevCommit> commits = call(repo, className, dateVersion, version);
					
				for(RevCommit commit: commits) {	
					if(commit.getParentCount() != 0) {
						
						RevCommit commitOld = commit.getParent(0);
						try(ObjectReader reader = git.getRepository().newObjectReader()){
							DiffFormatter diffFormatter = new DiffFormatter( DisabledOutputStream.INSTANCE );
							List<DiffEntry> entries = commitTree(git,reader,commit,commitOld,diffFormatter);
					
						// Print the contents of the DiffEntries
							for( DiffEntry entry : entries ) {
								if (entry.getOldPath().contains(className)) {
									updateOtherMetrics(entries,repo,commit,className);
									updateMetrics(entry,diffFormatter);
								}
							}
						
							diffFormatter.close();
						}
					
					}	
					
					
				walk.close();
			}
			metrics.add(this.loc);	
			metrics.add(this.locTouched);
			metrics.add(this.numRevision);
			metrics.add(this.locAdded);
			metrics.add(this.maxLocAdded);
			metrics.add(getAvgLocAdded());
			metrics.add(this.churn);
			metrics.add(this.maxChurn);
			metrics.add(getAvgChurn());
			metrics.add(this.chgSetSize);
			metrics.add(this.maxChgSetSize);
			metrics.add(getAvgChgSetSize());
			
			} catch (RevisionSyntaxException | IOException | GitAPIException | ParseException e) {
					
				e.printStackTrace();
			}
			return metrics; 
	}
	
	private static int countLinesOfFileInCommit(Repository repository, RevCommit commit, String name) throws IOException {
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevTree tree = commit.getTree();
      
            // now try to find a specific file
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(name));
                if (!treeWalk.next()) {
                    throw new IllegalStateException("Did not find expected file 'README.md'");
                }

                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);

                // load the content of the file into a stream
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                loader.copyTo(stream);

                revWalk.dispose();

                return IOUtils.readLines(new ByteArrayInputStream(stream.toByteArray()), StandardCharsets.UTF_8).size();
            }
        }
    }
	
	
	public List<RevCommit> call(Repository rep,String className,List<String> dateVersion, Integer version) throws IOException, GitAPIException, ParseException {
	    
		ArrayList<RevCommit> commits = new ArrayList<>();
	    
	    Date until = new SimpleDateFormat("yyyy-MM-dd").parse(dateVersion.get(version));
	    RevFilter between = CommitTimeRevFilter.before(until);
	    Git git = new Git(rep);
	    RevCommit start = null;
	    
	    do {
	        Iterable<RevCommit> log = git.log().all().setRevFilter(between).addPath(className).call();
	        for (RevCommit commit : log) {
	            if (commits.contains(commit)) {
	                start = null;
	            } else {
	                start = commit;
	                commits.add(commit);
	            }
	        }
	        if (start == null) return commits;
	    }
	    while ((className = getRenamedPath(start,git,rep,className)) != null);
	    
	    git.close();
	    return commits;
	}
	
	private String getRenamedPath( RevCommit start, Git git, Repository rep,String className) throws IOException, GitAPIException {
        Iterable<RevCommit> allCommitsLater = git.log().add(start).call();
        for (RevCommit commit : allCommitsLater) {

            TreeWalk tw = new TreeWalk(rep);
            tw.addTree(commit.getTree());
            tw.addTree(start.getTree());
            tw.setRecursive(true);
            RenameDetector rd = new RenameDetector(rep);
            rd.addAll(DiffEntry.scan(tw));
            List<DiffEntry> files = rd.compute();
            for (DiffEntry diffEntry : files) {
                if ((diffEntry.getChangeType() == DiffEntry.ChangeType.RENAME || diffEntry.getChangeType() == DiffEntry.ChangeType.COPY) && diffEntry.getNewPath().contains(className)) {
               
                    return diffEntry.getOldPath();
                }
            }
        }
        return null;
    }
	
	
	public Integer getMaxLocAdded() {
		return this.maxLocAdded;
	}

	
	public Integer getAvgLocAdded() {
		return  (int) (Math.round((double) this.locAdded / (double) this.numRevision));
	}
	
	
	public Integer getAvgChurn() {
		return (int) (Math.round((double) this.churn / (double) this.numRevision));
	}
	
	public Integer getAvgChgSetSize() {
		return (int) (Math.round((double) this.chgSetSize / (double) this.numRevision));
	}


	public Integer getMaxChurn() {
		return this.maxChurn;
	}
	
	public Integer getMaxChgSetSize() {
		return this.maxChgSetSize;
	}


	public static void main(String[] args) {
		
		List<String> lv = null;
		
		try {
			lv = CreationFileCSV.getInfoVersions("Bookkeeper",3);
		} catch (JSONException|IOException e1) {
			e1.printStackTrace();
		} 
		
		
		
		MetricsNumber mn = new MetricsNumber();
		Repository rep = mn.setup("Bookkeeper");
		mn.calculateMetrics(rep,1,"bookkeeper-server/src/main/java/org/apache/bookkeeper/bookie/Bookie.java",lv);
		
		
		
		/* revisioni:14
		locDeleted:188
		locAdded:1278
		*/

	}

}
