package secondmilestone;

import java.io.File;
import java.io.IOException;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.RemoveDuplicates;

public class CsvToArff {
	
	private static String[] proj = {"Bookkeeper","Storm"};
	private static String path = "src/Files/";
	
	public static void arffCreation(String path,String proj) throws Exception {
		File projectClasses = new File(path+proj.toUpperCase()+"_Dataset.csv");
		if (projectClasses.exists()) {
			CSVLoader loader = new CSVLoader();
			loader.setFieldSeparator(",");
		    loader.setSource(projectClasses);
		    Instances data = loader.getDataSet();//get instances object

		    data.deleteAttributeAt(0);//delete name project
		    data.deleteAttributeAt(1);//delete class project
		    
		    RemoveDuplicates rd = new RemoveDuplicates();
		    rd.setInputFormat(data);
		    
		    Instances subset = Filter.useFilter(data,rd);
		    
		    // save ARFF
		    ArffSaver saver = new ArffSaver();
		    saver.setInstances(subset);//set the dataset we want to convert
		    //and save as ARFF
		    saver.setFile(new File(proj.toUpperCase()+"_Dataset.arff"));
		    saver.writeBatch();
		}
		
	}
	
	
	public static void main(String[] args) {
		
		
		for(String s: proj) {	
		
			try {
				arffCreation(path,s);
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
	}

}
