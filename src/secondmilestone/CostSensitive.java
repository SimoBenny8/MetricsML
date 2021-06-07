package secondmilestone;

import java.util.ArrayList;
import java.util.List;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.core.Instances;

public class CostSensitive {
	
	
	private static CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) {
	    CostMatrix costMatrix = new CostMatrix(2);
	    costMatrix.setCell(0, 0, 0.0);
	    costMatrix.setCell(1, 0, weightFalsePositive);
	    costMatrix.setCell(0, 1, weightFalseNegative);
	    costMatrix.setCell(1, 1, 0.0);
	    return costMatrix;
	}
	
	public static List<Float> applyCostSensitive(AbstractClassifier ac,Instances trainingSet,Instances testingSet,Integer classIndex,Boolean threshold) throws Exception{
		
		List<Float> result = new ArrayList<>();
		CostSensitiveClassifier c1 = new CostSensitiveClassifier();
		c1.setClassifier(ac);

		c1.setCostMatrix(createCostMatrix(1.0, 10.0));
		c1.buildClassifier(trainingSet);
		c1.setMinimizeExpectedCost(threshold);

		Evaluation eval = new Evaluation(testingSet,c1.getCostMatrix());
		eval.evaluateModel(c1, testingSet);
		
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
