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

public class LiveMachineLearning extends PApplet implements AudioListener
{
	
	Minim minim;
	AudioInput in;
	AudioSnippet sweep;
	FFT fft;
	PFont font = loadFont("../fonts/BlenderPro-Bold-48.vlw");

	float[] fftAverage;
	float[] fftSmooth;
	
	String[] modelLabels = {"125Deg", "90Deg", "60Deg", "30Deg"};
	int fileIndex = 0;
	int predictionIndex = 0;
	boolean record = false;
	float maxPower = -999999;

	int STATE = 0;
	boolean dumpToCSV = false;
	String csvData = "";
	int dataSamples = 0;

	float steadyStateTime = 0;
	boolean livePrediction = false;
	
	float DOWNSAMPLE_SIZE = 47.00000f;
	int AUDIO_IN_BUFFERSIZE = 1024;
	int FFT_BUFFERSIZE = AUDIO_IN_BUFFERSIZE*16;
	int AUDIO_MULTIPLIER = 1;
	
	int sampleCount = 0;
	int LoopCount = 0;
	
	float rollingData[] = new float[FFT_BUFFERSIZE];
	
	ArrayList<String> trainingData = new ArrayList<String>();	
	WekaClassifier CLS;

	boolean INITIALIZING = true;
	float timeSincePlayback = 0.0000f;
	
	boolean captureData = false;
	
	String MODE = "AudioSweep"; // AudioSweep, Config
	String[] SMOModels;
	String[] AudioSweeper = {"../audio/InAudible50msX8.mp3","../audio/Audible50msX2_400Silence.mp3"};
	int[] cutOffFrequencies = {0,0};
	int indexForCutoffFreq;
	int cutoffFrequency = cutOffFrequencies[0];
	
	// Config
	ControlP5 cp5;
	DropdownList modelSelector;
	RadioButton sweepSelector;
	Textlabel TextLabelForModelLoader;
	Textlabel TextLabelForSweepSelector;
	Textlabel TextLabelForCurrentModel;
	Textlabel TextLabelForStatus;
	Textarea Status;
	  
	
	StealthSenseFeatureGenerator FeatureGenerator = new StealthSenseFeatureGenerator();

	public void setup()
	{
	  
	  minim = new Minim(this);
	  timeSincePlayback = millis();

	  // Sampling Rate: 96000
	  // Max Freq Supported: 47998.535 Hz
	  in = minim.getLineIn(Minim.MONO,AUDIO_IN_BUFFERSIZE,96000);
	  in.addListener(this);
	  
	  sweep = minim.loadSnippet(AudioSweeper[0]);
	  sweep.setGain(-150);
	  
	  fft = new FFT(FFT_BUFFERSIZE, in.sampleRate());
	  fft.window(FFT.HAMMING);
	  
	  size(1024, 500);
	  background(0);
	  fftAverage = new float[fft.specSize()];
	  fftSmooth = new float[fft.specSize()];
	  
	  indexForCutoffFreq = fft.freqToIndex(cutoffFrequency);
	  System.out.println(fft.specSize()-indexForCutoffFreq);
	  
	  DOWNSAMPLE_SIZE = 286;
	  timeSincePlayback = millis();
	  
	  // Setup Config
	  cp5 = new ControlP5(this);
	  
	  ///////////////////////
	  // 1.0 Configure Model Loader
	  ///////////////////////
	  modelSelector = cp5.addDropdownList("loadmodel").setPosition(40, 230);
	  modelSelector.setBackgroundColor(color(190));
	  modelSelector.setItemHeight(20);
	  modelSelector.setBarHeight(15);
	  modelSelector.setWidth(150);
	  modelSelector.captionLabel().set("SELECT MODEL");
	  modelSelector.captionLabel().style().marginTop = 3;
	  modelSelector.captionLabel().style().marginLeft = 3;
	  modelSelector.valueLabel().style().marginTop = 3;
	  SMOModels = findCSVFiles("../models/");
	  for (int i=0;i<SMOModels.length;i++) {
		  modelSelector.addItem(SMOModels[i], i);
	  }
	  modelSelector.setColorBackground(color(60));
	  modelSelector.setColorActive(color(255, 128));
	 
	  ///////////////////////
	  // 2.0 Configure Sweep Selector
	  ///////////////////////
	  sweepSelector = cp5.addRadioButton("radioButton")
			         .setPosition(200,215)
			         .setSize(40,20)
			         .setColorForeground(color(120))
			         .setColorActive(color(255))
			         .setColorLabel(color(255))
			         .setItemsPerRow(1)
			         .setSpacingColumn(50)
			         .addItem("Audible Sweep",0)
			         .addItem("InAudible Sweep",1)
			         .setArrayValue(new float[]{1.0f,0.0f});
	  
	  ///////////////////////
	  // 3.0 Labels
	  ///////////////////////
	  TextLabelForModelLoader = cp5.addTextlabel("label-modelloader")
              .setText("Load Previous Model:")
              .setPosition(40,200);
	  TextLabelForSweepSelector = cp5.addTextlabel("label-sweepselector")
              .setText("Select Audio Sweep:")
              .setPosition(200,200);
	  TextLabelForStatus = cp5.addTextlabel("label-status")
              .setText("STATUS:")
              .setPosition(400,140);
	  
	  /////////////////////////////
	  // 4.0 Current Model
	  /////////////////////////////
	  cp5.addTextfield("name for current model")
	     .setPosition(40,140)
	     .setSize(200,20)
	     .setColor(color(255,0,0));
	  
	  cp5.addButton("Save Current Model")
	     .setPosition(250,140)
	     .setSize(100,20);
	  
	  
	  ///////////////////////
	  // 5.0 Status
	  ///////////////////////
	  Status = cp5.addTextarea("status")
              .setPosition(400,160)
              .setSize(200,200)
              .setLineHeight(14)
              .setColor(color(255))
              .setColorBackground(color(16,53,76))
              .setColorForeground(color(16,53,76));
	  	  
	  // Hide Config Elements
	  hideConfigElements();
	  
	  // Pull-up system status
	  setStatus(systemStatus());
	  
	}
	 
