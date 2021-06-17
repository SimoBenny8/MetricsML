package secondmilestone;

import java.io.BufferedWriter;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.experiment.Stats;

enum FeatureSelectionEnum{
	NOFEATURE,
	FEATURE
}

enum Sampling{
	NOSAMPLING,
	UNDERSAMPLING,
	OVERSAMPLING,
	SMOTE
}

enum CostSensitiveEnum{
	NOCOST,
	CSTHRESHOLD,
	CSLEARNING
}

public class WalkForward {
	
	private static String[] columns = {"Dataset", "#TrainingRelease", "%Training", "%Defective in training", "%Defective in testing", "Classifier", "Balancing", "FeatureSelection", "Sensitivity", "TP", "FP", "TN", "FN", "Precision", "Recall", "AUC", "Kappa"};
	private static String[] projects = {"Bookkeeper","Storm"};
	private static Integer datasetSize;
	private static AbstractClassifier[] classifier = {new NaiveBayes(),new RandomForest(),new IBk()};
	private static Integer numDefectiveInTraining;
	private static Integer numDefectiveInTesting;
	
	public static List<Instances> applyWalkForward(File projArff,Integer version,  Double maxVersion) throws IOException {
		
		List<Instances> result = new ArrayList<>();
		
		ArffLoader loader = new ArffLoader();
		loader.setSource(projArff);
		Instances data = loader.getDataSet();//get instances object
		datasetSize = data.size();
		
		Integer classIndex = assignBuggyValue(data);
		Instances training = new Instances(data, 0);
		Instances testing = new Instances(data, 0);
		
		numDefectiveInTraining = 0;
		numDefectiveInTesting = 0;
	
		for(Instance instance: data) {
			if((double) version < maxVersion) {
				if (instance.value(0) <= version) {
					training.add(instance);
					if (instance.value(instance.numAttributes() - 1) == classIndex) {
						numDefectiveInTraining++;
					}
				
				} else if (instance.value(0) == (version + 1)) {
					testing.add(instance);
					if (instance.value(instance.numAttributes() - 1) == classIndex) {
						numDefectiveInTesting++;
					}
				} 
			}
		}
		
		result.add(training);
		result.add(testing);
		return result;
	}
	
	
	
	public static Integer assignBuggyValue(Instances instances) {
		Attribute attribute = instances.attribute(instances.numAttributes() - 1);
		if(attribute.toString().equals("Yes"))
			return 0;
		else
			return 1;
		
	}
	
