package secondmilestone;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

public class FeatureSelection {

	public static void main(String[] args) throws Exception {
		AttributeSelection filter = new AttributeSelection();
		//create evaluator and search algorithm objects
		CfsSubsetEval eval = new CfsSubsetEval();
		GreedyStepwise search = new GreedyStepwise();
		DataSource source = new DataSource("C:/Program Files/Weka-3-8/data/breast-cancerKnown.arff");
		Instances trainingSet = source.getDataSet();
		Instances testingSet = source.getDataSet();
		//set the algorithm to search backward
		search.setSearchBackwards(true);
		//set the filter to use the evaluator and search algorithm
		filter.setEvaluator(eval);
		filter.setSearch(search);
		//specify the dataset
		filter.setInputFormat(trainingSet);
		Instances trainingSetFiltered = Filter.useFilter(trainingSet, filter);
		Instances testingSetFiltered = Filter.useFilter(testingSet, filter);
		
		int numAttrNoFilter = trainingSet.numAttributes();
		trainingSet.setClassIndex(numAttrNoFilter - 1);
		testingSet.setClassIndex(numAttrNoFilter - 1);
		
		int numAttrFiltered = trainingSetFiltered.numAttributes();
		
		RandomForest classifier = new RandomForest();
		Evaluation evalClass = new Evaluation(testingSet);
		//evaluation with filtered
		trainingSetFiltered.setClassIndex(numAttrFiltered - 1);
		testingSetFiltered.setClassIndex(numAttrFiltered - 1);
		classifier.buildClassifier(trainingSetFiltered);
	    evalClass.evaluateModel(classifier, testingSetFiltered);
		
		System.out.println("AUC filtered = "+evalClass.areaUnderROC(1));
		System.out.println("Kappa filtered = "+evalClass.kappa());

	}

}
