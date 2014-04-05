import processing.core.PApplet;
import processing.core.PFont;
import ddf.minim.analysis.*;
import ddf.minim.*;
import ddf.minim.effects.*;
import controlP5.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class StealthSenseInterface
{}
/*
public class StealthSenseInterface
{
	
	Minim minim;
	AudioInput in;
	FFT fft;
	PApplet mainApp;

	String[] modelLabels = {"100", "90", "80", "70", "60", "50", "40", "30"};
	int fileIndex = 0;
	double predictionValue = 0;
	boolean record = false;
	float maxPower = -999999;

	int STATE = 0;
	boolean dumpToCSV = false;
	String csvData = "";
	int dataSamples = 0;

	boolean livePrediction = true;
	
	float DOWNSAMPLE_SIZE = 47.00000f;
	int AUDIO_IN_BUFFERSIZE = 1024;
	int FFT_BUFFERSIZE = AUDIO_IN_BUFFERSIZE*16;
	int AUDIO_MULTIPLIER = 1;
	
	int sampleCount = 0;
	int LoopCount = 0;
	
	float rollingData[] = new float[FFT_BUFFERSIZE];
	
	ArrayList<String> trainingData = new ArrayList<String>();	
	WekaRegression REG;

	boolean INITIALIZING = true;
	float timeSincePlayback = 0.0000f;
	
	boolean captureData = false;
	
	String rawTrainingFile = "../models/100_to_30_3071datapoints.csv"; // "../models/100_to_30_2300datapoints.csv";		
	StealthSenseFeatureGenerator FeatureGenerator = new StealthSenseFeatureGenerator();

	public StealthSenseInterface(PApplet applet)
	{
		minim = new Minim(applet);
		mainApp = applet;
		timeSincePlayback = mainApp.millis();
		
		fft = new FFT(FFT_BUFFERSIZE, in.sampleRate());
		fft.window(FFT.HAMMING);
	}
	
	
	
	public void keyPressed()
	{
		if (cp5.get(Textfield.class,"name for current model").isActive()) {
	    	  return;
	    }
		if (key==' ')
		  {
			  if (abs(millis()-timeSincePlayback)>300) {
				  for (int i=0; i<rollingData.length; i++) {rollingData[i] = 0;}
				  sampleCount = 0;
				  captureData = true;
				  timeSincePlayback = millis();
			  }
			  
		  }
	}
	
	public void controlEvent(ControlEvent theEvent) {
		
	  if (theEvent.isFrom(modelSelector)) {
	    // Find the CSV File
	    int ind = (int)theEvent.getGroup().getValue();
	    cp5.get(Textfield.class,"name for current model").setValue(SMOModels[ind]);
	    // Load ...
	    loadCSV("../models/"+SMOModels[ind]);
	    setStatus(systemStatus());
	  } 
	  
	  if(theEvent.isFrom(sweepSelector)) {
		    //print("got an event from "+theEvent.getName()+"\t");
		    sweep.close();
		    sweep = minim.loadSnippet(AudioSweeper[(int)theEvent.getValue()]);
		    cutoffFrequency = cutOffFrequencies[(int)theEvent.getValue()]; 
		    //indexForCutoffFreq = FFTSpectralIndexForFrequency(cutoffFrequency,in.sampleRate(),FFT_BUFFERSIZE);
		    indexForCutoffFreq = fft.freqToIndex(cutoffFrequency);
		    setStatus(systemStatus());
	  }
	  
	  if (theEvent.isFrom(cp5.get(Button.class,"Save Current Model"))) {
		  String modelName = cp5.get(Textfield.class,"name for current model").getText();
		  modelName = modelName.replaceAll(".csv$","");
		  saveCSV("../models/"+modelName+".csv");
		  
		  // Reload Model List
		  modelSelector.clear();
		  SMOModels = findCSVFiles("../models/");
		  for (int i=0;i<SMOModels.length;i++) {
			  modelSelector.addItem(SMOModels[i], i);
		  }
		  
		  setStatus(systemStatus()+"\n"+"Model '" + modelName + "' saved.");
	  }
	  
	  
	}

	public void keyReleased()
	{
	  
      if (cp5.get(Textfield.class,"name for current model").isActive()) {
    	  return;
      }
		
	  if (key=='d') {
	     fileIndex = (fileIndex+1) % modelLabels.length; 
	  }
	  
	  if (key=='a') {
	     fileIndex = fileIndex - 1;
	     if (fileIndex < 0) fileIndex = modelLabels.length-1; 
	  }
	  
	  if (key=='q') {
		  try {
			  trainingData.remove(trainingData.size()-1);
		  } catch (ArrayIndexOutOfBoundsException e) {
			  println("Nothing to remove");
		  }
	  }
	  	  
	  if (key=='t')
	  {
		  REG = new WekaRegression(rawTrainingFile);
	  }
	  
	  if (key=='x') {
		  if (MODE=="AudioSweep") {
			  MODE="Config";
			  showConfigElements();
		  } else {
			 MODE="AudioSweep";
			 hideConfigElements();
		  }
		  
	  }
	  
	  if (key=='c') {
		  trainingData.clear();
		  setStatus(systemStatus()+"\n"+"Training data cleared.");
	  }
	  
	  
	}
		
	void initializeBuffer()
	{
		sweep.loop(8);
		timeSincePlayback = millis();
	}
	
	void saveCSV(String filepath)
	{
		  String[] dataDump = new String[trainingData.size()+1];
		  String headers = "";
		  String[] fields = trainingData.get(0).split(",");
		  for (int i=0; i<fields.length-1; i++)
		  {
			  headers = headers + ",f" + i; 
		  }
		  headers = headers + ",class";
		  headers = headers.replaceAll("^,","");
		  dataDump[0] = headers;
		  for (int i=0; i<trainingData.size(); i++ ) {
			  dataDump[i+1] = trainingData.get(i);
		  }
		  
		  saveStrings(filepath,dataDump);
	}
	
	void loadCSV(String filepath)
	{
		// Clear trainingData
		trainingData.clear();
		// Load CSV from disk
		String[] csv = loadStrings(filepath);
		int labelCount = -1;
		HashMap<String, String> labels = new HashMap<String, String>();
		ArrayList<String> labelArray = new ArrayList<String>();
		
		for (int i=1; i<csv.length; i++)
		{
			String[] fields = csv[i].split(",");
			String label = fields[fields.length-1];
			trainingData.add(csv[i]);
			
			if (!labels.containsKey(label)) {
				labelArray.add(label);
				labels.put(label, "1");
			}
		}
		
		String[] result = new String[labelArray.size()];
		for (int i=0; i<labelArray.size(); i++) {result[i] = (String)labelArray.get(i);}
		modelLabels = result;
		
	}
	
		 
	public void stop()
	{
	  sweep.close();
	  minim.stop();
	  super.stop();
	}

	float dB(float val)
	{
	  return Math.round(2*20*Math.log10(100*val));
	}

	String[] findCSVFiles(String filepath)
	{
		// This function returns all the files in a directory as an array of Strings  String[] listFileNames(String dir) {

		File file = new File(filepath);
		if (file.isDirectory()) {
			String names[] = file.list();
			return names;
		}
		return null;
	}
	
	@Override
	public void samples(float[] audio) {
		int millisTreshold = 100 + (10*16);
		
		if (abs(millis()-timeSincePlayback)<millisTreshold && captureData) {
			sampleCount += 1;
			for (int i = 0; i < audio.length; i++)
				// Audio multiplier
				audio[i] *= AUDIO_MULTIPLIER;

			for (int i = 0; i < rollingData.length - AUDIO_IN_BUFFERSIZE; i++)
				rollingData[i] = rollingData[i + AUDIO_IN_BUFFERSIZE];

			for (int i = 0; i < AUDIO_IN_BUFFERSIZE; i++)
				rollingData[i + (rollingData.length - AUDIO_IN_BUFFERSIZE)] = audio[i];
			
		} else if (abs(millis()-timeSincePlayback)>millisTreshold && captureData && !livePrediction) {
			
			// Save the data points
			fft.forward(rollingData);
			String csvData = "";
			ArrayList features = FeatureGenerator.featuresFromFFT(fft);
		    for (int i=0; i<features.size(); i++)
		    {
			  csvData = csvData + "," + features.get(i).toString();
		    }
		  
		    csvData = csvData.replaceAll("^,","");
		    trainingData.add(csvData+","+modelLabels[fileIndex]);
		    captureData = false;
		    
		}  else if (abs(millis()-timeSincePlayback)>millisTreshold && captureData && livePrediction && !sweep.isPlaying()) {
			
			// Save the data points
			fft.forward(rollingData);
			ArrayList featureArray = FeatureGenerator.featuresFromFFT(fft);
			
			float[] features = new float[featureArray.size()];
			for (int j=0; j<features.length; j++)
			{
				features[j] = parseFloat(featureArray.get(j).toString());
			}
			predictionValue = REG.classifyGesture(features);
			captureData = false;
		}
	}

	@Override
	public void samples(float[] arg0, float[] arg1) {
		// .. 
	}

}
*/
