package secondmilestone;

public class MetricsWeka {
	
	private String projectName;
	private String sampling;
	private Float percentualTraining;
	private Boolean featureSelection;
	private Integer numTrainingVersions;
	private Float percDefectiveInTraining;
	private Float percDefectiveInTesting;
	private Integer datasetSize;
	
	public Float getPercentualTraining() {
		return percentualTraining;
	}

	public void setPercentualTraining(Float trainingSetSize) {
		this.percentualTraining = ((trainingSetSize/(float) getDatasetSize()) * 100);
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


	public Float getPercDefectiveInTraining() {
		return percDefectiveInTraining;
	}

	public void setPercDefectiveInTraining(Integer percDefectiveInTraining) {
		this.percDefectiveInTraining = ((float)percDefectiveInTraining/(float)getDatasetSize()*100);
	}

	public Float getPercDefectiveInTesting() {
		return percDefectiveInTesting;
	}

	public void setPercDefectiveInTesting(Integer percDefectiveInTesting) {
		this.percDefectiveInTesting = ((float)percDefectiveInTesting/(float)getDatasetSize()*100);
	}

	public Integer getDatasetSize() {
		return datasetSize;
	}

	public void setDatasetSize(Integer datasetSize) {
		this.datasetSize = datasetSize;
	}


	
}
