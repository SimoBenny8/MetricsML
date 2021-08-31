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
	private static String[] projects = {"Storm"};
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
	
	public static synchronized void valutationToCsv(Integer k,Instances training, Instances testing,Integer classIndex,Integer i,BufferedWriter rw, String featureSel){
		
	try {	
		
		for(Sampling samp: Sampling.values()) {
			
			for(int j = 0; j <classifier.length; j++) {
				Evaluation evalThreshold;
				Evaluation evalLearning;
				Evaluation evalNoCost;
	
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
						
						Float truePositivesthNs = (float) evalThreshold.numTruePositives(classIndex);
						Float falsePositivesthNs = (float) evalThreshold.numFalsePositives(classIndex);
						Float trueNegativesthNs = (float) evalThreshold.numTrueNegatives(classIndex);
						Float falseNegativesthNs = (float) evalThreshold.numFalseNegatives(classIndex);
						
						Float truePositivesNs = (float) evalNoCost.numTruePositives(classIndex);
						Float falsePositivesNs = (float) evalNoCost.numFalsePositives(classIndex);
						Float trueNegativesNs = (float) evalNoCost.numTrueNegatives(classIndex);
						Float falseNegativesNs = (float) evalNoCost.numFalseNegatives(classIndex);
						
						Float truePositiveslnNs = (float) evalLearning.numTruePositives(classIndex);
						Float falsePositiveslnNs = (float) evalLearning.numFalsePositives(classIndex);
						Float trueNegativeslnNs = (float) evalLearning.numTrueNegatives(classIndex);
						Float falseNegativeslnNs = (float) evalLearning.numFalseNegatives(classIndex);
						
						
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.NOSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.NOCOST.toString()+","+truePositivesNs.toString()+","+falsePositivesNs.toString()+","+trueNegativesNs.toString()+","+falseNegativesNs.toString()+","+evalNoCost.precision(classIndex)+","+evalNoCost.recall(classIndex)+","+evalNoCost.areaUnderROC(classIndex)+","+evalNoCost.kappa()+"\n");
						rw.flush();
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.NOSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSTHRESHOLD.toString()+","+truePositivesthNs.toString()+","+falsePositivesthNs.toString()+","+trueNegativesthNs.toString()+","+falseNegativesthNs.toString()+","+evalThreshold.precision(classIndex)+","+evalThreshold.recall(classIndex)+","+evalThreshold.areaUnderROC(classIndex)+","+evalThreshold.kappa()+"\n");
						rw.flush();
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.NOSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSLEARNING.toString()+","+truePositiveslnNs.toString()+","+falsePositiveslnNs.toString()+","+trueNegativeslnNs.toString()+","+falseNegativeslnNs.toString()+","+evalLearning.precision(classIndex)+","+evalLearning.recall(classIndex)+","+evalLearning.areaUnderROC(classIndex)+","+evalLearning.kappa()+"\n");
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
						
						Float truePositivesthUs = (float) evalThreshold.numTruePositives(classIndex);
						Float falsePositivesthUs = (float) evalThreshold.numFalsePositives(classIndex);
						Float trueNegativesthUs = (float) evalThreshold.numTrueNegatives(classIndex);
						Float falseNegativesthUs = (float) evalThreshold.numFalseNegatives(classIndex);
						
						Float truePositivesUs = (float) evalNoCost.numTruePositives(classIndex);
						Float falsePositivesUs = (float) evalNoCost.numFalsePositives(classIndex);
						Float trueNegativesUs = (float) evalNoCost.numTrueNegatives(classIndex);
						Float falseNegativesUs = (float) evalNoCost.numFalseNegatives(classIndex);
						
						Float truePositiveslnUs = (float) evalLearning.numTruePositives(classIndex);
						Float falsePositiveslnUs = (float) evalLearning.numFalsePositives(classIndex);
						Float trueNegativeslnUs = (float) evalLearning.numTrueNegatives(classIndex);
						Float falseNegativeslnUs = (float) evalLearning.numFalseNegatives(classIndex);
						
					
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.UNDERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.NOCOST.toString()+","+truePositivesUs.toString()+","+falsePositivesUs.toString()+","+trueNegativesUs.toString()+","+falseNegativesUs.toString()+","+evalNoCost.precision(classIndex)+","+evalNoCost.recall(classIndex)+","+evalNoCost.areaUnderROC(classIndex)+","+evalNoCost.kappa()+"\n");
						rw.flush();
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.UNDERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSTHRESHOLD.toString()+","+truePositivesthUs.toString()+","+falsePositivesthUs.toString()+","+trueNegativesthUs.toString()+","+falseNegativesthUs.toString()+","+evalThreshold.precision(classIndex)+","+evalThreshold.recall(classIndex)+","+evalThreshold.areaUnderROC(classIndex)+","+evalThreshold.kappa()+"\n");
						rw.flush();
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.UNDERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSLEARNING.toString()+","+truePositiveslnUs.toString()+","+falsePositiveslnUs.toString()+","+trueNegativeslnUs.toString()+","+falseNegativeslnUs.toString()+","+evalLearning.precision(classIndex)+","+evalLearning.recall(classIndex)+","+evalLearning.areaUnderROC(classIndex)+","+evalLearning.kappa()+"\n");
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
						
						Float truePositivesthOs = (float) evalThreshold.numTruePositives(classIndex);
						Float falsePositivesthOs = (float) evalThreshold.numFalsePositives(classIndex);
						Float trueNegativesthOs = (float) evalThreshold.numTrueNegatives(classIndex);
						Float falseNegativesthOs = (float) evalThreshold.numFalseNegatives(classIndex);
						
						Float truePositivesOs = (float) evalNoCost.numTruePositives(classIndex);
						Float falsePositivesOs = (float) evalNoCost.numFalsePositives(classIndex);
						Float trueNegativesOs = (float) evalNoCost.numTrueNegatives(classIndex);
						Float falseNegativesOs = (float) evalNoCost.numFalseNegatives(classIndex);
						
						Float truePositiveslnOs = (float) evalLearning.numTruePositives(classIndex);
						Float falsePositiveslnOs = (float) evalLearning.numFalsePositives(classIndex);
						Float trueNegativeslnOs = (float) evalLearning.numTrueNegatives(classIndex);
						Float falseNegativeslnOs = (float) evalLearning.numFalseNegatives(classIndex);
						
						
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.OVERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.NOCOST.toString()+","+truePositivesOs.toString()+","+falsePositivesOs.toString()+","+trueNegativesOs.toString()+","+falseNegativesOs.toString()+","+evalNoCost.precision(classIndex)+","+evalNoCost.recall(classIndex)+","+evalNoCost.areaUnderROC(classIndex)+","+evalNoCost.kappa()+"\n");
						rw.flush();
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.OVERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSTHRESHOLD.toString()+","+truePositivesthOs.toString()+","+falsePositivesthOs.toString()+","+trueNegativesthOs.toString()+","+falseNegativesthOs.toString()+","+evalThreshold.precision(classIndex)+","+evalThreshold.recall(classIndex)+","+evalThreshold.areaUnderROC(classIndex)+","+evalThreshold.kappa()+"\n");
						rw.flush();
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.OVERSAMPLING.toString()+","+featureSel+","+CostSensitiveEnum.CSLEARNING.toString()+","+truePositiveslnOs.toString()+","+falsePositiveslnOs.toString()+","+trueNegativeslnOs.toString()+","+falseNegativeslnOs.toString()+","+evalLearning.precision(classIndex)+","+evalLearning.recall(classIndex)+","+evalLearning.areaUnderROC(classIndex)+","+evalLearning.kappa()+"\n");
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
						
						Float truePositivesthSm = (float) evalThreshold.numTruePositives(classIndex);
						Float falsePositivesthSm = (float) evalThreshold.numFalsePositives(classIndex);
						Float trueNegativesthSm = (float) evalThreshold.numTrueNegatives(classIndex);
						Float falseNegativesthSm = (float) evalThreshold.numFalseNegatives(classIndex);
						
						Float truePositivesSm = (float) evalNoCost.numTruePositives(classIndex);
						Float falsePositivesSm = (float) evalNoCost.numFalsePositives(classIndex);
						Float trueNegativesSm = (float) evalNoCost.numTrueNegatives(classIndex);
						Float falseNegativesSm = (float) evalNoCost.numFalseNegatives(classIndex);
						
						Float truePositiveslnSm = (float) evalLearning.numTruePositives(classIndex);
						Float falsePositiveslnSm = (float) evalLearning.numFalsePositives(classIndex);
						Float trueNegativeslnSm = (float) evalLearning.numTrueNegatives(classIndex);
						Float falseNegativeslnSm = (float) evalLearning.numFalseNegatives(classIndex);
						
						
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.SMOTE.toString()+","+featureSel+","+CostSensitiveEnum.NOCOST.toString()+","+truePositivesSm.toString()+","+falsePositivesSm.toString()+","+trueNegativesSm.toString()+","+falseNegativesSm.toString()+","+evalNoCost.precision(classIndex)+","+evalNoCost.recall(classIndex)+","+evalNoCost.areaUnderROC(classIndex)+","+evalNoCost.kappa()+"\n");
						rw.flush();
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.SMOTE.toString()+","+featureSel+","+CostSensitiveEnum.CSTHRESHOLD.toString()+","+truePositivesthSm.toString()+","+falsePositivesthSm.toString()+","+trueNegativesthSm.toString()+","+falseNegativesthSm.toString()+","+evalThreshold.precision(classIndex)+","+evalThreshold.recall(classIndex)+","+evalThreshold.areaUnderROC(classIndex)+","+evalThreshold.kappa()+"\n");
						rw.flush();
						rw.append(projects[k].toUpperCase()+","+i.toString()+","+mw.getPercentualTraining().toString()+","+mw.getPercDefectiveInTraining().toString()+","+mw.getPercDefectiveInTesting().toString()+","+getNameClassifier(classifier[j],j)+","+Sampling.SMOTE.toString()+","+featureSel+","+CostSensitiveEnum.CSLEARNING.toString()+","+truePositiveslnSm.toString()+","+falsePositiveslnSm.toString()+","+trueNegativeslnSm.toString()+","+falseNegativeslnSm.toString()+","+evalLearning.precision(classIndex)+","+evalLearning.recall(classIndex)+","+evalLearning.areaUnderROC(classIndex)+","+evalLearning.kappa()+"\n");
						rw.flush();
						
						break;
						
					
				}
			}	
		}
	}catch(Exception e) {
		e.printStackTrace();
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
	
	
	
	
	public static void main(String[] args) throws IOException{
		
		
		for(Integer k = 0; k<projects.length;k++) {
			File file = new File(projects[k].toUpperCase()+"_Dataset.arff");
			List<Instances> wf = new ArrayList<>();
			ArffLoader loader = new ArffLoader();
			loader.setSource(file);
			Instances data = loader.getDataSet();//get instances object
			try(BufferedWriter rw = new BufferedWriter(new FileWriter(projects[k].toUpperCase()+"_Results_Weka"+".csv", true))){
			
				for(Integer j = 0; j<columns.length - 1; j++) {
					rw.append(columns[j] + ",");
				
				}
				rw.append(columns[columns.length-1]);
				rw.append("\n");
		
				AttributeStats as = data.attributeStats(0); //version column
				Stats s = as.numericStats;
				Double maxVersion = s.max;	
		
				for(Integer i = 1; i< maxVersion; i++) {
			
		
					wf = applyWalkForward(file,i,maxVersion);
					Integer classIndex = assignBuggyValue(wf.get(0));
					List<Instances> fs = FeatureSelection.applyFeatureSelection(wf.get(0),wf.get(1));
			
					valutationToCsv(k,wf.get(0), wf.get(1),classIndex,i, rw, "No_FS");
					valutationToCsv(k,fs.get(0), fs.get(1),classIndex,i, rw, "FS");
		
			
				}
			} catch (Exception e) {
		
				e.printStackTrace();
			}
			
		}
	}
}
	
 


