
import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;


public class WekaRegression
{
	SMOreg reg;
	Instances trainingdata;
	
	// Creates a weka classifier from a training file or pre-built model.
	// params:
	//   trainingFile: the file to load.
	//   isModel: Whether the file is actually a model
	public WekaRegression(String trainingFile)
	{
		try
		{		
			DataSource trainingSource = new DataSource(trainingFile);
			trainingdata = trainingSource.getDataSet();					
			trainingdata.setClassIndex(trainingdata.numAttributes() - 1);
			
			reg = new SMOreg();
			
			reg.setKernel(new RBFKernel());
			reg.buildClassifier(trainingdata);
			
			Evaluation eval = new Evaluation(trainingdata);
			eval.crossValidateModel(reg, trainingdata, 4, new Random(1));
			System.out.println(eval.toSummaryString());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public WekaRegression(String trainingFile, String modelFile){
		// Load training data
		DataSource trainingSource;
		try {
			trainingSource = new DataSource(trainingFile);
			trainingdata = trainingSource.getDataSet();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		trainingdata.setClassIndex(trainingdata.numAttributes() - 1);
		
	}

	
	public double classifyGesture(float[] features)
	{
		Instance instance = new Instance(features.length);
		instance.setDataset(trainingdata);

		double gestureID = -1;
		
		for (int i=0;i<features.length;i++)
			instance.setValue(i,features[i]);
		
		try 
		{
			gestureID = (double) reg.classifyInstance(instance);
			
		} 
		catch (Exception e) 
		{
			System.out.println(e.getLocalizedMessage());
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
		return gestureID;
	}
}
