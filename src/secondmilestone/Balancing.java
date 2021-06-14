package secondmilestone;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;

public class Balancing {

	public static FilteredClassifier underSampling(AbstractClassifier ac) throws Exception {
		
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(ac);
		SpreadSubsample  spreadSubsample = new SpreadSubsample();
		String[] opts = new String[]{ "-M", "1.0"};
		spreadSubsample.setOptions(opts);
		fc.setFilter(spreadSubsample);
		return fc;
	}
	
	
	public static FilteredClassifier smote(AbstractClassifier ac,Instances trainingSet,Integer numDefectiveInTraining) throws Exception {
		
		String[] opts;
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(ac);
		SMOTE smote = new SMOTE();
		smote.setInputFormat(trainingSet);
		Float numPercentual = (float) Math.round((float) ((trainingSet.numInstances() - 2*numDefectiveInTraining)*100)/(float) numDefectiveInTraining);
		opts = new String[] {"-P", numPercentual.toString()};
		smote.setOptions(opts);
		fc.setFilter(smote);
		return fc;
	}
	
	public static FilteredClassifier overSampling(AbstractClassifier ac,Instances trainingSet) throws Exception { 
		
		String[] opts;
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(ac);
		Resample resample = new Resample();
		resample.setInputFormat(trainingSet);
		
		opts = new String[]{ "-B", "1.0","-S","1", "-Z", "100.0"};
		resample.setOptions(opts);
		fc.setFilter(resample);
		
		return fc;
		
	}
	
	public static void main(String[] args) {
		
		int items = -2000;
		
		for(int i = 0; i< items; i++) {
			
		}
//		System.out.println(r.toString());
	}

}
