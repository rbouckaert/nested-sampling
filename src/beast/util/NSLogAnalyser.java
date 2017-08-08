package beast.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.app.BEASTVersion2;
import beast.app.util.Utils;
import beast.core.util.Log;
import beast.util.LogAnalyser;

public class NSLogAnalyser extends LogAnalyser {
	public int particleCount = -1;
	
    public NSLogAnalyser(String absolutePath, int burninPercentage, boolean quiet, int particleCount) throws IOException {
		super(absolutePath, burninPercentage, quiet, false);
		this.particleCount = particleCount;
		calcStats();
	}


	/**
     * calculate statistics on the data, one per column.
     * First column (sample nr) is not set *
     */
    @Override
    public void calcStats() {
    	if (!m_sLabels[1].equals("NSlikelihood")) {
    		throw new IllegalArgumentException("This does not appear to be generated by NS sampling since the second column is not 'NSlikelihood'");
    	}
    	// calc marginal likelihood Z
    	Double [] NSLikelihoods = m_fTraces[1];
		// Z = \sum_i Li*wi
 		double Z = 0;
 		double m = NSLikelihoods[NSLikelihoods.length - 1];
 		int N = particleCount;
 		double [] weights = new double[NSLikelihoods.length];
 		for (int i = 0; i < NSLikelihoods.length; i++) {
 			double Xi = Math.exp(-(double)i/N);
 			double Xi_1 = Math.exp(-(i-1.0)/N);
 			double wi = Xi_1 - Xi;
 			weights[i] = wi * Math.exp(NSLikelihoods[i] - m);
 			Z += weights[i];
 		}
 		Z = Math.log(Z) + m;
 		
 		Log.warning("Marginal likelihood: " + Z);

 		// normalise weights so they sum to 1
 		double w = Randomizer.getTotal(weights);
 		for (int i = 0; i < NSLikelihoods.length; i++) {
 			weights[i] /= w;
 		}
 		
 		// max nr of posterior samples
 		double ESS = 0;
 		for (int i = 0; i < NSLikelihoods.length; i++) {
 			if (weights[i] > 0) {
 				ESS -= weights[i] * Math.log(weights[i]);
 			}
 		}
 		ESS = Math.exp(ESS);
 		
 		Log.warning("Max ESS: " + ESS);
        
        logln("\nCalculating statistics\n\n" + BAR);
        int stars = 0;
        int items = m_sLabels.length;
        m_fMean = newDouble(items);
        m_fStdError = newDouble(items);
        m_fStdDev = newDouble(items);
        m_fMedian = newDouble(items);
        m_f95HPDlow = newDouble(items);
        m_f95HPDup = newDouble(items);
        m_fESS = newDouble(items);
        m_fACT = newDouble(items);
        m_fGeometricMean = newDouble(items);
//        int sampleInterval = (int) (m_fTraces[0][1] - m_fTraces[0][0]);
        for (int i = 2; i < items; i++) {
            // calc mean and standard deviation
            Double[] trace = m_fTraces[i];
            double sum = 0, sum2 = 0;
            for (int k = 0; k < trace.length; k++) {
                double f = trace[k];
     			double wi = weights[k];
                sum += f * wi;
                sum2 += f * f * wi;
            }
            if (m_types[i] != type.NOMINAL) {
                m_fMean[i] = sum;// / trace.length;
                m_fStdDev[i] = Math.sqrt(sum2 /* trace.length */ - m_fMean[i] * m_fMean[i]);
            } else {
                m_fMean[i] = Double.NaN;
                m_fStdDev[i] = Double.NaN;
            }

//            if (m_types[i] == type.REAL || m_types[i] == type.INTEGER) {
//                // calc median, and 95% HPD interval
//                Double[] sorted = trace.clone();
//                Arrays.sort(sorted);
//                m_fMedian[i] = sorted[trace.length / 2];
//                // n instances cover 95% of the trace, reduced down by 1 to match Tracer
//                int n = (int) ((sorted.length - 1) * 95.0 / 100.0);
//                double minRange = Double.MAX_VALUE;
//                int hpdIndex = 0;
//                for (int k = 0; k < sorted.length - n; k++) {
//                    double range = sorted[k + n] - sorted[k];
//                    if (range < minRange) {
//                        minRange = range;
//                        hpdIndex = k;
//                    }
//                }
//                m_f95HPDlow[i] = sorted[hpdIndex];
//                m_f95HPDup[i] = sorted[hpdIndex + n];
//
//                // calc effective sample size
//                m_fACT[i] = ESS.ACT(m_fTraces[i], sampleInterval);
//                m_fStdError[i] = ESS.stdErrorOfMean(trace, sampleInterval);
//                m_fESS[i] = trace.length / (m_fACT[i] / sampleInterval);
//
//                // calc geometric mean
//                if (sorted[0] > 0) {
//                    // geometric mean is only defined when all elements are positive
//                    double gm = 0;
//                    for (double f : trace)
//                        gm += Math.log(f);
//                    m_fGeometricMean[i] = Math.exp(gm / trace.length);
//                } else
//                    m_fGeometricMean[i] = Double.NaN;
//            } else {
                m_fMedian[i] = Double.NaN;
                m_f95HPDlow[i] = Double.NaN;
                m_f95HPDup[i] = Double.NaN;
                m_fACT[i] = Double.NaN;
                m_fESS[i] = Double.NaN;
                m_fGeometricMean[i] = Double.NaN;
//            }
            while (stars < 80 * (i + 1) / items) {
                log("*");
                stars++;
            }
        }
        logln("\n");
    } // calcStats

    
    private Double[] newDouble(int items) {
		Double [] array = new Double[items];
		Arrays.fill(array, Double.NaN);
		return array;
	}

