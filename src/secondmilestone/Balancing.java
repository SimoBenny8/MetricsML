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
	
	
	public static FilteredClassifier smote(AbstractClassifier ac,Instances trainingSet) throws Exception {
		
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(ac);
		SMOTE smote = new SMOTE();
		smote.setInputFormat(trainingSet);
		fc.setFilter(smote);
		return fc;
	}
	
	public static FilteredClassifier overSampling(AbstractClassifier ac,Instances trainingSet) throws Exception { //Verificare
		
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(ac);
		// Check what is the majority class
		//Integer trainingSize = trainingSet.size();
		//Double sampleSizePercent =  ((this.numDefectiveInTraining > ((double) trainingSize / 2.0)) ? (double) this.numDefectiveInTraining/trainingSize : (1 - ((double) this.numDefectiveInTraining/trainingSize)));
		//sampleSizePercent = sampleSizePercent * 100 * 2;
		Resample resample = new Resample();
		resample.setInputFormat(trainingSet);
		//opts = new String[]{ "-B", "1.0", "-Z", String.format(Locale.US, "%.2f", sampleSizePercent)};
		//resample.setOptions(opts);
		fc.setFilter(resample);
		
		return fc;
		
	}
	
	public static void main(String[] args) {
		
	}

}
