package com.copp.jmeter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;


/**
 * The Class ProcessJmeterResults.
 */
//@Slf4j
public class ProcessJmeterResults {
	
	/** The jmeter results file. */
	private static File jmeterResultsFile;

	/** The results. */
	private List<JmeterDataSet> results;

	/** The logger. */
	public static Logger logger = Logger.getLogger(ProcessJmeterResults.class);

	/** The constant wait. */
	private final int constantWait, numThreads;

	/** The parse samples. */
	private final boolean parseSamples;

	/** The test type. */
	private final String environment;
	
	/** The test type. */
	private final String testType;
	
	
	/** The db snapshot url. */
	private final String dbSnapshotUrl;
	
	/** The project. */
	private final String project;

	/**
	 * Instantiates a new process jmeter results.
	 * 
	 * @param input the input
	 * @param wait the wait
	 */
	public ProcessJmeterResults(File input, int wait) {
		ProcessJmeterResults.jmeterResultsFile = input;
		this.constantWait = wait;
		this.numThreads = 0;
		this.results = new ArrayList<JmeterDataSet>();
		this.parseSamples = false;
		this.environment=null;
		this.testType=null;
		this.dbSnapshotUrl=null;
		this.project=null;
		logger.trace(getClass().getName() + " instance created");
	}

	/**
	 * Instantiates a new process jmeter results.
	 * 
	 * @param input the input
	 * @param wait the wait
	 * @param parseSamples the parse samples
	 */
	public ProcessJmeterResults(File input, int wait, boolean parseSamples) {
		ProcessJmeterResults.jmeterResultsFile = input;
		this.constantWait = wait;
		this.numThreads = 0;
		this.results = new ArrayList<JmeterDataSet>();
		this.parseSamples = parseSamples;
		this.environment=null;
		this.testType=null;
		this.dbSnapshotUrl=null;
		this.project=null;
		logger.trace(getClass().getName() + " instance created");
	}

	/**
	 * Instantiates a new process jmeter results.
	 * 
	 * @param input the input
	 * @param wait the wait
	 * @param environment the environment
	 * @param testType the test type
	 * @param numThreads the num threads
	 * @param project the project
	 */
	public ProcessJmeterResults(File input, int wait, int numThreads, String environment, String testType, String dbSnapshotUrl, String project) {
		ProcessJmeterResults.jmeterResultsFile = input;
		this.constantWait = wait;
		this.numThreads = numThreads;
		this.results = new ArrayList<JmeterDataSet>();
		this.parseSamples = false;
		this.environment=environment;
		this.testType=testType;
		this.dbSnapshotUrl=dbSnapshotUrl;
		this.project=project;
		logger.trace(getClass().getName() + " instance created");
	}

	/**
	 * Gets the results.
	 * 
	 * @return the results
	 */
	public List<JmeterDataSet> getResults() {
		return this.results;
	}

	/**
	 * Validate results file is ready to be parsed.
	 * 
	 * @param file the file
	 * 
	 * @return true, if successful
	 */
	private boolean validateResultsFile(File file) {
		if(file.exists() && file.canRead()) {
			return true;
		}
		return false;
	}

