package secondmilestone;

import java.util.ArrayList;
import java.util.List;

import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

public class FeatureSelection {
	
	private FeatureSelection() {
	    throw new IllegalStateException("Utility class");
	  }


	
	public static List<Instances> applyFeatureSelection(Instances trainingSet,Instances testingSet){
		List<Instances> result = new ArrayList<>();
	 try {	
		AttributeSelection filter = new AttributeSelection();
		//create evaluator and search algorithm objects
		CfsSubsetEval cfsEval = new CfsSubsetEval();
		BestFirst search = new BestFirst();
		//set the filter to use the evaluator and search algorithm
		filter.setEvaluator(cfsEval);
		filter.setSearch(search);
		//specify the dataset
		filter.setInputFormat(trainingSet);
		Instances trainingSetFiltered = Filter.useFilter(trainingSet, filter);
		Instances testingSetFiltered = Filter.useFilter(testingSet, filter);
		
		result.add(trainingSetFiltered);
		result.add(testingSetFiltered);
	 }catch(Exception e) {
		 e.printStackTrace();
	 }
		
		return result;
		
		
	}
}
