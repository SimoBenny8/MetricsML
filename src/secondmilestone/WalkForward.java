package secondmilestone;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

public class WalkForward {
	
	
	public static List<Instances> applyWalkForward(File projArff,Integer version) throws IOException {
		
		List<Instances> result = new ArrayList<>();
		
		ArffLoader loader = new ArffLoader();
		loader.setSource(projArff);
		Instances data = loader.getDataSet();//get instances object
		
		Instances training = new Instances(data, 0);
		Instances testing = new Instances(data, 0);
		
		for(Instance instance: data) {
			
			if (instance.value(0) <= version) {
				training.add(instance);
				
			} else if (instance.value(0) == (version + 1)) {
				testing.add(instance);
				
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
			return 1;
		else
			return 0;
		
	}
	
	
	public static void main(String[] args) {
		
		
	}

}
