package secondmilestone;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import main.MetricsCSV;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.RemoveDuplicates;

public class CsvToArff {
	
	
	private static final Logger logger = Logger.getLogger(CsvToArff.class.getName());
	private static String[] proj = {"Bookkeeper","Storm"};
	private static String path = "src/Files/";
	
	public static void arffCreation(File projectClasses) throws Exception {
		
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
		    saver.setFile(new File(projectClasses.getName()+".arff"));
		    saver.writeBatch();
		}
		
	}
	
	public static void splitCsvFile(String path,String proj, Integer versionMin,Integer versionMax) throws IOException {
		File projectClasses = new File(path+proj.toUpperCase()+"_Dataset.csv");
		if (projectClasses.exists()) {
			try(BufferedWriter rw = new BufferedWriter(new FileWriter(proj.toUpperCase()+"_Version"+versionMin.toString()+"-"+versionMax.toString()+"_Weka.csv", true))){
				String[] columnsCsv = MetricsCSV.getColumnsCsv();
				for(Integer i = 0; i<columnsCsv.length - 1; i++) {
					rw.append(columnsCsv[i] + ",");
					
				}
				rw.append(columnsCsv[columnsCsv.length-1]);
				rw.append("\n");
				try(BufferedReader rd = new BufferedReader(new FileReader(projectClasses))){
					String s;
					while((s = rd.readLine()) != null) {
						String[] data = s.split(",");
						if(!data[1].equals("Version")) {
							if (Integer.parseInt(data[1])>= versionMin && Integer.parseInt(data[1])<= versionMax) {
								rw.append(s);
								rw.append("\n");
							}
						}else {
							logger.log(Level.INFO,"tag row");
						}
					}
				}
			}
		}
		
	}
	
	
	public static void main(String[] args){
		
		for(String s: proj) {
			
			File projectClasses = new File(path+s.toUpperCase()+"_Dataset.csv");
			
		
			try {
				arffCreation(projectClasses);
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
	}

}
