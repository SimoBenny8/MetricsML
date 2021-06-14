package secondmilestone;

import java.util.ArrayList;
import java.util.List;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

public class FeatureSelection {
	
	private FeatureSelection() {
	    throw new IllegalStateException("Utility class");
	  }


	
	public static List<Instances> applyFeatureSelection(Instances trainingSet,Instances testingSet) throws Exception{
		AttributeSelection filter = new AttributeSelection();
		//create evaluator and search algorithm objects
		CfsSubsetEval cfsEval = new CfsSubsetEval();
		GreedyStepwise search = new GreedyStepwise();
		
		List<Instances> result = new ArrayList<>();
		
		//set the algorithm to search backward
		search.setSearchBackwards(true);
		//set the filter to use the evaluator and search algorithm
		filter.setEvaluator(cfsEval);
		filter.setSearch(search);
		//specify the dataset
		filter.setInputFormat(trainingSet);
		Instances trainingSetFiltered = Filter.useFilter(trainingSet, filter);
		Instances testingSetFiltered = Filter.useFilter(testingSet, filter);
		
		result.add(trainingSetFiltered);
		result.add(testingSetFiltered);
		
		return result;
		
		
	}
}