	/**
	 * Parses the jmeter results file, looking for either transactions or samples.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void parseResultsFile() throws IOException {
		if(!validateResultsFile(jmeterResultsFile)) {
			logger.error("ERROR: Jmeter results file not found: " + jmeterResultsFile);
			System.exit(0);
		}
			
		BufferedReader buffer = new BufferedReader(new FileReader(jmeterResultsFile));
		String line = null;
		while ((line = buffer.readLine()) != null) {
			if (parseSamples) {
				if(line.contains("<httpSample")) {
					parseTransaction(line);
				}else if (line.contains("<sample")) {
					parseTransaction(line);
				}
			}else if (line.contains("<sample"))
				parseTransaction(line);
		}
		buffer.close();
	}

	/**
	 * Parses parse a line which begins with "<sample". This represents a transaction.
	 * 
	 * @param line the line
	 */
	private void parseTransaction(String line) {
		line = line.trim();
		String[] data = line.split(" ");
		String label = "";
		logger.debug(line);
		logger.debug("Length for current line: " + data.length);
		boolean success = checkForSuccess(data[4]);
		long timestamp = Long.parseLong(removeQuotes(data[3]));

		//since we are splitting on ' ' characters, the value for "lb" will be split into muliple tokens if
		//the name has spaces in it (which it will). Concatenate the values for each of these tokens.
		for(int x = 5;x<data.length;x++) {
		if(data[x].contains("rc=")) {break;}
		label += data[x] + " ";
		}

		label = removeSpecialCharacters(removeQuotes(label));
		label = label.trim();
		logger.debug("lb: " + label);

		//add the test if it does not currently exist
		if(!checkDataSet(label)) {
			this.results.add(new JmeterDataSet(label));
			getDataSet(label).setMinTimeStamp(timestamp);
			getDataSet(label).setMaxTimeStamp(timestamp);
		}

		//if the test exists, add the current data for it
		if(checkDataSet(label)) {
			Integer value = new Integer(removeQuotes(data[1]));
			logger.debug(value - this.constantWait);

			//we need to subtract the constant wait or the entire transaction will
			//have incorrectly inflated values.
			if (label.contains("AllSteps"))
				getDataSet(label).addData(value - (this.constantWait*(getDataSetCount(label)-1)));
			else 
				getDataSet(label).addData(value - this.constantWait);

			//if the transaction was successful, increment success count
			if(success) {getDataSet(label).incrementSuccessCount();}

			//set the maximum and minimum time values for each sample
			if (timestamp < getDataSet(label).getMinTimeStamp())
				getDataSet(label).setMinTimeStamp(timestamp);
			if (timestamp > getDataSet(label).getMaxTimeStamp())
				getDataSet(label).setMaxTimeStamp(timestamp);

			//set throughput
			getDataSet(label).setThroughput();

			logger.debug(getDataSet(label).getName() + ":" + getDataSet(label).getCount()); 
		}
	}

	/**
	 * Check for successful transaction.
	 * 
	 * @param str the str
	 * 
	 * @return true, if successful
	 */
	private boolean checkForSuccess(String str) {
		Pattern pattern = Pattern.compile("s=\"true\"");
		Matcher matcher = pattern.matcher(str);
		if(matcher.find()) {return true;}
		return false;
	}

	/**
	 * Clean unwanted chars from data.
	 * 
	 * @param data the data
	 * 
	 * @return the string
	 */
	private static String removeQuotes(String data) {
		data = data.substring(data.indexOf('"')+1,data.length()-1);
		logger.debug(data);
		return data;
	}

	/**
	 * Clean name.
	 * 
	 * @param name the name
	 * 
	 * @return the string
	 */
	private static String removeSpecialCharacters(String name) {
		Pattern pattern = Pattern.compile("[\\<\\>\\%\\\\$]");
		Matcher matcher = pattern.matcher(name);
		name = matcher.replaceAll("");
		return name.replace("\"", "");
	}