    static void printUsageAndExi() {
        	System.out.println("NSLogAnalyser [file1] ... [filen]");
            System.out.println("-oneline Display only one line of output per file.\n" +
                    "         Header is generated from the first file only.\n" +
                    "         (Implies quiet mode.)");
            System.out.println("-quiet Quiet mode.  Avoid printing status updates to stderr.");
        	System.out.println("-help");
        	System.out.println("--help");
        	System.out.println("-h print this message");
        	System.out.println("[fileX] log file to analyse. Multiple files are allowed, each is analysed separately");
        	System.exit(0);
	}
    
	public static void main(String[] args) {
    try {
        NSLogAnalyser analyser;
        	// process args
        	int burninPercentage = 0;
            boolean oneLine = false;
            boolean quiet = false;
        	List<String> files = new ArrayList<>();
        	int N = -1;
        	int i = 0;
        	while (i < args.length) {
        		String arg = args[i];
                switch (arg) {
                case "-oneline":
                    oneLine = true;
                    i += 1;
                    break;

                case "-quiet":
                    quiet = true;
                    i += 1;
                    break;
                case "-N":
                	N = Integer.parseInt(args[i+1]);
                    i += 2;
                    break;

        		case "-h":
        		case "-help":
        		case "--help":
        			printUsageAndExit();
        			break;
        		default:
        			if (arg.startsWith("-")) {
        				Log.warning.println("unrecognised command " + arg);
        				printUsageAndExit();
        			}
        			files.add(arg);
        			i++;
        		}
        	}
        	if (N < 0) {
        		throw new IllegalArgumentException("Number of particles must be specified with the -N argument");
        	}
        	if (files.size() == 0) {
        		// no file specified, open file dialog to select one
                BEASTVersion2 version = new BEASTVersion2();
                File file = Utils.getLoadFile("LogAnalyser " + version.getVersionString() + " - Select log file to analyse",
                        null, "BEAST log (*.log) Files", "log", "txt");
                if (file == null) {
                    return;
                }
                analyser = new NSLogAnalyser(file.getAbsolutePath(), burninPercentage, quiet, N);
                analyser.print(System.out);
        	} else {
        		// process files
                if (oneLine) {
                    for (int idx=0; idx<files.size(); idx++) {
                        analyser = new NSLogAnalyser(files.get(idx), burninPercentage, true, N);

                        if (idx == 0)
                            analyser.printOneLineHeader(System.out);

                        analyser.printOneLine(System.out);
                    }

                } else {
                    for (String file : files) {
                        analyser = new NSLogAnalyser(file, burninPercentage, quiet, N);
                        analyser.print(System.out);
                    }
                }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
}
