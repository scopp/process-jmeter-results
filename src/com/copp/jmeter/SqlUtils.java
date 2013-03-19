package com.copp.jmeter;

import com.copp.jmeter.PropertyUtils;

import java.io.*;
import java.sql.*;

import org.apache.log4j.Logger;

import oracle.jdbc.driver.OracleDriver;


/**
 * The Class SqlUtils.
 */
public class SqlUtils {
	
	/** The logger. */
	public static Logger logger = Logger.getLogger(SqlUtils.class);

	/** The pro util. */
	private static PropertyUtils proUtil = null;

	/** The connection url thin. */
	private static String connectionURLThin= null;

	/** The user id. */
	private static String userID = null;

	/** The user password. */
	private static String userPassword = null;

	/** The basedir. */
	private static String basedir= null;

	/**
	 * Gets the connection.
	 * 
	 * @param project the project
	 * 
	 * @return the connection
	 */
	public static Connection getConnection(String project){
		proUtil = PropertyUtils.getInstance();
		userID = proUtil.getProperty(project + ".results.db.user");
		userPassword = proUtil.getProperty(project + ".results.db.password");
		connectionURLThin= proUtil.getProperty("results.db.url");
		basedir= proUtil.getProperty("scripts.folder");

		Connection cnn= null;
		try {
				DriverManager.registerDriver(new OracleDriver());
				cnn = DriverManager.getConnection(connectionURLThin, userID, userPassword);
			} catch (Exception e) {
				logger.error(e.getMessage()+ " can not build database connection.");
			}
			return cnn;
	}


	/**
	 * Execute jmeter update.
	 * 
	 * @param environment the environment
	 * @param testType the test type
	 * @param constantWait the constant wait
	 * @param numThreads the num threads
	 * @param category the category
	 * @param requestType the request type
	 * @param userStory the user story
	 * @param stepName the step name
	 * @param stepOrder the step order
	 * @param minValue the min value
	 * @param maxValue the max value
	 * @param arithMean the arith mean
	 * @param geoMean the geo mean
	 * @param median the median
	 * @param stdDev the std dev
	 * @param ninetyPercent the ninety percent
	 * @param totalRequests the total requests
	 * @param testDuration the test duration
	 * @param starttime the starttime
	 * @param throughput the throughput
	 * @param successPercent the success percent
	 * @param project the project
	 * @param dbSnapshotUrl the db snapshot url
	 * 
	 * @throws Exception the exception
	 */
	public static void executeJmeterUpdate(String project, String environment, String testType, int constantWait, int numThreads, 
			String category, String requestType, String userStory, String stepName, int stepOrder, 
			double minValue, double maxValue, double arithMean, double geoMean, double median, double stdDev, double ninetyPercent, 
			double totalRequests, double testDuration, long starttime, double throughput, double successPercent, String dbSnapshotUrl) throws Exception{
		Connection cnn = getConnection(project);
		CallableStatement proc = null;
		if(cnn!=null){
			try {
				proc = cnn.prepareCall("{ call write_test_run_prc(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");
			} catch (SQLException e) {
				logger.error(e.getMessage()+ " Can not prepare call for the connection.");
			}
		}
		//example - exec write_test_run_prc('dev','perf','0','100','online','http','24','Login Step 1','1',
		//45.661,8797.154,578.123,453.182,429.482,43.489,689.854,444.449,3600.652,1253632152,50.123,99.549);
		try{
			proc.setString(1, environment);
			proc.setString(2, testType);
			proc.setDouble(3, constantWait);
			proc.setDouble(4, numThreads);
			proc.setString(5, category);
			proc.setString(6, requestType);
			proc.setString(7, userStory);
			proc.setString(8, stepName);
			proc.setString(9, Integer.toString(stepOrder));
			proc.setDouble(10, minValue);
			proc.setDouble(11, maxValue);
			proc.setDouble(12, arithMean);
			proc.setDouble(13, geoMean);
			proc.setDouble(14, median);
			proc.setDouble(15, stdDev);
			proc.setDouble(16, ninetyPercent);
			proc.setDouble(17, totalRequests);
			proc.setDouble(18, testDuration);
			proc.setDouble(19, starttime);
			proc.setDouble(20, throughput);
			proc.setDouble(21, successPercent);
			proc.setString(22, dbSnapshotUrl);
			proc.execute();
		}catch(SQLException e){
			logger.error(e.getMessage()+ " Excute Sql batch fail, it could be caused by invalid Sql. Trying to insert:");
			logger.error("   environment: " + environment);
			logger.error("      testType: " + testType);
			logger.error("  constantWait: " + constantWait);
			logger.error("    numThreads: " + numThreads);
			logger.error("      category: " + category);
			logger.error("   requestType: " + requestType);
			logger.error("     userStory: " + userStory);
			logger.error("      stepName: " + stepName);
			logger.error("     stepOrder: " + Integer.toString(stepOrder));
			logger.error("      minValue: " + minValue);
			logger.error("      maxValue: " + maxValue);
			logger.error("     arithMean: " + arithMean);
			logger.error("       geoMean: " + geoMean);
			logger.error("        median: " + median);
			logger.error("        stdDev: " + stdDev);
			logger.error(" ninetyPercent: " + ninetyPercent);
			logger.error(" totalRequests: " + totalRequests);
			logger.error("     starttime: " + starttime);
			logger.error("    throughput: " + throughput);
			logger.error("successPercent: " + successPercent);
			logger.error(" dbSnapshotUrl: " + dbSnapshotUrl);
		}finally{
			try{
				proc.close();
				cnn.rollback();
				cnn.close();
			}catch(Exception ex){
				logger.error(ex.getMessage()+ " dabase connection close error.");
			}
		}
	}

