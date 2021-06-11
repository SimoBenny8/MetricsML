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
	THRESHOLD,
	LEARNING
}


public class WalkForward {
	
	private static String[] columns = {"Dataset", "#TrainingRelease", "%Training", "%Defective in training", "%Defective in testing", "Classifier", "Balancing", "FeatureSelection", "Sensitivity", "TP", "FP", "TN", "FN", "Precision", "Recall", "AUC", "Kappa"};
	private static String[] projects = {"Bookkeeper","Storm"};
	private static Integer datasetSize;
	
	public static List<Instances> applyWalkForward(File projArff,Integer version,  Double maxVersion) throws IOException {
		
		List<Instances> result = new ArrayList<>();
		
		ArffLoader loader = new ArffLoader();
		loader.setSource(projArff);
		Instances data = loader.getDataSet();//get instances object
		datasetSize = data.size();
		
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
	try(BufferedWriter rw = new BufferedWriter(new FileWriter(projects[0]+"_Results_Weka"+".csv", true))){
			
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
			
			
			Integer numDefectiveInTraining = 0;
			Integer numDefectiveInTesting = 0;
			
			
			for(FeatureSelectionEnum feature: FeatureSelectionEnum.values()) {
				if(feature.equals(FeatureSelectionEnum.FEATURE)) {
					List<Instances> fs = FeatureSelection.applyFeatureSelection(wf.get(0),wf.get(1));
					Integer classIndex = assignBuggyValue(fs.get(0));
					for(Instance instance: fs.get(0)) {
						if (instance.value(instance.numAttributes() - 1) == classIndex) {
							numDefectiveInTraining++;
						}
					}
					
					for(Instance instance: fs.get(1)) {
						if (instance.value(instance.numAttributes() - 1) == classIndex) {
							numDefectiveInTesting++;
						}
					}
						for(Sampling samp: Sampling.values()) {
							Evaluation evalnb;
							Evaluation evalrf;
							Evaluation evalibk;
							
							Float truePositivesnb;
							Float falsePositivesnb;
							Float trueNegativesnb;
							Float falseNegativesnb;
							
							Float truePositivesrf;
							Float falsePositivesrf;
							Float trueNegativesrf;
							Float falseNegativesrf;
							
							Float truePositivesibk;
							Float falsePositivesibk;
							Float trueNegativesibk;
							Float falseNegativesibk;
							switch(samp) {
								case NOSAMPLING:
									for(CostSensitiveEnum cse :  CostSensitiveEnum.values()) {
										switch(cse) {
											case NOCOST:
												MetricsWeka mw = new MetricsWeka();
												mw.setDatasetSize(datasetSize);
												mw.setPercentualTraining((float) fs.get(0).numInstances());
												mw.setNumTrainingVersions(i);
												mw.setPercDefectiveInTraining(numDefectiveInTraining);
												mw.setPercDefectiveInTesting(numDefectiveInTesting);
												
												int numAttributes = fs.get(0).numAttributes();
												fs.get(0).setClassIndex(numAttributes - 1);
												fs.get(1).setClassIndex(numAttributes - 1);
												
												
												NaiveBayes nbClassifier = new NaiveBayes();
												RandomForest rfClassifier = new RandomForest();
												IBk ibkClassifier = new IBk();

												nbClassifier.buildClassifier(fs.get(0));
												rfClassifier.buildClassifier(fs.get(0));
												ibkClassifier.buildClassifier(fs.get(0));
												
												evalnb = new Evaluation(fs.get(1));
												evalrf = new Evaluation(fs.get(1));	
												evalibk = new Evaluation(fs.get(1));	
												
												evalnb.evaluateModel(nbClassifier, fs.get(1)); 
												evalrf.evaluateModel(rfClassifier, fs.get(1)); 
												evalibk.evaluateModel(ibkClassifier, fs.get(1));
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
												
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"No_Balancing"+","+"FS"+","+"No_Cost"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"No_Balancing"+","+"FS"+","+"No_Cost"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"IBk"+","+"No_Balancing"+","+"FS"+","+"No_Cost"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;
											case THRESHOLD:
												MetricsWeka mw2 = new MetricsWeka();
												mw2.setDatasetSize(datasetSize);
												mw2.setPercentualTraining((float) fs.get(0).numInstances());
												mw2.setNumTrainingVersions(i);
												mw2.setPercDefectiveInTraining(numDefectiveInTraining);
												mw2.setPercDefectiveInTesting(numDefectiveInTesting);
												
												evalnb = CostSensitive.applyCostSensitive(new NaiveBayes(),fs.get(0),fs.get(1),true);
												evalrf = CostSensitive.applyCostSensitive(new RandomForest(),fs.get(0),fs.get(1),true);	
												evalibk = CostSensitive.applyCostSensitive(new IBk(),fs.get(0),fs.get(1),true);	
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"No_Balancing"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"No_Balancing"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"IBk"+","+"No_Balancing"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;
											case LEARNING:
												MetricsWeka mw3 = new MetricsWeka();
												mw3.setDatasetSize(datasetSize);
												mw3.setPercentualTraining((float) fs.get(0).numInstances());
												mw3.setNumTrainingVersions(i);
												mw3.setPercDefectiveInTraining(numDefectiveInTraining);
												mw3.setPercDefectiveInTesting(numDefectiveInTesting);
												
												evalnb = CostSensitive.applyCostSensitive(new NaiveBayes(),fs.get(0),fs.get(1),false);
												evalrf = CostSensitive.applyCostSensitive(new RandomForest(),fs.get(0),fs.get(1),false);	
												evalibk = CostSensitive.applyCostSensitive(new IBk(),fs.get(0),fs.get(1),false);	
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"No_Balancing"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"No_Balancing"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"IBk"+","+"No_Balancing"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;
										}
									}
									
									break;
								case UNDERSAMPLING:
									
									FilteredClassifier fcnb = Balancing.underSampling( new NaiveBayes());
									FilteredClassifier fcrf = Balancing.underSampling( new RandomForest());
									FilteredClassifier fcibk = Balancing.underSampling( new IBk());
									
									for(CostSensitiveEnum cse :  CostSensitiveEnum.values()) {
										switch(cse) {
											case NOCOST:
												MetricsWeka mw = new MetricsWeka();
												mw.setDatasetSize(datasetSize);
												mw.setPercentualTraining((float) fs.get(0).numInstances());
												mw.setNumTrainingVersions(i);
												mw.setPercDefectiveInTraining(numDefectiveInTraining);
												mw.setPercDefectiveInTesting(numDefectiveInTesting);
												
												int numAttributes = fs.get(0).numAttributes();
												fs.get(0).setClassIndex(numAttributes - 1);
												fs.get(1).setClassIndex(numAttributes - 1);
												
												fcnb.buildClassifier(fs.get(0));
												fcrf.buildClassifier(fs.get(0));
												fcibk.buildClassifier(fs.get(0));
												
												evalnb = new Evaluation(fs.get(1));
												evalrf = new Evaluation(fs.get(1));	
												evalibk = new Evaluation(fs.get(1));	
												
												evalnb.evaluateModel(fcnb, fs.get(1)); 
												evalrf.evaluateModel(fcrf, fs.get(1)); 
												evalibk.evaluateModel(fcibk, fs.get(1));
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
												
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"UnderSampling"+","+"FS"+","+"No_Cost"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"UnderSampling"+","+"FS"+","+"No_Cost"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"IBk"+","+"UnderSampling"+","+"FS"+","+"No_Cost"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;
												
											case THRESHOLD:
												MetricsWeka mw2 = new MetricsWeka();
												mw2.setDatasetSize(datasetSize);
												mw2.setPercentualTraining((float) fs.get(0).numInstances());
												mw2.setNumTrainingVersions(i);
												mw2.setPercDefectiveInTraining(numDefectiveInTraining);
												mw2.setPercDefectiveInTesting(numDefectiveInTesting);
												
												evalnb = CostSensitive.applyCostSensitive(fcnb,fs.get(0),fs.get(1),true);
												evalrf = CostSensitive.applyCostSensitive(fcrf,fs.get(0),fs.get(1),true);	
												evalibk = CostSensitive.applyCostSensitive(fcibk,fs.get(0),fs.get(1),true);	
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"UnderSampling"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"UnderSampling"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"IBk"+","+"UnderSampling"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;

											case LEARNING:
												MetricsWeka mw3 = new MetricsWeka();
												mw3.setDatasetSize(datasetSize);
												mw3.setPercentualTraining((float) fs.get(0).numInstances());
												mw3.setNumTrainingVersions(i);
												mw3.setPercDefectiveInTraining(numDefectiveInTraining);
												mw3.setPercDefectiveInTesting(numDefectiveInTesting);
												
												evalnb = CostSensitive.applyCostSensitive(fcnb,fs.get(0),fs.get(1),false);
												evalrf = CostSensitive.applyCostSensitive(fcrf,fs.get(0),fs.get(1),false);	
												evalibk = CostSensitive.applyCostSensitive(fcibk,fs.get(0),fs.get(1),false);	
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"UnderSampling"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"UnderSampling"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"IBk"+","+"UnderSampling"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;
										}
									}
									break;
								case OVERSAMPLING:
									FilteredClassifier fcnb2 = Balancing.overSampling( new NaiveBayes(),fs.get(0));
									FilteredClassifier fcrf2 = Balancing.overSampling( new RandomForest(),fs.get(0));
									FilteredClassifier fcibk2 = Balancing.overSampling( new IBk(),fs.get(0));
									for(CostSensitiveEnum cse :  CostSensitiveEnum.values()) {
										switch(cse) {
										case NOCOST:
											MetricsWeka mw = new MetricsWeka();
											mw.setDatasetSize(datasetSize);
											mw.setPercentualTraining((float) fs.get(0).numInstances());
											mw.setNumTrainingVersions(i);
											mw.setPercDefectiveInTraining(numDefectiveInTraining);
											mw.setPercDefectiveInTesting(numDefectiveInTesting);
											
											int numAttributes = fs.get(0).numAttributes();
											fs.get(0).setClassIndex(numAttributes - 1);
											fs.get(1).setClassIndex(numAttributes - 1);
											
											fcnb2.buildClassifier(fs.get(0));
											fcrf2.buildClassifier(fs.get(0));
											fcibk2.buildClassifier(fs.get(0));
											
											evalnb = new Evaluation(fs.get(1));
											evalrf = new Evaluation(fs.get(1));	
											evalibk = new Evaluation(fs.get(1));	
											
											evalnb.evaluateModel(fcnb2, fs.get(1)); 
											evalrf.evaluateModel(fcrf2, fs.get(1)); 
											evalibk.evaluateModel(fcibk2, fs.get(1));
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"OverSampling"+","+"FS"+","+"No_Cost"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"OverSampling"+","+"FS"+","+"No_Cost"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"IBk"+","+"OverSampling"+","+"FS"+","+"No_Cost"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;
											
										case THRESHOLD:
											MetricsWeka mw2 = new MetricsWeka();
											mw2.setDatasetSize(datasetSize);
											mw2.setPercentualTraining((float) fs.get(0).numInstances());
											mw2.setNumTrainingVersions(i);
											mw2.setPercDefectiveInTraining(numDefectiveInTraining);
											mw2.setPercDefectiveInTesting(numDefectiveInTesting);
											
											evalnb = CostSensitive.applyCostSensitive(fcnb2,fs.get(0),fs.get(1),true);
											evalrf = CostSensitive.applyCostSensitive(fcrf2,fs.get(0),fs.get(1),true);	
											evalibk = CostSensitive.applyCostSensitive(fcibk2,fs.get(0),fs.get(1),true);	
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
										
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"OverSampling"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"OverSampling"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"IBk"+","+"OverSampling"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;

										case LEARNING:
											MetricsWeka mw3 = new MetricsWeka();
											mw3.setDatasetSize(datasetSize);
											mw3.setPercentualTraining((float) fs.get(0).numInstances());
											mw3.setNumTrainingVersions(i);
											mw3.setPercDefectiveInTraining(numDefectiveInTraining);
											mw3.setPercDefectiveInTesting(numDefectiveInTesting);
											
											evalnb = CostSensitive.applyCostSensitive(fcnb2,fs.get(0),fs.get(1),false);
											evalrf = CostSensitive.applyCostSensitive(fcrf2,fs.get(0),fs.get(1),false);	
											evalibk = CostSensitive.applyCostSensitive(fcibk2,fs.get(0),fs.get(1),false);	
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
										
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"OverSampling"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"OverSampling"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"IBk"+","+"OverSampling"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;
									}
									}
									break;
								case SMOTE:
									FilteredClassifier fcnb3 = Balancing.smote( new NaiveBayes(),fs.get(0),numDefectiveInTraining);
									FilteredClassifier fcrf3 = Balancing.smote( new RandomForest(),fs.get(0),numDefectiveInTraining);
									FilteredClassifier fcibk3 = Balancing.smote( new IBk(),fs.get(0),numDefectiveInTraining);
									for(CostSensitiveEnum cse :  CostSensitiveEnum.values()) {
										switch(cse) {
										case NOCOST:
											MetricsWeka mw = new MetricsWeka();
											mw.setDatasetSize(datasetSize);
											mw.setPercentualTraining((float) fs.get(0).numInstances());
											mw.setNumTrainingVersions(i);
											mw.setPercDefectiveInTraining(numDefectiveInTraining);
											mw.setPercDefectiveInTesting(numDefectiveInTesting);
											
											int numAttributes = fs.get(0).numAttributes();
											fs.get(0).setClassIndex(numAttributes - 1);
											fs.get(1).setClassIndex(numAttributes - 1);
											
											fcnb3.buildClassifier(fs.get(0));
											fcrf3.buildClassifier(fs.get(0));
											fcibk3.buildClassifier(fs.get(0));
											
											evalnb = new Evaluation(fs.get(1));
											evalrf = new Evaluation(fs.get(1));	
											evalibk = new Evaluation(fs.get(1));	
											
											evalnb.evaluateModel(fcnb3, fs.get(1)); 
											evalrf.evaluateModel(fcrf3, fs.get(1)); 
											evalibk.evaluateModel(fcibk3, fs.get(1));
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"Smote"+","+"FS"+","+"No_Cost"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"Smote"+","+"FS"+","+"No_Cost"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"IBk"+","+"Smote"+","+"FS"+","+"No_Cost"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;
											
										case THRESHOLD:
											MetricsWeka mw2 = new MetricsWeka();
											mw2.setDatasetSize(datasetSize);
											mw2.setPercentualTraining((float) fs.get(0).numInstances());
											mw2.setNumTrainingVersions(i);
											mw2.setPercDefectiveInTraining(numDefectiveInTraining);
											mw2.setPercDefectiveInTesting(numDefectiveInTesting);
											
											evalnb = CostSensitive.applyCostSensitive(fcnb3,fs.get(0),fs.get(1),true);
											evalrf = CostSensitive.applyCostSensitive(fcrf3,fs.get(0),fs.get(1),true);	
											evalibk = CostSensitive.applyCostSensitive(fcibk3,fs.get(0),fs.get(1),true);	
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
										
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"Smote"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"Smote"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"IBk"+","+"Smote"+","+"FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;

										case LEARNING:
											MetricsWeka mw3 = new MetricsWeka();
											mw3.setDatasetSize(datasetSize);
											mw3.setPercentualTraining((float) fs.get(0).numInstances());
											mw3.setNumTrainingVersions(i);
											mw3.setPercDefectiveInTraining(numDefectiveInTraining);
											mw3.setPercDefectiveInTesting(numDefectiveInTesting);
											
											evalnb = CostSensitive.applyCostSensitive(fcnb3,fs.get(0),fs.get(1),false);
											evalrf = CostSensitive.applyCostSensitive(fcrf3,fs.get(0),fs.get(1),false);	
											evalibk = CostSensitive.applyCostSensitive(fcibk3,fs.get(0),fs.get(1),false);	
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
										
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"Smote"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"Smote"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"IBk"+","+"Smote"+","+"FS"+","+"Cost_Sensitive_Learning"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;
										}
									}
									
									
									break;
							}
						}
						
					
				}else {
					Integer classIndex = assignBuggyValue(wf.get(0));
					for(Instance instance: wf.get(0)) {
						if (instance.value(instance.numAttributes() - 1) == classIndex) {
							numDefectiveInTraining++;
						}
					}
					
					for(Instance instance: wf.get(1)) {
						if (instance.value(instance.numAttributes() - 1) == classIndex) {
							numDefectiveInTesting++;
						}
					}
						for(Sampling samp: Sampling.values()) {
							Evaluation evalnb;
							Evaluation evalrf;
							Evaluation evalibk;
							
							Float truePositivesnb;
							Float falsePositivesnb;
							Float trueNegativesnb;
							Float falseNegativesnb;
							
							Float truePositivesrf;
							Float falsePositivesrf;
							Float trueNegativesrf;
							Float falseNegativesrf;
							
							Float truePositivesibk;
							Float falsePositivesibk;
							Float trueNegativesibk;
							Float falseNegativesibk;
							switch(samp) {
								case NOSAMPLING:
									for(CostSensitiveEnum cse :  CostSensitiveEnum.values()) {
										switch(cse) {
											case NOCOST:
												MetricsWeka mw = new MetricsWeka();
												mw.setDatasetSize(datasetSize);
												mw.setPercentualTraining((float) wf.get(0).numInstances());
												mw.setNumTrainingVersions(i);
												mw.setPercDefectiveInTraining(numDefectiveInTraining);
												mw.setPercDefectiveInTesting(numDefectiveInTesting);
												
												int numAttributes = wf.get(0).numAttributes();
												wf.get(0).setClassIndex(numAttributes - 1);
												wf.get(1).setClassIndex(numAttributes - 1);
												
												
												NaiveBayes nbClassifier = new NaiveBayes();
												RandomForest rfClassifier = new RandomForest();
												IBk ibkClassifier = new IBk();

												nbClassifier.buildClassifier(wf.get(0));
												rfClassifier.buildClassifier(wf.get(0));
												ibkClassifier.buildClassifier(wf.get(0));
												
												evalnb = new Evaluation(wf.get(1));
												evalrf = new Evaluation(wf.get(1));	
												evalibk = new Evaluation(wf.get(1));	
												
												evalnb.evaluateModel(nbClassifier, wf.get(1)); 
												evalrf.evaluateModel(rfClassifier, wf.get(1)); 
												evalibk.evaluateModel(ibkClassifier, wf.get(1));
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
												
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"No_Balancing"+","+"No_FS"+","+"No_Cost"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"No_Balancing"+","+"No_FS"+","+"No_Cost"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"IBk"+","+"No_Balancing"+","+"No_FS"+","+"No_Cost"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;
											case THRESHOLD:
												MetricsWeka mw2 = new MetricsWeka();
												mw2.setDatasetSize(datasetSize);
												mw2.setPercentualTraining((float) wf.get(0).numInstances());
												mw2.setNumTrainingVersions(i);
												mw2.setPercDefectiveInTraining(numDefectiveInTraining);
												mw2.setPercDefectiveInTesting(numDefectiveInTesting);
												
												evalnb = CostSensitive.applyCostSensitive(new NaiveBayes(),wf.get(0),wf.get(1),true);
												evalrf = CostSensitive.applyCostSensitive(new RandomForest(),wf.get(0),wf.get(1),true);	
												evalibk = CostSensitive.applyCostSensitive(new IBk(),wf.get(0),wf.get(1),true);	
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"No_Balancing"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"No_Balancing"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"IBk"+","+"No_Balancing"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;
											case LEARNING:
												MetricsWeka mw3 = new MetricsWeka();
												mw3.setDatasetSize(datasetSize);
												mw3.setPercentualTraining((float) wf.get(0).numInstances());
												mw3.setNumTrainingVersions(i);
												mw3.setPercDefectiveInTraining(numDefectiveInTraining);
												mw3.setPercDefectiveInTesting(numDefectiveInTesting);
												
												evalnb = CostSensitive.applyCostSensitive(new NaiveBayes(),wf.get(0),wf.get(1),false);
												evalrf = CostSensitive.applyCostSensitive(new RandomForest(),wf.get(0),wf.get(1),false);	
												evalibk = CostSensitive.applyCostSensitive(new IBk(),wf.get(0),wf.get(1),false);	
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"No_Balancing"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"No_Balancing"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"IBk"+","+"No_Balancing"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;
										}
									}
									
									break;
								case UNDERSAMPLING:
									
									FilteredClassifier fcnb = Balancing.underSampling( new NaiveBayes());
									FilteredClassifier fcrf = Balancing.underSampling( new RandomForest());
									FilteredClassifier fcibk = Balancing.underSampling( new IBk());
									
									for(CostSensitiveEnum cse :  CostSensitiveEnum.values()) {
										switch(cse) {
											case NOCOST:
												MetricsWeka mw = new MetricsWeka();
												mw.setDatasetSize(datasetSize);
												mw.setPercentualTraining((float) wf.get(0).numInstances());
												mw.setNumTrainingVersions(i);
												mw.setPercDefectiveInTraining(numDefectiveInTraining);
												mw.setPercDefectiveInTesting(numDefectiveInTesting);
												
												int numAttributes = wf.get(0).numAttributes();
												wf.get(0).setClassIndex(numAttributes - 1);
												wf.get(1).setClassIndex(numAttributes - 1);
												
												fcnb.buildClassifier(wf.get(0));
												fcrf.buildClassifier(wf.get(0));
												fcibk.buildClassifier(wf.get(0));
												
												evalnb = new Evaluation(wf.get(1));
												evalrf = new Evaluation(wf.get(1));	
												evalibk = new Evaluation(wf.get(1));	
												
												evalnb.evaluateModel(fcnb, wf.get(1)); 
												evalrf.evaluateModel(fcrf, wf.get(1)); 
												evalibk.evaluateModel(fcibk, wf.get(1));
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
												
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"UnderSampling"+","+"No_FS"+","+"No_Cost"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"UnderSampling"+","+"No_FS"+","+"No_Cost"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"IBk"+","+"UnderSampling"+","+"No_FS"+","+"No_Cost"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;
												
											case THRESHOLD:
												MetricsWeka mw2 = new MetricsWeka();
												mw2.setDatasetSize(datasetSize);
												mw2.setPercentualTraining((float) wf.get(0).numInstances());
												mw2.setNumTrainingVersions(i);
												mw2.setPercDefectiveInTraining(numDefectiveInTraining);
												mw2.setPercDefectiveInTesting(numDefectiveInTesting);
												
												evalnb = CostSensitive.applyCostSensitive(fcnb,wf.get(0),wf.get(1),true);
												evalrf = CostSensitive.applyCostSensitive(fcrf,wf.get(0),wf.get(1),true);	
												evalibk = CostSensitive.applyCostSensitive(fcibk,wf.get(0),wf.get(1),true);	
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"UnderSampling"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"UnderSampling"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"IBk"+","+"UnderSampling"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;

											case LEARNING:
												MetricsWeka mw3 = new MetricsWeka();
												mw3.setDatasetSize(datasetSize);
												mw3.setPercentualTraining((float) wf.get(0).numInstances());
												mw3.setNumTrainingVersions(i);
												mw3.setPercDefectiveInTraining(numDefectiveInTraining);
												mw3.setPercDefectiveInTesting(numDefectiveInTesting);
												
												evalnb = CostSensitive.applyCostSensitive(fcnb,wf.get(0),wf.get(1),false);
												evalrf = CostSensitive.applyCostSensitive(fcrf,wf.get(0),wf.get(1),false);	
												evalibk = CostSensitive.applyCostSensitive(fcibk,wf.get(0),wf.get(1),false);	
												
												truePositivesnb = (float) evalnb.numTruePositives(classIndex);
												falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
												trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
												falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
												
												truePositivesrf = (float) evalrf.numTruePositives(classIndex);
												falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
												trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
												falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
												
												truePositivesibk = (float) evalibk.numTruePositives(classIndex);
												falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
												trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
												falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"UnderSampling"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"UnderSampling"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
												rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"IBk"+","+"UnderSampling"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
												break;
										}
									}
									break;
								case OVERSAMPLING:
									FilteredClassifier fcnb2 = Balancing.overSampling( new NaiveBayes(),wf.get(0));
									FilteredClassifier fcrf2 = Balancing.overSampling( new RandomForest(),wf.get(0));
									FilteredClassifier fcibk2 = Balancing.overSampling( new IBk(),wf.get(0));
									for(CostSensitiveEnum cse :  CostSensitiveEnum.values()) {
										switch(cse) {
										case NOCOST:
											MetricsWeka mw = new MetricsWeka();
											mw.setDatasetSize(datasetSize);
											mw.setPercentualTraining((float) wf.get(0).numInstances());
											mw.setNumTrainingVersions(i);
											mw.setPercDefectiveInTraining(numDefectiveInTraining);
											mw.setPercDefectiveInTesting(numDefectiveInTesting);
											
											int numAttributes = wf.get(0).numAttributes();
											wf.get(0).setClassIndex(numAttributes - 1);
											wf.get(1).setClassIndex(numAttributes - 1);
											
											fcnb2.buildClassifier(wf.get(0));
											fcrf2.buildClassifier(wf.get(0));
											fcibk2.buildClassifier(wf.get(0));
											
											evalnb = new Evaluation(wf.get(1));
											evalrf = new Evaluation(wf.get(1));	
											evalibk = new Evaluation(wf.get(1));	
											
											evalnb.evaluateModel(fcnb2, wf.get(1)); 
											evalrf.evaluateModel(fcrf2, wf.get(1)); 
											evalibk.evaluateModel(fcibk2, wf.get(1));
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"OverSampling"+","+"No_FS"+","+"No_Cost"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"OverSampling"+","+"No_FS"+","+"No_Cost"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"IBk"+","+"OverSampling"+","+"No_FS"+","+"No_Cost"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;
											
										case THRESHOLD:
											MetricsWeka mw2 = new MetricsWeka();
											mw2.setDatasetSize(datasetSize);
											mw2.setPercentualTraining((float) wf.get(0).numInstances());
											mw2.setNumTrainingVersions(i);
											mw2.setPercDefectiveInTraining(numDefectiveInTraining);
											mw2.setPercDefectiveInTesting(numDefectiveInTesting);
											
											evalnb = CostSensitive.applyCostSensitive(fcnb2,wf.get(0),wf.get(1),true);
											evalrf = CostSensitive.applyCostSensitive(fcrf2,wf.get(0),wf.get(1),true);	
											evalibk = CostSensitive.applyCostSensitive(fcibk2,wf.get(0),wf.get(1),true);	
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
										
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"OverSampling"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"OverSampling"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"IBk"+","+"OverSampling"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;

										case LEARNING:
											MetricsWeka mw3 = new MetricsWeka();
											mw3.setDatasetSize(datasetSize);
											mw3.setPercentualTraining((float) wf.get(0).numInstances());
											mw3.setNumTrainingVersions(i);
											mw3.setPercDefectiveInTraining(numDefectiveInTraining);
											mw3.setPercDefectiveInTesting(numDefectiveInTesting);
											
											evalnb = CostSensitive.applyCostSensitive(fcnb2,wf.get(0),wf.get(1),false);
											evalrf = CostSensitive.applyCostSensitive(fcrf2,wf.get(0),wf.get(1),false);	
											evalibk = CostSensitive.applyCostSensitive(fcibk2,wf.get(0),wf.get(1),false);	
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
										
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"OverSampling"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"OverSampling"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"IBk"+","+"OverSampling"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;
									}
									}
									break;
								case SMOTE:
									FilteredClassifier fcnb3 = Balancing.smote( new NaiveBayes(),wf.get(0),numDefectiveInTraining);
									FilteredClassifier fcrf3 = Balancing.smote( new RandomForest(),wf.get(0),numDefectiveInTraining);
									FilteredClassifier fcibk3 = Balancing.smote( new IBk(),wf.get(0),numDefectiveInTraining);
									for(CostSensitiveEnum cse :  CostSensitiveEnum.values()) {
										switch(cse) {
										case NOCOST:
											MetricsWeka mw = new MetricsWeka();
											mw.setDatasetSize(datasetSize);
											mw.setPercentualTraining((float) wf.get(0).numInstances());
											mw.setNumTrainingVersions(i);
											mw.setPercDefectiveInTraining(numDefectiveInTraining);
											mw.setPercDefectiveInTesting(numDefectiveInTesting);
											
											int numAttributes = wf.get(0).numAttributes();
											wf.get(0).setClassIndex(numAttributes - 1);
											wf.get(1).setClassIndex(numAttributes - 1);
											
											fcnb3.buildClassifier(wf.get(0));
											fcrf3.buildClassifier(wf.get(0));
											fcibk3.buildClassifier(wf.get(0));
											
											evalnb = new Evaluation(wf.get(1));
											evalrf = new Evaluation(wf.get(1));	
											evalibk = new Evaluation(wf.get(1));	
											
											evalnb.evaluateModel(fcnb3, wf.get(1)); 
											evalrf.evaluateModel(fcrf3, wf.get(1)); 
											evalibk.evaluateModel(fcibk3, wf.get(1));
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
											
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"Smote"+","+"No_FS"+","+"No_Cost"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"Smote"+","+"No_FS"+","+"No_Cost"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+"IBk"+","+"Smote"+","+"No_FS"+","+"No_Cost"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;
											
										case THRESHOLD:
											MetricsWeka mw2 = new MetricsWeka();
											mw2.setDatasetSize(datasetSize);
											mw2.setPercentualTraining((float) wf.get(0).numInstances());
											mw2.setNumTrainingVersions(i);
											mw2.setPercDefectiveInTraining(numDefectiveInTraining);
											mw2.setPercDefectiveInTesting(numDefectiveInTesting);
											
											evalnb = CostSensitive.applyCostSensitive(fcnb3,wf.get(0),wf.get(1),true);
											evalrf = CostSensitive.applyCostSensitive(fcrf3,wf.get(0),wf.get(1),true);	
											evalibk = CostSensitive.applyCostSensitive(fcibk3,wf.get(0),wf.get(1),true);	
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
										
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"Smote"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"Smote"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw2.getPercentualTraining().toString()+","+mw2.getPercDefectiveInTraining().toString()+","+mw2.getPercDefectiveInTesting().toString()+","+"IBk"+","+"Smote"+","+"No_FS"+","+"Cost_Sensitive_Threshold"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;

										case LEARNING:
											MetricsWeka mw3 = new MetricsWeka();
											mw3.setDatasetSize(datasetSize);
											mw3.setPercentualTraining((float) wf.get(0).numInstances());
											mw3.setNumTrainingVersions(i);
											mw3.setPercDefectiveInTraining(numDefectiveInTraining);
											mw3.setPercDefectiveInTesting(numDefectiveInTesting);
											
											evalnb = CostSensitive.applyCostSensitive(fcnb3,wf.get(0),wf.get(1),false);
											evalrf = CostSensitive.applyCostSensitive(fcrf3,wf.get(0),wf.get(1),false);	
											evalibk = CostSensitive.applyCostSensitive(fcibk3,wf.get(0),wf.get(1),false);	
											
											truePositivesnb = (float) evalnb.numTruePositives(classIndex);
											falsePositivesnb = (float) evalnb.numFalsePositives(classIndex);
											trueNegativesnb = (float) evalnb.numTrueNegatives(classIndex);
											falseNegativesnb = (float) evalnb.numFalseNegatives(classIndex);
											
											truePositivesrf = (float) evalrf.numTruePositives(classIndex);
											falsePositivesrf = (float) evalrf.numFalsePositives(classIndex);
											trueNegativesrf = (float) evalrf.numTrueNegatives(classIndex);
											falseNegativesrf = (float) evalrf.numFalseNegatives(classIndex);
											
											truePositivesibk = (float) evalibk.numTruePositives(classIndex);
											falsePositivesibk = (float) evalibk.numFalsePositives(classIndex);
											trueNegativesibk = (float) evalibk.numTrueNegatives(classIndex);
											falseNegativesibk = (float) evalibk.numFalseNegatives(classIndex);
										
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"NaiveBayes"+","+"Smote"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesnb.toString()+","+falsePositivesnb.toString()+","+trueNegativesnb.toString()+","+falseNegativesnb.toString()+","+evalnb.precision(classIndex)+","+evalnb.recall(classIndex)+","+evalnb.areaUnderROC(classIndex)+","+evalnb.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"RandomForest"+","+"Smote"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesrf.toString()+","+falsePositivesrf.toString()+","+trueNegativesrf.toString()+","+falseNegativesrf.toString()+","+evalrf.precision(classIndex)+","+evalrf.recall(classIndex)+","+evalrf.areaUnderROC(classIndex)+","+evalrf.kappa()+"\n");
											rw.append(projects[0].toUpperCase()+","+i.toString()+","+mw3.getPercentualTraining().toString()+","+mw3.getPercDefectiveInTraining().toString()+","+mw3.getPercDefectiveInTesting().toString()+","+"IBk"+","+"Smote"+","+"No_FS"+","+"Cost_Sensitive_Learning"+","+truePositivesibk.toString()+","+falsePositivesibk.toString()+","+trueNegativesibk.toString()+","+falseNegativesibk.toString()+","+evalibk.precision(classIndex)+","+evalibk.recall(classIndex)+","+evalibk.areaUnderROC(classIndex)+","+evalibk.kappa()+"\n");
											break;
										}
									}
									
									
									break;
							}
						}
				}	
			
			}
		}
	}
	}
}
	
 


