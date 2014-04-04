import java.util.ArrayList;

import ddf.minim.analysis.FFT;


class FrequencySweepRange
{
	float start = 0;
	float end = 0;
	
	public FrequencySweepRange(float s, float e) {
		start = s;
		end = e;
	}
}

public class StealthSenseFeatureGenerator {
	
	public static FrequencySweepRange InfraSound = new FrequencySweepRange(2, 12);
	public static FrequencySweepRange UltraSound = new FrequencySweepRange(19000, 40000);
	
	public ArrayList<Float> featuresFromFFT(FFT fft) {
		ArrayList<Float> features = new ArrayList<Float>();
		
		// Extract Raw FFT
		float[] infraSoundSignal = extractInfraSound(fft);
		float[] ultraSoundSignal = downSample(extractUltraSound(fft), 448);
		for (int i=0; i<infraSoundSignal.length; i++) features.add(infraSoundSignal[i]);
		for (int i=0; i<ultraSoundSignal.length; i++) features.add(ultraSoundSignal[i]);
		
		// RMS
		features.add(RMS(infraSoundSignal));
		features.add(RMS(ultraSoundSignal));
		
		// Center of Mass
		features.add(centerOfMass(infraSoundSignal));
		features.add(centerOfMass(ultraSoundSignal));
		
		// Max and Min
		int infraSoundMaxIndex = maxIndex(infraSoundSignal);
		int ultraSoundMaxIndex = maxIndex(ultraSoundSignal);
		features.add(fft.indexToFreq(fft.freqToIndex(InfraSound.start)+infraSoundMaxIndex));
		features.add(fft.indexToFreq(fft.freqToIndex(UltraSound.start)+ultraSoundMaxIndex));
		features.add(infraSoundSignal[infraSoundMaxIndex]);
		features.add(ultraSoundSignal[ultraSoundMaxIndex]);
		
		// Average
		features.add(average(infraSoundSignal));
		features.add(average(ultraSoundSignal));
		
		// Standard Deviation
		features.add(standardDeviation(infraSoundSignal));
		features.add(standardDeviation(ultraSoundSignal));
		
		// Spectral Band Ratios
		float[] infraSoundSpectralBandRatio = spectralBandRatio(infraSoundSignal);
		float[] ultraSoundSpectralBandRatio = spectralBandRatio(downSample(extractUltraSound(fft), 28));
		for (int i=0; i<infraSoundSpectralBandRatio.length; i++) features.add(infraSoundSpectralBandRatio[i]);
		for (int i=0; i<ultraSoundSpectralBandRatio.length; i++) features.add(ultraSoundSpectralBandRatio[i]);
		
		// That's it!
		return(features);
	}
	
	public float[] extractInfraSound(FFT fft) 
	{
		int start_indx = fft.freqToIndex(InfraSound.start);
		int end_indx = fft.freqToIndex(InfraSound.end);
		
		/* Grab FFT Values. Store into an ArrayList to avoid nasty IndexExceptions */
		ArrayList<Float> fftValues = new ArrayList<Float>();	
		for (int i=start_indx; i<end_indx; i++) {
			fftValues.add(fft.getBand(i));
		}
		
		return arrayListToFloatArray(fftValues);
	}
	
	public float[] extractUltraSound(FFT fft) 
	{
		int start_indx = fft.freqToIndex(UltraSound.start);
		int end_indx = fft.freqToIndex(UltraSound.end);
		
		/* Grab FFT Values. Store into an ArrayList to avoid nasty IndexExceptions */
		ArrayList<Float> fftValues = new ArrayList<Float>();	
		for (int i=start_indx; i<end_indx; i++) {
			fftValues.add(fft.getBand(i));
		}
		
		float[] result = new float[fftValues.size()];
		int i = 0;
		for (Float f : fftValues) {
			result[i++] = (f != null ? f : Float.NaN);
		}
		
		return result;
	}
	
	public float RMS(float[] signal) {
		float sum = 0;
		for (int i=0; i<signal.length; i++) {
			sum += signal[i]*signal[i];
		}
		return (float)Math.sqrt(sum/signal.length);
	}
	
	public float[] downSample(float[] samples, int downSampleSize)
	{
		// Divide Spectral Band into DOWNSAMPLE_SIZE
		int residue = samples.length%(int)downSampleSize;
		int binSize = 0;
		
		// Figure out the width of the each downsampled band
		if (residue<(downSampleSize*0.5f)) {
			binSize = (int)Math.floor((float)samples.length/downSampleSize);
		} else {
			binSize = (int)Math.ceil((float)samples.length/downSampleSize);
		}
		
		float[] result = new float[downSampleSize];
		int subSampleCount = 0;
		int current_index = 0;
		for (int i=0; i<samples.length; i++)
		{
			int index = (int) Math.floor(i/binSize);
			index = (int)Math.min(downSampleSize-1, index);
			if (index==current_index) {
				subSampleCount += 1;
				result[index] += samples[i];
			} else {
				result[current_index] = result[current_index]/subSampleCount;
				current_index = index;
				subSampleCount = 1;
			}
			
		}
		
		// Perform this for the very last downsample band
		result[current_index] = result[current_index]/subSampleCount;
		return result;
	}
	
	public float centerOfMass(float[] signal)
	{
		float totalCount = 0;
        float totalSum = 0;

        for (int index = 0;index<signal.length;index++)
        {
            totalCount += signal[index];
            totalSum += index * signal[index];
        }

        return (float)totalSum/totalCount;
	}
	
	public int maxIndex (float[] signal)
	{
		float mx = Float.MIN_VALUE;
		int mx_indx = 0;
		for (int i=0; i<signal.length; i++) {
			if (signal[i]>mx) {
				mx = signal[i];
				mx_indx = i;
			}
		}
		return mx_indx;
	}
	
	public int minIndex(float[] signal)
	{
		float mn = Float.MAX_VALUE;
		int mn_indx = 0;
		for (int i=0; i<signal.length; i++) {
			if (signal[i]<mn) {
				mn = signal[i];
				mn_indx = i;
			}
		}
		return mn_indx;
	}
	
	public float average(float[] signal) {
		float avg = 0;
		for (int i=0; i<signal.length; i++) {
			avg += signal[i];
		}
		return (avg/signal.length);
	}
	
	public float standardDeviation(float[] signal) {
		float avg = average(signal);
		float sum = 0;
		for (int i=0; i<signal.length; i++)
		{
			sum += Math.pow(signal[i]-avg, 2);
		}		
		return (float)sum/(signal.length);
	}
	
	public float[] spectralBandRatio(float[] signal) {
		// Add ratio as features
		ArrayList<Float> ratios = new ArrayList<Float>();
		for (int i=0; i<signal.length; i++)
		{
		  for (int j=i+1; j<signal.length; j++)
		  {
			  ratios.add((1+signal[i])/(1+signal[j]));
		  }
		}
		return arrayListToFloatArray(ratios);
	}
		
	public float[] arrayListToFloatArray(ArrayList<Float> floatArrayList)
	{
		
		float[] result = new float[floatArrayList.size()];
		int i = 0;
		for (Float f : floatArrayList) {
			result[i++] = (f != null ? f : Float.NaN);
		}
		return result;
	}
}
