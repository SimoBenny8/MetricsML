package secondmilestone;

public class MetricsWeka {
	
	private String projectName;
	private Classifier classifier;
	private String sampling;
	private Float percentualTraining;
	private Boolean featureSelection;
	private Integer numTrainingVersions;
	private Integer numDefectiveInTraining;
	private Integer numDefectiveInTesting;
	private Integer datasetSize;
	
	public Float getPercentualTraining() {
		return percentualTraining;
	}

	public void setPercentualTraining(Float trainingSetSize) {
		this.percentualTraining = (((float) trainingSetSize/(float) getDatasetSize()) * 100);
	}


	public Integer getNumTrainingVersions() {
		return numTrainingVersions;
	}

	public void setNumTrainingVersions(Integer numTrainingVersions) {
		this.numTrainingVersions = numTrainingVersions;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public Classifier getClassifier() {
		return classifier;
	}

	public void setClassifier(Classifier classifier) {
		this.classifier = classifier;
	}

	public String getSampling() {
		return sampling;
	}

	public void setSampling(String sampling) {
		this.sampling = sampling;
	}

	public boolean isFeatureSelection() {
		return featureSelection;
	}

	public void setFeatureSelection(Boolean featureSelection) {
		this.featureSelection = featureSelection;
	}


	public Integer getNumDefectiveInTraining() {
		return numDefectiveInTraining;
	}

	public void setNumDefectiveInTraining(Integer numDefectiveInTraining) {
		this.numDefectiveInTraining = numDefectiveInTraining;
	}

	public Integer getNumDefectiveInTesting() {
		return numDefectiveInTesting;
	}

	public void setNumDefectiveInTesting(Integer numDefectiveInTesting) {
		this.numDefectiveInTesting = numDefectiveInTesting;
	}

	public Integer getDatasetSize() {
		return datasetSize;
	}

	public void setDatasetSize(Integer datasetSize) {
		this.datasetSize = datasetSize;
	}


	public static void main(String[] args) {
		

	}

}