	/**
	 * Check if test name is already in results data set.
	 * 
	 * @param name the name
	 * 
	 * @return true, if successful
	 */
	private boolean checkDataSet(String name) {
		for(Iterator<JmeterDataSet> iterate = this.results.iterator();iterate.hasNext();) {
			JmeterDataSet dataSet = iterate.next();
			if(dataSet.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the data set count.
	 * 
	 * @return the data set count
	 */
	private int getDataSetCount(String label) {
		String[] name = label.split(" ");
		String userStory = name[0];

		int count = 0;
		for(Iterator<JmeterDataSet> iterate = this.results.iterator();iterate.hasNext();) {
			JmeterDataSet dataSet = iterate.next();
			if(dataSet.getName().matches(userStory+"\\s.*") && dataSet.getName().contains("Transaction Controller"))
				count ++;
		}
		return count;
	}

	/**
	 * Gets a specific data set.
	 * 
	 * @param name the name
	 * 
	 * @return the data set
	 */
	private JmeterDataSet getDataSet(String name) {
		for(Iterator<JmeterDataSet> iterate = this.results.iterator();iterate.hasNext();) {
			JmeterDataSet dataSet = iterate.next();
			if(dataSet.getName().equals(name)) {
				return dataSet;
			}
		}
		return null;
	}

	/**
	 * Process transactions.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void processTransactions() throws IOException {
		JmeterDataSet dataSet = null;
		logger = Logger.getLogger("com.psi.qa.utils.ProcessJmeterResults");
		logger.debug(logger.getName());
		FileAppender appender = (FileAppender)logger.getAppender("PROCESS_JMETER_RESULTS");
		appender.setFile(ProcessJmeterResults.jmeterResultsFile + ".out");
		appender.activateOptions();

		logger.info("---------------------------------------------------");
		logger.info("---------------------- Reports --------------------");
		logger.info("---------------------------------------------------");
		for(Iterator<JmeterDataSet> iterate = getResults().iterator();iterate.hasNext();) {
			dataSet = iterate.next();
			if(dataSet.getName().contains("Transaction"))
				printStatistics(dataSet);
		}
		logger.info("--------------------- Totals ----------------------");
		logger.info("---------------------------------------------------");
		double totalSamples = 0;
		double totalSuccesses = 0;
		double totalThroughput = 0;
		for(Iterator<JmeterDataSet> iterate = getResults().iterator();iterate.hasNext();) {
			dataSet = iterate.next();
			totalSamples += dataSet.getCount();
			totalSuccesses += dataSet.getSuccessCount();
			totalThroughput += dataSet.getThroughput();
		}
		double totalPercentage = totalSuccesses/totalSamples;
		logger.info("       Total Successes: " + totalSuccesses);
		logger.info("         Total Samples: " + totalSamples);
		logger.info(" Total Requests/second: " + totalThroughput + " req/s");
		logger.info("       Total Success %: " + totalPercentage * 100 + "%");

		logger.info("---------------------------------------------------");
		logger.info("-------------------- EXCEL Format -----------------");
		logger.info("---------------------------------------------------");
		logger.info("Test Name|Max|Min|Arith Mean|Geo Mean|Median|Std Dev|90%|Success Cnt|Sample Cnt|Req/sec|Success %|");
		for(Iterator<JmeterDataSet> iterate = getResults().iterator();iterate.hasNext();) {
			dataSet = iterate.next();
			if(dataSet.getName().contains("Transaction"))
				printXlsStatistics(dataSet);
		}
		logger.info("Totals||||||||"+totalSuccesses+"|"+totalSamples+"|"+totalThroughput+"|"+(totalSuccesses/totalSamples) * 100+"%"+"|");
	}

	/**
	 * Prints the statistics.
	 * 
	 * @param dataSet the data set
	 */
	private void printStatistics(JmeterDataSet dataSet) {
		logger.info("             Test Name: " + dataSet.getName());
		logger.info("         Maximum value: " + (dataSet.getMaximumValue()/1000) +"s");
		logger.info("         Minimum value: " + (dataSet.getMinimumValue()/1000) +"s");
		logger.info("       Arithmetic Mean: " + (dataSet.getMean()/1000) +"s");
		logger.info("        Geometric Mean: " + (dataSet.getGeometricMean()/1000) +"s");
		logger.info("                Median: " + (dataSet.getMedian()/1000) +"s");
		logger.info("    Standard Deviation: " + (dataSet.getStandardDeviation()/1000) +"s");
		logger.info("        90% Percentile: " + (dataSet.getPercentile(90)/1000) +"s");
		logger.info("         Success Count: " + dataSet.getSuccessCount());
		logger.info("          Sample Count: " + dataSet.getCount());
		logger.info("         Max Timestamp: " + dataSet.getMaxTimeStamp());
		logger.info("         Min Timestamp: " + dataSet.getMinTimeStamp());
		logger.info("               Req/sec: " + dataSet.getThroughput() + " req/s");
		logger.info("               Success: " + (dataSet.getSuccessCount()/dataSet.getCount()) * 100 + "%");
		logger.info("---------------------------------------------------");
	}

	/**
	 * Prints the xls statistics.
	 * 
	 * @param dataSet the data set
	 */
	private void printXlsStatistics(JmeterDataSet dataSet) {
		String pipe = "|";
		logger.info(dataSet.getName()
				+pipe+(dataSet.getMaximumValue()/1000)
				+pipe+(dataSet.getMinimumValue()/1000)
				+pipe+(dataSet.getMean()/1000)
				+pipe+(dataSet.getGeometricMean()/1000)
				+pipe+(dataSet.getMedian()/1000)
				+pipe+(dataSet.getStandardDeviation()/1000)
				+pipe+(dataSet.getPercentile(90)/1000)
				+pipe+dataSet.getSuccessCount()
				+pipe+(int)dataSet.getCount()
				+pipe+dataSet.getThroughput()
				+pipe+((dataSet.getSuccessCount()/dataSet.getCount()) * 100) + "%"+pipe);
	}

	/**
	 * Save results to database.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void saveResultsToDatabase() throws IOException {
		JmeterDataSet dataSet = null;

		for(Iterator<JmeterDataSet> iterate = getResults().iterator();iterate.hasNext();) {
			dataSet = iterate.next();
			if(dataSet.getName().contains("Transaction Controller")) {
				//UserStory StepName UserStoryCategory TestCategory StepOrder
				String[] name = dataSet.getName().split(" ");

				String userStory = null;
				String stepName = null;
				int stepOrder = 999;
				String category = null;
				String requestType = null;
				try {
					userStory = name[0];
					stepName = name[1];
					stepOrder = new Integer(name[2]);
					category = name[3];
					requestType = name[4];
				} catch (Exception e) {
					logger.error("Trying to split: " + dataSet.getName());
					e.printStackTrace();
				}
				double maxValue = dataSet.getMaximumValue()/1000;
				double minValue = dataSet.getMinimumValue()/1000;
				double arithMean = dataSet.getMean()/1000;
				double geoMean = dataSet.getGeometricMean()/1000;
				double median = dataSet.getMedian()/1000;
				double stdDev = dataSet.getStandardDeviation()/1000;
				double ninetyPercent = dataSet.getPercentile(90)/1000;
				double totalRequests = dataSet.getCount();
				double testDuration = (dataSet.getMaxTimeStamp() - dataSet.getMinTimeStamp())/60;
				long starttime = dataSet.getMinTimeStamp();
				double throughput = dataSet.getThroughput();
				double successPercent = (dataSet.getSuccessCount()/dataSet.getCount()) * 100;

				try {
					SqlUtils.executeJmeterUpdate(this.project, this.environment, this.testType, this.constantWait, this.numThreads, 
							category, requestType, userStory, stepName, stepOrder, 
							minValue, maxValue, arithMean, geoMean, median, stdDev, ninetyPercent, 
							totalRequests, testDuration, starttime, throughput, successPercent, this.dbSnapshotUrl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Process all transactions found.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void process() throws IOException {
		parseResultsFile();
		processTransactions();
		if (this.environment !=null && this.testType!=null) {
			saveResultsToDatabase();
		}
	}

	/**
	 * The main method.
	 * 
	 * @param args the arguments
	 * 
	 * @throws Exception the exception
	 */
	public static void main(String[] args) throws Exception {
		if(args.length < 2 || args.length == 4 || args.length == 5 || args.length == 6 || args.length > 7) {
			System.out.println("USAGE: ProcessJmeterResults <(1)jmeter.jtl> <(2)constant-wait-time (ms)>");
			System.out.println("---OR---");
			System.out.println("USAGE: ProcessJmeterResults <(1)jmeter.jtl> <(2)constant-wait-time (ms)> <(3)true - parse " +
					"http samples as well as transaction samples>");
			System.out.println("---OR---");
			System.out.println("USAGE: ProcessJmeterResults <(1)jmeter.jtl> <(2)constant-wait-time (ms)> <(3)number of threads> " +
					"<(4)env (dev/qa)> <(5)test type (perf/soak)> <(6)db snapshot url (ks/gef)> <(7)project (ks/gef)>");
			System.exit(0);
		}
		if(args.length == 2) {
			ProcessJmeterResults process = new ProcessJmeterResults(new File(args[0]),new Integer(args[1]));
			process.process();
		}else if(args.length == 3 && args[2].equals("true")) {
			ProcessJmeterResults process = new ProcessJmeterResults(new File(args[0]),new Integer(args[1]),new Boolean(args[2]));
			process.process();
		}else if(args.length == 7 && 
				(args[3].equals("dev") || args[3].equals("qa")) && 
				(args[4].equals("perf") || args[4].equals("soak") && 
				(args[5] != "" || args[5] != null ) && 
				(args[6] != "" || args[6] != null ))) {
			ProcessJmeterResults process = new ProcessJmeterResults(new File(args[0]),new Integer(args[1]),new Integer(args[2]),
					new String(args[3]),new String(args[4]), new String(args[5]), new String(args[6]));
			process.process();
		}else {
			System.out.println("USAGE: ProcessJmeterResults <(1)jmeter.jtl> <(2)constant-wait-time (ms)>");
			System.out.println("---OR---");
			System.out.println("USAGE: ProcessJmeterResults <(1)jmeter.jtl> <(2)constant-wait-time (ms)> <(3)true - parse " +
					"http samples as well as transaction samples>");
			System.out.println("---OR---");
			System.out.println("USAGE: ProcessJmeterResults <(1)jmeter.jtl> <(2)constant-wait-time (ms)> <(3)number of threads> " +
					"<(4)env (dev/qa)> <(5)test type (perf/soak)> <(6)db snapshot url> <(7)project (ga/ks/gef)>");
			System.exit(0);
		}
	}
}