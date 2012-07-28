package org.gridkit.gatling.firegrid;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Skewness;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.gridkit.util.formating.Formats;

public class SampleSummary {

	public static void main(String[] args) {
		for(String file: args) {
			try {
				List<String> lines = new ArrayList<String>();
				BufferedReader reader;
				if (file.endsWith(".gz")) {
					reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
				} 
				else {
					reader = new BufferedReader(new FileReader(file));
				}
				while(true) {
					String line = reader.readLine();
					if (line == null) {
						break;
					}
					if (line.trim().length() > 0) {
						lines.add(line);
					}
				}
				if (lines.size() > 1) {
					processStats(file, lines);
				}
				else {
					System.out.println("[" + file + "] - no data");
				}
			}
			catch(Exception e) {
				System.err.println("[" + file + "] - exception " + e.toString());
			}
		}
			
	}

	private static void processStats(String file, List<String> lines) {
		long maxTimestamp = Long.MIN_VALUE;
		long minTimestamp = Long.MAX_VALUE;
		
		double scale = 1d / TimeUnit.MILLISECONDS.toNanos(1);
		double[] samples = new double[lines.size()];
		
		Map<Integer, List<String>> resultBuckets = new TreeMap<Integer, List<String>>();

		int n = 0;
		for(String line: lines) {
			String[] parts = line.split("[\t]");
			long timestamp = Long.parseLong(parts[1]);
			long duration = Long.parseLong(parts[2]);
			samples[n++] = scale * duration;
			if (maxTimestamp < timestamp) {
				maxTimestamp = timestamp;
			}
			if (minTimestamp > timestamp) {
				minTimestamp = timestamp;
			}
			int result= Integer.parseInt(parts[3].trim());
			List<String> bucket = resultBuckets.get(result);
			if (bucket == null) {
				bucket = new ArrayList<String>();
				resultBuckets.put(result, bucket);
			}
			bucket.add(line);			
		}
		
		double rate = 1d * TimeUnit.SECONDS.toMillis(1) * samples.length / (maxTimestamp - minTimestamp);
		Arrays.sort(samples);
		
		double mean = new Mean().evaluate(samples);
		double stdDev = new StandardDeviation().evaluate(samples);
		double skew = new Skewness().evaluate(samples);
		
		double min = samples[0];
		double k1 = samples[    samples.length / 10];
		double k2 = samples[2 * samples.length / 10];
		double k3 = samples[3 * samples.length / 10];
		double k4 = samples[4 * samples.length / 10];
		double k5 = samples[5 * samples.length / 10];
		double k6 = samples[6 * samples.length / 10];
		double k7 = samples[7 * samples.length / 10];
		double k8 = samples[8 * samples.length / 10];
		double k9 = samples[9 * samples.length / 10];
		double max = samples[samples.length - 1];
		
		System.out.println("[" + file + "] - " + samples.length + " samples. " + Formats.toTimestamp(minTimestamp) + " - " + Formats.toTimestamp(maxTimestamp) + " (" + TimeUnit.MILLISECONDS.toSeconds(maxTimestamp - minTimestamp) + " sec)");
		System.out.println(String.format("  Rate: %.2f ms", rate));
		System.out.println(String.format("  Mean: %.4f ms", mean));
		System.out.println(String.format("  StdDev: %.4f ms", stdDev));
		System.out.println(String.format("  Skew: %.4f ms", skew));
		System.out.println(String.format("  Percentiles: min: %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f |max: %.4f ", min, k1,k2,k3,k4,k5,k6,k7,k8,k9,max));
		
		if (resultBuckets.size() > 1) {
			for(List<String> bucket: resultBuckets.values()) {
				printBucket(bucket);
			}
		}
	}

	private static void printBucket(List<String> lines) {
		
		double scale = 1d / TimeUnit.MILLISECONDS.toNanos(1);
		double[] samples = new double[lines.size()];

		int result = 0;
		int n = 0;
		for(String line: lines) {
			String[] parts = line.split("[\t]");
			long duration = Long.parseLong(parts[2]);
			samples[n++] = scale * duration;
			result= Integer.parseInt(parts[3].trim());
		}
		
		Arrays.sort(samples);
		
		double mean = new Mean().evaluate(samples);
		double stdDev = new StandardDeviation().evaluate(samples);
		double skew = new Skewness().evaluate(samples);
		
		double min = samples[0];
		double k1 = samples[    samples.length / 10];
		double k2 = samples[2 * samples.length / 10];
		double k3 = samples[3 * samples.length / 10];
		double k4 = samples[4 * samples.length / 10];
		double k5 = samples[5 * samples.length / 10];
		double k6 = samples[6 * samples.length / 10];
		double k7 = samples[7 * samples.length / 10];
		double k8 = samples[8 * samples.length / 10];
		double k9 = samples[9 * samples.length / 10];
		double max = samples[samples.length - 1];
		
		System.out.println(String.format("  Result code %d - %d samples", result, samples.length));
		System.out.println(String.format("    Mean: %.4f ms", mean));
		System.out.println(String.format("    StdDev: %.4f ms", stdDev));
		System.out.println(String.format("    Skew: %.4f ms", skew));
		System.out.println(String.format("    Percentiles: min: %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f |max: %.4f ", min, k1,k2,k3,k4,k5,k6,k7,k8,k9,max));
	}
}
