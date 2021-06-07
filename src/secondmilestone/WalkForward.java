package secondmilestone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.experiment.Stats;

public class WalkForward {
	
	private static String[] columns = {"Dataset", "#TrainingRelease", "%Training", "%Defective in training", "%Defective in testing", "Classifier", "Balancing", "FeatureSelection", "Sensitivity", "TP", "FP", "TN", "FN", "Precision", "Recall", "AUC", "Kappa"};
	private static String[] projects = {"Bookkeeper","Storm"};
	
	public static List<Instances> applyWalkForward(File projArff,Integer version,  Double maxVersion) throws IOException {
		
		List<Instances> result = new ArrayList<>();
		
		ArffLoader loader = new ArffLoader();
		loader.setSource(projArff);
		Instances data = loader.getDataSet();//get instances object
		
		
		Instances training = new Instances(data, 0);
		Instances testing = new Instances(data, 0);
		
		for(Instance instance: data) {
			if((double) version < maxVersion) {
				if (instance.value(0) <= version) {
					training.add(instance);
				
				} else if (instance.value(0) == (version + 1)) {
					testing.add(instance);
				
				} 
			}
		}
		
		result.add(training);
		result.add(testing);
		return result;
	}
	
	
	//TODO: testare se funziona
	public static Integer assignBuggyValue(Instances instances) {
		Attribute attribute = instances.attribute(instances.numAttributes() - 1);
		if(attribute.toString().equals("Yes"))
			return 0;
		else
			return 1;
		
	}
	
	
	public static void main(String[] args) throws Exception {
		
		File file = new File("BOOKKEEPER_Dataset.arff");
		List<Instances> wf = new ArrayList<>();
		
		ArffLoader loader = new ArffLoader();
		loader.setSource(file);
		Instances data = loader.getDataSet();//get instances object
	try(BufferedWriter rw = new BufferedWriter(new FileWriter(projects[0]+"_Results"+".txt", true))){
			
			for(Integer j = 0; j<columns.length - 1; j++) {
				rw.append(columns[j] + ",");
				
			}
			rw.append(columns[columns.length-1]);
			rw.append("\n");
		
			AttributeStats as = data.attributeStats(0); //version column
			Stats s = as.numericStats;
			Double maxVersion = s.max;	
		
		for(Integer i = 1; i< maxVersion; i++) {
			
			try {
				wf = applyWalkForward(file,i,maxVersion);
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			
			MetricsWeka mw = new MetricsWeka();
			mw.setPercentualTraining((float) wf.get(0).numInstances());
			mw.setNumTrainingVersions(i);
			
			Integer classIndex = assignBuggyValue(wf.get(0));
			int numAttributes = wf.get(0).numAttributes();
			wf.get(0).setClassIndex(numAttributes - 1);
			wf.get(1).setClassIndex(numAttributes - 1);
			
			NaiveBayes nbClassifier = new NaiveBayes();
			RandomForest rfClassifier = new RandomForest();
			IBk ibkClassifier = new IBk();

			nbClassifier.buildClassifier(wf.get(0));
			rfClassifier.buildClassifier(wf.get(0));
			ibkClassifier.buildClassifier(wf.get(0));
			
			Evaluation eval = new Evaluation(wf.get(1));
			Evaluation eval2 = new Evaluation(wf.get(1));	
			Evaluation eval3 = new Evaluation(wf.get(1));	
			
			eval.evaluateModel(nbClassifier, wf.get(1)); 
			eval2.evaluateModel(rfClassifier, wf.get(1)); 
			eval3.evaluateModel(ibkClassifier, wf.get(1)); 
			
			//rw.append(projects[0].toUpperCase()+","+i.toString()+","+(wf.get(0).numInstances())/data.numInstances()*100+",");
			
		/*	Float truePositives = (float) eval.numTruePositives(classIndex);
			Float falsePositives = (float) eval.numFalsePositives(classIndex);
			Float trueNegatives = (float) eval.numTrueNegatives(classIndex);
			Float falseNegatives = (float) eval.numFalseNegatives(classIndex);
			
			System.out.println("AUC1 = "+eval.areaUnderROC(classIndex));
			System.out.println("kappa1 = "+eval.kappa());
			System.out.println("Precision1 = "+eval.precision(classIndex));//rispetto ad un valore che identifica buggyness
			System.out.println("Recall1 = "+eval.recall(classIndex));
			
			System.out.println("AUC2 = "+eval2.areaUnderROC(classIndex));
			System.out.println("kappa2 = "+eval2.kappa());
			System.out.println("Precision2 = "+eval2.precision(classIndex));//rispetto ad un valore che identifica buggyness
			System.out.println("Recall2 = "+eval2.recall(classIndex));
			
			System.out.println("AUC3 = "+eval3.areaUnderROC(classIndex));
			System.out.println("kappa3 = "+eval3.kappa());
			System.out.println("Precision3 = "+eval3.precision(classIndex));//rispetto ad un valore che identifica buggyness
			System.out.println("Recall3 = "+eval3.recall(classIndex));*/
			
			
		}
	}
  }

}