	public static synchronized void valutationToCsv(Instances training, Instances testing,Integer classIndex,Integer i,BufferedWriter rw, String featureSel) throws Exception {
		
		for(Sampling samp: Sampling.values()) {
			
			for(int j = 0; j <classifier.length; j++) {
				Evaluation evalThreshold;
				Evaluation evalLearning;
				Evaluation evalNoCost;
	
				Float truePositivesth;
				Float falsePositivesth;
				Float trueNegativesth;
				Float falseNegativesth;
				
				Float truePositives;
				Float falsePositives;
				Float trueNegatives;
				Float falseNegatives;
				
				Float truePositivesln;
				Float falsePositivesln;
				Float trueNegativesln;
				Float falseNegativesln;
				
				FilteredClassifier fcnb = null;
				FilteredClassifier fcos = null;
				FilteredClassifier fcsmote = null;
				
				MetricsWeka mw = new MetricsWeka();
				mw.setDatasetSize(datasetSize);
				mw.setPercentualTraining((float) training.numInstances());
				mw.setNumTrainingVersions(i);
				mw.setPercDefectiveInTraining(numDefectiveInTraining);
				mw.setPercDefectiveInTesting(numDefectiveInTesting);
				
				int numAttributes = training.numAttributes();
				training.setClassIndex(numAttributes - 1);
				testing.setClassIndex(numAttributes - 1);
				
				switch(samp) {
				
					case NOSAMPLING:
						
						classifier[j].buildClassifier(training);
						
						evalThreshold = CostSensitive.applyCostSensitive(classifier[j],training,testing,true);
						evalLearning = CostSensitive.applyCostSensitive(classifier[j],training,testing,false);	
						evalNoCost = new Evaluation(training);	
						
						evalThreshold.evaluateModel(classifier[j], testing); 
						evalLearning.evaluateModel(classifier[j], testing); 
						evalNoCost.evaluateModel(classifier[j], testing); 
						
						truePositivesth = (float) evalThreshold.numTruePositives(classIndex);
						falsePositivesth = (float) evalThreshold.numFalsePositives(classIndex);
						trueNegativesth = (float) evalThreshold.numTrueNegatives(classIndex);
						falseNegativesth = (float) evalThreshold.numFalseNegatives(classIndex);
						
						truePositives = (float) evalNoCost.numTruePositives(classIndex);
						falsePositives = (float) evalNoCost.numFalsePositives(classIndex);
						trueNegatives = (float) evalNoCost.numTrueNegatives(classIndex);
						falseNegatives = (float) evalNoCost.numFalseNegatives(classIndex);
						
						truePositivesln = (float) evalLearning.numTruePositives(classIndex);
						falsePositivesln = (float) evalLearning.numFalsePositives(classIndex);
						trueNegativesln = (float) evalLearning.numTrueNegatives(classIndex);
						falseNegativesln = (float) evalLearning.numFalseNegatives(classIndex);
						
						
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.NOSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.NOCOST.toString()+","+truePositives.toString()+","+falsePositives.toString()+","+trueNegatives.toString()+","+falseNegatives.toString()+","+evalNoCost.precision(classIndex)+","+evalNoCost.recall(classIndex)+","+evalNoCost.areaUnderROC(classIndex)+","+evalNoCost.kappa()+"\n");
						rw.flush();
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.NOSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSTHRESHOLD.toString()+","+truePositivesth.toString()+","+falsePositivesth.toString()+","+trueNegativesth.toString()+","+falseNegativesth.toString()+","+evalThreshold.precision(classIndex)+","+evalThreshold.recall(classIndex)+","+evalThreshold.areaUnderROC(classIndex)+","+evalThreshold.kappa()+"\n");
						rw.flush();
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.NOSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSLEARNING.toString()+","+truePositivesln.toString()+","+falsePositivesln.toString()+","+trueNegativesln.toString()+","+falseNegativesln.toString()+","+evalLearning.precision(classIndex)+","+evalLearning.recall(classIndex)+","+evalLearning.areaUnderROC(classIndex)+","+evalLearning.kappa()+"\n");
						rw.flush();
						break;
					case UNDERSAMPLING:
						 fcnb = Balancing.underSampling( classifier[j]);
						
						fcnb.buildClassifier(training);
						
						evalThreshold = CostSensitive.applyCostSensitive(fcnb,training,testing,true);
						evalLearning = CostSensitive.applyCostSensitive(fcnb,training,testing,false);	
						evalNoCost = new Evaluation(training);	
						
						evalThreshold.evaluateModel(fcnb, testing); 
						evalLearning.evaluateModel(fcnb, testing); 
						evalNoCost.evaluateModel(fcnb, testing); 
						
						truePositivesth = (float) evalThreshold.numTruePositives(classIndex);
						falsePositivesth = (float) evalThreshold.numFalsePositives(classIndex);
						trueNegativesth = (float) evalThreshold.numTrueNegatives(classIndex);
						falseNegativesth = (float) evalThreshold.numFalseNegatives(classIndex);
						
						truePositives = (float) evalNoCost.numTruePositives(classIndex);
						falsePositives = (float) evalNoCost.numFalsePositives(classIndex);
						trueNegatives = (float) evalNoCost.numTrueNegatives(classIndex);
						falseNegatives = (float) evalNoCost.numFalseNegatives(classIndex);
						
						truePositivesln = (float) evalLearning.numTruePositives(classIndex);
						falsePositivesln = (float) evalLearning.numFalsePositives(classIndex);
						trueNegativesln = (float) evalLearning.numTrueNegatives(classIndex);
						falseNegativesln = (float) evalLearning.numFalseNegatives(classIndex);
						
					
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.UNDERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.NOCOST.toString()+","+truePositives.toString()+","+falsePositives.toString()+","+trueNegatives.toString()+","+falseNegatives.toString()+","+evalNoCost.precision(classIndex)+","+evalNoCost.recall(classIndex)+","+evalNoCost.areaUnderROC(classIndex)+","+evalNoCost.kappa()+"\n");
						rw.flush();
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.UNDERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSTHRESHOLD.toString()+","+truePositivesth.toString()+","+falsePositivesth.toString()+","+trueNegativesth.toString()+","+falseNegativesth.toString()+","+evalThreshold.precision(classIndex)+","+evalThreshold.recall(classIndex)+","+evalThreshold.areaUnderROC(classIndex)+","+evalThreshold.kappa()+"\n");
						rw.flush();
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.UNDERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSLEARNING.toString()+","+truePositivesln.toString()+","+falsePositivesln.toString()+","+trueNegativesln.toString()+","+falseNegativesln.toString()+","+evalLearning.precision(classIndex)+","+evalLearning.recall(classIndex)+","+evalLearning.areaUnderROC(classIndex)+","+evalLearning.kappa()+"\n");
						rw.flush();
						break;
						
					case OVERSAMPLING:
						fcos = Balancing.overSampling( classifier[j],training);
						
						fcos.buildClassifier(training);
						
						evalThreshold = CostSensitive.applyCostSensitive(fcos,training,testing,true);
						evalLearning = CostSensitive.applyCostSensitive(fcos,training,testing,false);	
						evalNoCost = new Evaluation(training);	
						
						evalThreshold.evaluateModel(fcos, testing); 
						evalLearning.evaluateModel(fcos, testing); 
						evalNoCost.evaluateModel(fcos, testing); 
						
						truePositivesth = (float) evalThreshold.numTruePositives(classIndex);
						falsePositivesth = (float) evalThreshold.numFalsePositives(classIndex);
						trueNegativesth = (float) evalThreshold.numTrueNegatives(classIndex);
						falseNegativesth = (float) evalThreshold.numFalseNegatives(classIndex);
						
						truePositives = (float) evalNoCost.numTruePositives(classIndex);
						falsePositives = (float) evalNoCost.numFalsePositives(classIndex);
						trueNegatives = (float) evalNoCost.numTrueNegatives(classIndex);
						falseNegatives = (float) evalNoCost.numFalseNegatives(classIndex);
						
						truePositivesln = (float) evalLearning.numTruePositives(classIndex);
						falsePositivesln = (float) evalLearning.numFalsePositives(classIndex);
						trueNegativesln = (float) evalLearning.numTrueNegatives(classIndex);
						falseNegativesln = (float) evalLearning.numFalseNegatives(classIndex);
						
						
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.OVERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.NOCOST.toString()+","+truePositives.toString()+","+falsePositives.toString()+","+trueNegatives.toString()+","+falseNegatives.toString()+","+evalNoCost.precision(classIndex)+","+evalNoCost.recall(classIndex)+","+evalNoCost.areaUnderROC(classIndex)+","+evalNoCost.kappa()+"\n");
						rw.flush();
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.OVERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSTHRESHOLD.toString()+","+truePositivesth.toString()+","+falsePositivesth.toString()+","+trueNegativesth.toString()+","+falseNegativesth.toString()+","+evalThreshold.precision(classIndex)+","+evalThreshold.recall(classIndex)+","+evalThreshold.areaUnderROC(classIndex)+","+evalThreshold.kappa()+"\n");
						rw.flush();
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.OVERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSLEARNING.toString()+","+truePositivesln.toString()+","+falsePositivesln.toString()+","+trueNegativesln.toString()+","+falseNegativesln.toString()+","+evalLearning.precision(classIndex)+","+evalLearning.recall(classIndex)+","+evalLearning.areaUnderROC(classIndex)+","+evalLearning.kappa()+"\n");
						rw.flush();
						break;
						
					case SMOTE:
						
						fcsmote = Balancing.smote( classifier[j],training,numDefectiveInTraining,classIndex);
						
						fcsmote.buildClassifier(training);
						
						evalThreshold = CostSensitive.applyCostSensitive(fcsmote,training,testing,true);
						evalLearning = CostSensitive.applyCostSensitive(fcsmote,training,testing,false);	
						evalNoCost = new Evaluation(training);	
						
						evalThreshold.evaluateModel(fcsmote, testing); 
						evalLearning.evaluateModel(fcsmote, testing); 
						evalNoCost.evaluateModel(fcsmote, testing); 
						
						truePositivesth = (float) evalThreshold.numTruePositives(classIndex);
						falsePositivesth = (float) evalThreshold.numFalsePositives(classIndex);
						trueNegativesth = (float) evalThreshold.numTrueNegatives(classIndex);
						falseNegativesth = (float) evalThreshold.numFalseNegatives(classIndex);
						
						truePositives = (float) evalNoCost.numTruePositives(classIndex);
						falsePositives = (float) evalNoCost.numFalsePositives(classIndex);
						trueNegatives = (float) evalNoCost.numTrueNegatives(classIndex);
						falseNegatives = (float) evalNoCost.numFalseNegatives(classIndex);
						
						truePositivesln = (float) evalLearning.numTruePositives(classIndex);
						falsePositivesln = (float) evalLearning.numFalsePositives(classIndex);
						trueNegativesln = (float) evalLearning.numTrueNegatives(classIndex);
						falseNegativesln = (float) evalLearning.numFalseNegatives(classIndex);
						
						
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.SMOTE.toString()+","+featureSel+","+CostSensitiveEnum.NOCOST.toString()+","+truePositives.toString()+","+falsePositives.toString()+","+trueNegatives.toString()+","+falseNegatives.toString()+","+evalNoCost.precision(classIndex)+","+evalNoCost.recall(classIndex)+","+evalNoCost.areaUnderROC(classIndex)+","+evalNoCost.kappa()+"\n");
						rw.flush();
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.SMOTE.toString()+","+featureSel+","+CostSensitiveEnum.CSTHRESHOLD.toString()+","+truePositivesth.toString()+","+falsePositivesth.toString()+","+trueNegativesth.toString()+","+falseNegativesth.toString()+","+evalThreshold.precision(classIndex)+","+evalThreshold.recall(classIndex)+","+evalThreshold.areaUnderROC(classIndex)+","+evalThreshold.kappa()+"\n");
						rw.flush();
						rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.SMOTE.toString()+","+featureSel+","+CostSensitiveEnum.CSLEARNING.toString()+","+truePositivesln.toString()+","+falsePositivesln.toString()+","+trueNegativesln.toString()+","+falseNegativesln.toString()+","+evalLearning.precision(classIndex)+","+evalLearning.recall(classIndex)+","+evalLearning.areaUnderROC(classIndex)+","+evalLearning.kappa()+"\n");
						rw.flush();
						
						break;
						
					
				}
			}	
		}
		
		
	}
	
	public static String getNameClassifier(AbstractClassifier ac,Integer j) {
		String name = ac.toString();
		String[] nameC = name.split(" ");
		
		if(j.equals(1)) {
			String[] nameRF = nameC[0].split("\n");
			return nameRF[0];
		}else if(j.equals(0))
			return nameC[0]+nameC[1];
		else
			return nameC[0];
	}
	
	
	
	
	public static void main(String[] args) throws Exception {
		
		File file = new File(projects[0].toUpperCase()+"_Dataset.arff");
		List<Instances> wf = new ArrayList<>();
		ArffLoader loader = new ArffLoader();
		loader.setSource(file);
		Instances data = loader.getDataSet();//get instances object
	try(BufferedWriter rw = new BufferedWriter(new FileWriter(projects[0].toUpperCase()+"_Results_Weka"+".csv", true))){
			
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
			
		
			Integer classIndex = assignBuggyValue(wf.get(0));
			List<Instances> fs = FeatureSelection.applyFeatureSelection(wf.get(0),wf.get(1));
			
			valutationToCsv(wf.get(0), wf.get(1),classIndex,i, rw, "No_FS");
			valutationToCsv(fs.get(0), fs.get(1),classIndex,i, rw, "FS");
		
			
		}
	  }
	
		
	}
}
	
 