	public void draw()
	{
 
		if (MODE=="AudioSweep") {
			background(0);
			smooth();
			drawAudioFrequencyDomain();
			drawAudioTimeDomain();
			drawLabels();
		} else if (MODE=="Config") {
			background(77);
			textFont(font, 48);
			text("Config", 30, 60);
		}

	}
	
	void hideConfigElements()
	{
		modelSelector.hide();
		sweepSelector.hide();
		TextLabelForModelLoader.hide();
		TextLabelForSweepSelector.hide();
		cp5.get(Textfield.class,"name for current model").hide();
		cp5.get(Button.class,"Save Current Model").hide();
		Status.hide();
		TextLabelForStatus.hide();
	}
	
	void showConfigElements()
	{
		modelSelector.show();
		sweepSelector.show();
		TextLabelForModelLoader.show();
		TextLabelForSweepSelector.show();
		cp5.get(Textfield.class,"name for current model").show();
		cp5.get(Button.class,"Save Current Model").show();
		Status.show();
		TextLabelForStatus.show();
	}
	
	void setStatus(String message)
	{
		Status.setText(message);
	}
	
	String systemStatus()
	{
		String msg = "";
		msg += "FFT TimeSize: " + fft.timeSize() + "\n";
		msg += "FFT SpecSize: " + fft.specSize() + "\n";
		msg += "Sampling Rate: " + in.sampleRate()+ "\n";
		msg += "Cutoff Frequency: " + cutoffFrequency + "\n";
		msg += "Index for Cutoff Freq: " + indexForCutoffFreq + "\n";
		msg += "Max Freq Supported for FFT: " + fft.indexToFreq(fft.specSize()-1) + "\n";
		msg += "# Bands In Frequency Range: " + (fft.specSize()-indexForCutoffFreq) + "\n";
		
		msg += "-----" + "\n";
	    ArrayList features = FeatureGenerator.featuresFromFFT(fft);
	    
		msg += "Downsamples: " + DOWNSAMPLE_SIZE + "\n";
		msg += "# of Features: " + features.size() + "\n";
		
		msg += "-----" + "\n";
		
		msg += "Model Data Size: " + trainingData.size() + "\n";
		msg += "Labels: " + Arrays.toString(modelLabels) +"\n";
		
		return msg;
	}
	
	void drawLabels()
	{
	  fill(255);
	  if (!livePrediction) {
		  textFont(font, 48);
		  text(modelLabels[fileIndex], 5, 60);
		  text("n: " + trainingData.size(), width/2, 60);
	  } else {
		  text(modelLabels[predictionIndex], 5, 60);
	  }
	}
	
	void drawAudioFrequencyDomain()
	{		  
		  float[] raw = new float[fft.specSize()];
		  for (int i=0; i<fft.specSize(); i++) {
			  raw[i] = fft.getBand(i);
		  }
		  
		  int DOWNSAMPLE = 565;
		  float subWindow = (float)(width) / DOWNSAMPLE;
		  float[] fftDownsampled = FeatureGenerator.downSample(raw, DOWNSAMPLE);
		  
		  noStroke();
	      for (int i=0; i<fftDownsampled.length; i++)
	      {
	    	  fill(128,128,128,128);
	    	  rect(subWindow*i, height - (2.5f*dB(fftDownsampled[i])),subWindow,(2.5f*dB(fftDownsampled[i])));
	      } 
	}
		
	void drawAudioTimeDomain()
	{
		  stroke(255);
		  int baseLine = 120;
		  for (int i=0; i<rollingData.length; i++)
		  {
			  float startX = map(i,rollingData.length,0,width,0);
			  float endX = map(i+1,rollingData.length,0,width,0);
			  
			  if (i%in.bufferSize()==0) {
				  line(startX,baseLine-5,startX,baseLine+5);
			  }
			  
			  try { 
		        line(startX, baseLine - (500*rollingData[i-1]), endX, baseLine - (500*rollingData[i]));
		    } catch (Exception e) {
		    	line(startX, baseLine, endX, baseLine - (500*rollingData[i]));
		    }
		  }
	}
		
	float[] getRawFeaturesFromFFT()
	{
		float[] raw = new float[fft.specSize()-indexForCutoffFreq];
		for (int i=0; i<raw.length; i++) {
			raw[i] = fft.getBand(indexForCutoffFreq+i);
		}
		return raw;
	}
	
	float[] getRawFeaturesFromFFTInfraSoundAndUltraSound()
	{
		int infra_lb = fft.freqToIndex(7);
		int infra_ub = fft.freqToIndex(12);
		float[] raw = new float[(infra_ub-infra_lb)+(fft.specSize()-indexForCutoffFreq)];
		
		for (int i=0; i<(infra_ub-infra_lb); i++) {
			raw[i] = fft.getBand(i+infra_ub);
		}
		
		for (int i=(infra_ub-infra_lb); i<raw.length; i++) {
			raw[i] = fft.getBand(indexForCutoffFreq+i);
		}
		return raw;
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
		  String filepath = "train.csv";
		  saveCSV(filepath);
		  CLS = new WekaClassifier(filepath);
	  }
	  
	  if (key=='l') {
		  livePrediction = !livePrediction;
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
			int prediction = CLS.classifyGesture(features);
			predictionIndex = prediction;
			captureData = false;
		    
		}

	}

	@Override
	public void samples(float[] arg0, float[] arg1) {
		// .. 
	}

}
