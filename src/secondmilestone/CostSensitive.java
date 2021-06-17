package secondmilestone;


import weka.classifiers.AbstractClassifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.core.Instances;

public class CostSensitive {
	
	 private CostSensitive() {
		    throw new IllegalStateException("Utility class");
	 }

	
	private static CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) {
	    CostMatrix costMatrix = new CostMatrix(2);
	    costMatrix.setCell(0, 0, 0.0);
	    costMatrix.setCell(1, 0, weightFalsePositive);
	    costMatrix.setCell(0, 1, weightFalseNegative);
	    costMatrix.setCell(1, 1, 0.0);
	    return costMatrix;
	}
	
	public static Evaluation applyCostSensitive(AbstractClassifier ac,Instances trainingSet,Instances testingSet,Boolean threshold){
		Evaluation eval = null; 
	try {	
		CostSensitiveClassifier c1 = new CostSensitiveClassifier();
		c1.setClassifier(ac);

		c1.setCostMatrix(createCostMatrix(1.0, 10.0));
		c1.buildClassifier(trainingSet);
		c1.setMinimizeExpectedCost(threshold);

		eval = new Evaluation(testingSet,c1.getCostMatrix());
		eval.evaluateModel(c1, testingSet);
	 }catch (Exception e) {
		 e.printStackTrace();
	 }
	    
	    return eval;
		
	}


}
