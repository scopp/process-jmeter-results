package com.copp.jmeter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * The Class PropertyUtils.
 */
public class PropertyUtils {
	
	/** The logger. */
	public static Logger logger = Logger.getLogger(PropertyUtils.class);
	
	/** The Constant CONF_DIR. */
	private static final String CONF_DIR = "conf";
	
	/** The Constant DEFAULT_PROPS. */
	private static final String DEFAULT_PROPS = "db.properties";

	/** The props. */
	private static Properties theProps = new Properties();

	/** The instance. */
	private static PropertyUtils theInstance = new PropertyUtils();

	/**
	 * Instantiates a new property utils.
	 */
	private PropertyUtils() {
		initProperties(DEFAULT_PROPS);
	}

	/**
	 * Public access method for the single PropertyUtil instance.
	 * Systex:PropertyUtils name = propertyUtils.getInstance();
	 * <p/>
	 * 
	 * @return static PropertyUtils singleton instance
	 */
	public static PropertyUtils getInstance() {
		return theInstance;
	}

	/**
	 * Inits the properties.
	 * 
	 * @param filename the filename
	 */
	private static void initProperties(String filename) {
		try {
			logger.info("DB property file : " + filename);

			InputStream stream = readFileAsStream(filename);
			if (stream != null){
			theProps.load(stream);
			}
		}
		catch (FileNotFoundException foundEx) {
			logger.error(filename + " Not Found!");
		}
		catch (IOException ioEx) {
			String msg = "Failed to load property from " + filename;
			logger.error(msg + ":" + ioEx);
		}
	}

	/**
	 * Read file as stream.
	 * 
	 * @param filename the filename
	 * 
	 * @return the input stream
	 */
	public static InputStream readFileAsStream(final String filename) {
		InputStream stream = null;
		File confProps = new File(CONF_DIR + "/" + filename);

		if (confProps.exists())
		{
			logger.debug("File [" + filename + "] found under [" + CONF_DIR + "]");
		try {
			stream = new FileInputStream(confProps);
		} catch (FileNotFoundException e) {
			logger.error(filename + " Not Found!");
		}

		return stream;
		}

		// file does not exist under domain root or Eclipse Project root	 
		logger.debug("File [" + filename + "] loaded from ResourceLocator Classpath");

		ClassLoader classLoader = PropertyUtils.class.getClassLoader();
		stream = classLoader.getResourceAsStream(filename);

		return stream;
	}

	/**
	 * Gets the property.
	 * 
	 * @param property the property
	 * 
	 * @return the property
	 */
	public String getProperty(String property) {
		return theProps.getProperty(property);
	}

	/**
	 * Inits the properties.
	 */
	synchronized public static void initProperties(){
		initProperties(DEFAULT_PROPS);
	}

	/**
	 * Inits the properties.
	 * 
	 * @param props the props
	 */
	synchronized public static void initProperties(Properties props) {
		try {
			theProps = props;
		}
		catch (Exception e) {
			logger.error(e.getMessage()+ " encountered an error re-initializing properties");

		}
	}
}