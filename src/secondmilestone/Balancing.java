package secondmilestone;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;

public class Balancing {
	
	private Balancing() {
		    throw new IllegalStateException("Weka class");
	}


	public static FilteredClassifier underSampling(AbstractClassifier ac){
		
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(ac);
		SpreadSubsample  spreadSubsample = new SpreadSubsample();
		String[] opts = new String[]{ "-M", "1.0"};
		try {
			spreadSubsample.setOptions(opts);
		} catch (Exception e) {
			e.printStackTrace();
		}
		fc.setFilter(spreadSubsample);
		return fc;
	}
	
	
	public static FilteredClassifier smote(AbstractClassifier ac,Instances trainingSet,Integer numDefectiveInTraining,Integer classIndex){
		FilteredClassifier fc = new FilteredClassifier();
	 try {	
		String[] opts;
		fc.setClassifier(ac);
		SMOTE smote = new SMOTE();
		smote.setInputFormat(trainingSet);
		Float numPercentual = (float) Math.round((float) ((trainingSet.numInstances() - 2*numDefectiveInTraining)*100)/(float) numDefectiveInTraining);
		opts = new String[] {"-P", numPercentual.toString(),"-K","1","-C",classIndex.toString()};
		smote.setOptions(opts);
		fc.setFilter(smote);
	 }catch (Exception e) {
		 e.printStackTrace();
	 }
		return fc;
	}
	
	public static FilteredClassifier overSampling(AbstractClassifier ac,Instances trainingSet){ 
		FilteredClassifier fc = new FilteredClassifier();
	 try {	
		String[] opts;
		fc.setClassifier(ac);
		Resample resample = new Resample();
		resample.setInputFormat(trainingSet);
		
		opts = new String[]{ "-B", "1.0","-S","1", "-Z", "100.0"};
		resample.setOptions(opts);
		fc.setFilter(resample);
	 }catch (Exception e) {
		 e.printStackTrace();
	 }
		
		return fc;
		
	}
	
}
