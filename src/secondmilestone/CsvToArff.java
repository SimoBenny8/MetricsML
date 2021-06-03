package secondmilestone;

import java.io.File;
import java.io.IOException;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

public class CsvToArff {

	public static void arffCreation() throws IOException {
		File projectClasses = new File("csvFile.csv");
		if (projectClasses.exists()) {
			CSVLoader loader = new CSVLoader();
			loader.setFieldSeparator(";");
		    loader.setSource(projectClasses);
		    Instances data = loader.getDataSet();//get instances object

		    data.deleteAttributeAt(1);//delete name project
		    
		    // save ARFF
		    ArffSaver saver = new ArffSaver();
		    saver.setInstances(data);//set the dataset we want to convert
		    //and save as ARFF
		    saver.setFile(new File("csvFile.arff"));
		    saver.writeBatch();
		}
		
	}
	
	
	public static void main(String[] args) {
		
		
	}

}
