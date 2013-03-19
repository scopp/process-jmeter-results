package com.copp.jmeter;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * The Class JmeterDataSet to represent individual Jmeter data.
 */
public class JmeterDataSet {
	/** The name. */
	private String name; //test name

	/** The statistics. */
	private DescriptiveStatistics statistics;

	/** The success count. */
	private double successCount = 0;

	/** The min time stamp. */
	private long minTimeStamp = 0;

	/** The max time stamp. */
	private long maxTimeStamp = 0;

	/** The throughput. */
	private double throughput = 0;

	/**
	 * Instantiates a new jmeter data set.
	 * 
	 * @param name the name
	 */
	protected JmeterDataSet(String name) {
		this.name = name;
		this.statistics = new DescriptiveStatistics(20000000);
		ProcessJmeterResults.logger.trace(getClass().getName() + " instance created");
	}

	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	protected String getName() {
		return this.name;
	}

	/**
	 * Adds the data.
	 * 
	 * @param data the data
	 */
	protected void addData(int data) {
		this.statistics.addValue(new Double(data));
		ProcessJmeterResults.logger.debug("Statistics N: " + this.statistics.getN());
	}

	/**
	 * Sets the min time stamp.
	 * 
	 * @param timestamp the new min time stamp
	 */
	protected void setMinTimeStamp (long timestamp) {
		this.minTimeStamp=timestamp;
	}

	/**
	 * Gets the min time stamp.
	 * 
	 * @return the min time stamp
	 */
	protected long getMinTimeStamp () {
		return this.minTimeStamp;
	}

	/**
	 * Sets the max time stamp.
	 * 
	 * @param timestamp the new max time stamp
	 */
	protected void setMaxTimeStamp (long timestamp) {
		this.maxTimeStamp=timestamp;
	}

	/**
	 * Gets the max time stamp.
	 * 
	 * @return the max time stamp
	 */
	protected long getMaxTimeStamp () {
		return this.maxTimeStamp;
	}

	/**
	 * Gets the throughput.
	 * 
	 * @return the throughput
	 */
	protected double getThroughput() {
		return this.throughput; 
	}

	/**
	 * Sets the throughput.
	 */
	protected void setThroughput() {
		this.throughput = getCount()/((getMaxTimeStamp() - getMinTimeStamp())/1000);
	}

	/**
	 * Gets the count.
	 * 
	 * @return the count
	 */
	protected double getCount() {
		return this.statistics.getN();
	}

	/**
	 * Gets the success count.
	 * 
	 * @return the success count
	 */
	protected double getSuccessCount() {
		return this.successCount;
	}

	/**
	 * Increment success count.
	 */
	protected void incrementSuccessCount() {
		this.successCount ++;
	}

	/**
	 * Gets the geometric mean.
	 * 
	 * @return the geometric mean
	 */
	protected double getGeometricMean() {
		return this.statistics.getGeometricMean();
	}

	/**
	 * Gets the minimum value.
	 * 
	 * @return the minimum value
	 */
	protected double getMinimumValue() {
		return this.statistics.getMin();
	}

	/**
	 * Gets the maximum value.
	 * 
	 * @return the maximum value
	 */
	protected double getMaximumValue() {
		return this.statistics.getMax();
	}

	/**
	 * Gets the mean.
	 * 
	 * @return the mean
	 */
	protected double getMean() {
		return this.statistics.getMean();
	}

	/**
	 * Gets the standard deviation.
	 * 
	 * @return the standard deviation
	 */
	protected double getStandardDeviation() {
		return this.statistics.getStandardDeviation();
	}

	/**
	 * Gets the percentile.
	 * 
	 * @param percentile the percentile
	 * 
	 * @return the percentile
	 */
	protected double getPercentile(double percentile) {
		return this.statistics.getPercentile(percentile);
	}

	/**
	 * Gets the median.
	 * 
	 * @return the median
	 */
	protected double getMedian() {
		return this.statistics.getPercentile(50);
	}
}