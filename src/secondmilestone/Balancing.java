package secondmilestone;

import java.util.Locale;

import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;

public class Balancing {

	public static void underSampling() throws Exception {
		FilteredClassifier fc = null;
		String[] opts = null;
		
		RandomForest classifier = new RandomForest();
		fc = new FilteredClassifier();
		fc.setClassifier(classifier);
		SpreadSubsample  spreadSubsample = new SpreadSubsample();
		opts = new String[]{ "-M", "1.0"};
		spreadSubsample.setOptions(opts);
		fc.setFilter(spreadSubsample);
		//abstractClassfier = fc;
	}
	
	
	public static void smote() {
		FilteredClassifier fc = null;
		
		RandomForest classifier = new RandomForest();
		fc = new FilteredClassifier();
		fc.setClassifier(classifier);
		SMOTE smote = new SMOTE();
		//smote.setInputFormat(this.trainingSet);
		fc.setFilter(smote);
	}
	
	public static void overSampling() { //TODO
		FilteredClassifier fc = null;
		
		RandomForest classifier = new RandomForest();
		// Check what is the majority class
		//Integer trainingSize = this.trainingSet.size();
		//Double sampleSizePercent =  ((this.numDefectiveInTraining > ((double) trainingSize / 2.0)) ? (double) this.numDefectiveInTraining/trainingSize : (1 - ((double) this.numDefectiveInTraining/trainingSize)));
		//sampleSizePercent = sampleSizePercent * 100 * 2;
		Resample resample = new Resample();
		//resample.setInputFormat(this.trainingSet);
		//opts = new String[]{ "-B", "1.0", "-Z", String.format(Locale.US, "%.2f", sampleSizePercent)};
		//resample.setOptions(opts);
		fc.setFilter(resample);
		
	}
	
	public static void main(String[] args) {
		
	}

}
