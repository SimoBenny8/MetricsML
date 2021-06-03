package secondmilestone;

import java.io.File;
import java.io.IOException;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

public class CsvToArff {
	
	private static String[] proj = {"Bookkeeper","Storm"};
	private static String path = "src/Files/";
	
	public static void arffCreation(String path,String proj) throws IOException {
		File projectClasses = new File(path+proj);
		if (projectClasses.exists()) {
			CSVLoader loader = new CSVLoader();
			loader.setFieldSeparator(",");
		    loader.setSource(projectClasses);
		    Instances data = loader.getDataSet();//get instances object

		    data.deleteAttributeAt(0);//delete name project
		    
		    // save ARFF
		    ArffSaver saver = new ArffSaver();
		    saver.setInstances(data);//set the dataset we want to convert
		    //and save as ARFF
		    saver.setFile(new File(proj.toUpperCase()+"_Dataset.arff"));
		    saver.writeBatch();
		}
		
	}
	
	
	public static void main(String[] args) {
		
		
		for(String s: proj) {	
		
			try {
				arffCreation(path,s);
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
	}

}
