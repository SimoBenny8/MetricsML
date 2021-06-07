package secondmilestone;

import java.util.ArrayList;
import java.util.List;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

public class FeatureSelection {
	
	public static List<Float> applyFeatureSelection(Instances trainingSet,Instances testingSet,AbstractClassifier ac,Integer classIndex) throws Exception{
		AttributeSelection filter = new AttributeSelection();
		//create evaluator and search algorithm objects
		CfsSubsetEval cfsEval = new CfsSubsetEval();
		GreedyStepwise search = new GreedyStepwise();
		
		List<Float> result = new ArrayList<>();
		
		//set the algorithm to search backward
		search.setSearchBackwards(true);
		//set the filter to use the evaluator and search algorithm
		filter.setEvaluator(cfsEval);
		filter.setSearch(search);
		//specify the dataset
		filter.setInputFormat(trainingSet);
		Instances trainingSetFiltered = Filter.useFilter(trainingSet, filter);
		Instances testingSetFiltered = Filter.useFilter(testingSet, filter);
		
		Integer numAttrNoFilter = trainingSet.numAttributes();
		trainingSet.setClassIndex(numAttrNoFilter - 1);
		testingSet.setClassIndex(numAttrNoFilter - 1);
		
		Integer numAttrFiltered = trainingSetFiltered.numAttributes();
		
		Evaluation eval = new Evaluation(testingSet);
		//evaluation with filtered
		trainingSetFiltered.setClassIndex(numAttrFiltered - 1);
		testingSetFiltered.setClassIndex(numAttrFiltered - 1);
		ac.buildClassifier(trainingSetFiltered);
	    eval.evaluateModel(ac, testingSetFiltered);
	    
	    Float truePositives = (float) eval.numTruePositives(classIndex);
		Float falsePositives = (float) eval.numFalsePositives(classIndex);
		Float trueNegatives = (float) eval.numTrueNegatives(classIndex);
		Float falseNegatives = (float) eval.numFalseNegatives(classIndex);
	    
	    result.add(truePositives);
	    result.add(falsePositives);
	    result.add(trueNegatives);
	    result.add(falseNegatives);
	    result.add((float)eval.precision(classIndex));
	    result.add((float)eval.recall(classIndex));
	    result.add((float)eval.areaUnderROC(classIndex));
	    result.add((float)eval.kappa());
	    
	    return result;
		
	}

	public static void main(String[] args) throws Exception {
		
		
	}

}
