package secondmilestone;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class Classifier {

	
	
	public static void main(String[] args) throws Exception {
		
		DataSource source1 = new DataSource("C:/Program Files/Weka-3-8/data/breast-cancerKnown.arff");
		Instances training = source1.getDataSet();
		DataSource source2 = new DataSource("C:/Program Files/Weka-3-8/data/breast-cancerNOTK.arff");
		Instances testing = source2.getDataSet();

		int numAttr = training.numAttributes();
		training.setClassIndex(numAttr - 1);
		testing.setClassIndex(numAttr - 1);

		NaiveBayes nbClassifier = new NaiveBayes();
		RandomForest rfClassifier = new RandomForest();
		IBk ibkClassifier = new IBk();

		nbClassifier.buildClassifier(training);
		rfClassifier.buildClassifier(training);
		ibkClassifier.buildClassifier(training);
		
		Evaluation eval1 = new Evaluation(testing);	
		Evaluation eval2 = new Evaluation(testing);	
		Evaluation eval3 = new Evaluation(testing);	
		
		eval1.evaluateModel(nbClassifier, testing); 
		eval2.evaluateModel(rfClassifier, testing); 
		eval3.evaluateModel(ibkClassifier, testing); 
		
		System.out.println("AUC = "+eval1.areaUnderROC(1));
		System.out.println("kappa = "+eval1.kappa());
		System.out.println("Precision = "+eval1.precision(1));//rispetto ad un valore che identifica buggyness
		System.out.println("Recall = "+eval1.recall(1));
		
	}

}