	/**
	 * Execute script.
	 * 
	 * @param sqlFilePath the sql file path
	 * @param project the project
	 */
	public static void executeScript(String project, String sqlFilePath){
		Connection cnn = getConnection(project);
		Statement statement=null;
		if(cnn!=null){
			try {
				statement = cnn.createStatement();
			} catch (SQLException e) {
				logger.error(e.getMessage()+ " Can not create staement for the connection.");
			}
		}
		sqlFilePath = basedir + "/" + sqlFilePath;
		statement = buildSqlBatchStatement(sqlFilePath,statement);
		try{
			Boolean defaultAutoCommit = cnn.getAutoCommit();
			cnn.setAutoCommit(false);
			statement.executeBatch();
			cnn.commit();
			cnn.setAutoCommit(defaultAutoCommit);
	//		cnn.close();
		}catch(SQLException e){
			logger.error(e.getMessage()+ " Excute Sql batch fail, it could be caused by invalid Sql.");
		}finally{
			try{
				cnn.rollback();
				cnn.close();
			}catch(Exception ex){
				logger.error(ex.getMessage()+ " dabase connection close error.");
			}
		}
	}

	/**
	 * Builds the sql batch statement.
	 * 
	 * @param sqlFilePath the sql file path
	 * @param statement the statement
	 * 
	 * @return the statement
	 */
	private static Statement buildSqlBatchStatement(String sqlFilePath, Statement statement){
		File sqlFile = new File(sqlFilePath);
		InputStreamReader sir = null;
		try{
			sir = new InputStreamReader(new FileInputStream(sqlFile));
		}catch(FileNotFoundException e){
			logger.error(e.getMessage()+ " sql file can not be found. File Name: " + sqlFilePath);
		}
		BufferedReader bufferedReader = new BufferedReader(sir);
		String sqlLine ="";
		try{
			while ((sqlLine=bufferedReader.readLine())!=null){
				sqlLine = sqlLine.trim();
				if(!isSkipping(sqlLine)){
					statement.addBatch(fillEndSymbols(sqlLine));
				}
			}
			bufferedReader.close();
			sir.close();
		}catch(IOException ioe){
			logger.error(ioe.getMessage()+ " Error when reading or closing reader.");
		}catch(SQLException sqle){
			logger.error(sqle.getMessage()+ " Can not add Sql batch into statement.");
		}
		return statement;
	}

	/**
	 * Fill end symbols.
	 * 
	 * @param sqlStr the sql str
	 * 
	 * @return the string
	 */
	private static String fillEndSymbols(String sqlStr){
		int len = sqlStr.indexOf(";");
		if(len>0){
			sqlStr =sqlStr.substring(0, len);
		}
		return sqlStr;
	}

	/**
	 * Checks if is skipping.
	 * 
	 * @param sqlStr the sql str
	 * 
	 * @return the boolean
	 */
	private static Boolean isSkipping(String sqlStr){
		return sqlStr.startsWith("--") || "".equals(sqlStr);
	}
}