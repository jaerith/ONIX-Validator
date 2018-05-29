package bn.com.onix.validation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

/**
 * OnixValidator --- This program will do the work of validating a set of ONIX files found in a specific directory, 
 *                   editing them when appropriate (like changing a HTTP URL for a DTD to a local URL).
 * @author    Aaron Kendall
 */
public class OnixValidator {
	
	public static final char FS_SEP = File.separatorChar;
	public static final char RS_SEP = '/';
	
	public  static String NWL_CHAR = "\n";
	private static String OS_NAME  = System.getProperty("os.name").toLowerCase(); 
		        
	public static final String CONST_DEFAULT_CFG_DIR  = "cfg";
	public static final String CONST_DEFAULT_LOG_DIR  = "log";
	
	public static final String CONST_DEFAULT_ONIX_DTD_URL_BASE = "http://www.editeur.org/onix/";
    
    private static boolean s_bDebug  = false;
    
    private static int  s_mnPurgeLogsOlderThanNumDays = 7;
	private static int  s_mnMaxEntityCount            = 64000;

    private static Properties s_oProperties  = new Properties();
    
    public static Logger s_oSearchExtractLog = null;

/**
 * This method is the main thread of the program.  After initializing its members via the *.properties files,
 * it will operate in the following manner:
 *
 * 1.) It will iterate through the Inbox directory, examining each file and expecting that file to be an ONIX XML file:
 *   a. )For that file, it will attempt to edit the DOCTYPE tag inside and point the DTD URL to a specified local directory.
 *   b.) For that file, it will then attempt to validate.
 *   c.) Valid files will go into the Output directory; invalid files will go into the Failed directory.
 * 
 * 2.) Any debug and error messages will be written to the specified log.
 *
 * @param  args the command line arguments given to the program
 * @return      void
 * @see         String
 */	
	public static void main(String[] args) {	
		
		boolean  bInitMembers     = true;
        String   sInboxDir        = "Y:\\repository";
        String   sOutputDir       = "";
        String   sFailedDir       = "";
        String   sLocalDtdRoot    = "";
        
        // Pre-initialization (i.e., setting the appropriate configuration for logging)
        try {
        	
        	NWL_CHAR = System.getProperty("line.separator");
        	
			String      sEnvironment   = System.getProperty("env", "DEV");
			String      sLogPropsPath  = "";			
			Properties  LoggerProps    = new Properties();
			InputStream ResourceStream = null;

        	System.out.println("DEBUG: Loading the (" + sEnvironment + ") properties for logging.");
			
			if ((sEnvironment != null) && !sEnvironment.isEmpty()) {
												
				sLogPropsPath  = RS_SEP + sEnvironment + RS_SEP + "log4j.properties";
				System.out.println("DEBUG: Attempting to load (" + sEnvironment + ") log props as resource from (" + sLogPropsPath + ").");
				ResourceStream = OnixValidator.class.getResourceAsStream(sLogPropsPath);			    
			}
			
			if (ResourceStream != null) {
				LoggerProps.load(ResourceStream);
				PropertyConfigurator.configure(LoggerProps);
				ResourceStream.close();
			}
			
		    s_oSearchExtractLog = LoggerFactory.getLogger(OnixValidator.class);
		    
		    System.setOut(createInfoLoggingProxy(System.out,  s_oSearchExtractLog));
	        System.setErr(createErrorLoggingProxy(System.err, s_oSearchExtractLog));
		    
	    } catch (Exception e) {
			System.out.println(getStackTrace(e));
	    }
        
        // Initialization
		try {
						
			s_oSearchExtractLog.info("The search data extractor is initializing itself.");
			System.out.println("The search data extractor is initializing itself.");
		    
			initMembers();
						
		} catch (FileNotFoundException e) {
			
			String sErrMsg = getStackTrace(e); 
            bInitMembers   = false;
            
            System.out.println(sErrMsg);
            logFatal(sErrMsg);
            
        } catch (IOException e) {
        	
        	String sErrMsg = getStackTrace(e);        	
			bInitMembers   = false;
			
			System.out.println(sErrMsg);
			logFatal(sErrMsg);
			
        } catch (Exception e) {

            String sErrMsg = getStackTrace(e);        	
			bInitMembers   = false;
			
			System.out.println(sErrMsg);
			logFatal(sErrMsg);
			
        }
		finally {
			if (!bInitMembers)
				System.exit(1);
	    }
		
		s_oSearchExtractLog.info("The ONIX Validator is starting.");		
        		
		try {
			
	        sInboxDir     = s_oProperties.getProperty("inputDir");
	        sOutputDir    = s_oProperties.getProperty("outputDir");
			sFailedDir    = s_oProperties.getProperty("failedDir");
			sLocalDtdRoot = s_oProperties.getProperty("localDtdRoot");

	        if ((args.length >= 1) && !args[0].isEmpty()) {
	        	
    	        // s_bDebug = true;

	            sInboxDir = args[0];
	
	            if ((args.length >= 2) && !args[1].isEmpty()) {
	
	            	sOutputDir = args[1];
	            	
		            if ((args.length >= 2) && !args[1].isEmpty()) {
		            	
		            	sFailedDir = args[2];
		            }	            	
	            }
	        }

	   	    validateDirectory(sInboxDir,  "Inbox");
	   	    validateDirectory(sOutputDir, "Output");
	   	    validateDirectory(sFailedDir, "Failed");	   	    
	   	    
		    try {
		    	String sPurgeLogsOlderThanNumOfDays = s_oProperties.getProperty("purgeLogsOlderThanNumOfDays");
		    	
		    	s_mnPurgeLogsOlderThanNumDays = Integer.parseInt(sPurgeLogsOlderThanNumOfDays);
		    }
	        catch (Exception e) {
	        	s_mnPurgeLogsOlderThanNumDays = 30;
	        }
			
		    try {
		    	String sMaxEntityCount = s_oProperties.getProperty("maxEntityCount");
		    	
		    	s_mnMaxEntityCount = Integer.parseInt(sMaxEntityCount);
		    	
		    	System.setProperty("jdk.xml.entityExpansionLimit", sMaxEntityCount);
		    }
	        catch (Exception e) {
	        	s_mnMaxEntityCount = 64000;
	        }			
		    
		    if ((sLocalDtdRoot != null) && !sLocalDtdRoot.isEmpty()) {
		    	
		    	replaceDtdReferences(sInboxDir, CONST_DEFAULT_ONIX_DTD_URL_BASE, sLocalDtdRoot);
		    	
		    	validateOnixFiles(sInboxDir, sOutputDir, sFailedDir);
		    }
		    		    			    		    		  
		} catch (ParserConfigurationException e) {
		    logException(e);
		} catch (XMLStreamException e) {
			logException(e);
		} catch (org.xml.sax.SAXException e) {
			logException(e);
		} catch (MalformedURLException e) {
			logException(e);	    
		} catch (IOException e) {
			logException(e);
	    } catch (Exception e) {
	    	logException(e);
		}
		
		s_oSearchExtractLog.info("The ONIX Validator is complete.");		
	}
	
    /**
     * Returns a PrintStream object that will be the proxy for making all relevant 
	 * output for the occurrence of an error.
	 * <p>
	 * This method will work as long as 'poLogger' endures during the life of the program.
	 *
	 * @param  poPrintStream IO stream to the new file, in which we are writing errors during this instance of the program
	 * @param  poLogger the general log file for the program
	 * @return the IO stream that now wraps all desired forms of output on the behalf of an error
	 * @see    PrintStream
	 */	
	public static PrintStream createErrorLoggingProxy(final PrintStream poPrintStream, final Logger poLogger) {
    	
        return new PrintStream(poPrintStream) {
        	
            public void print(final String psLogMsg) {
            	poPrintStream.print(psLogMsg);
            	poLogger.error(psLogMsg);
            }
        };
    }	

	/**
	 * Returns a PrintStream object that will be the proxy for making all relevant 
	 * output for the occurrence of an info (i.e., debug) message.
	 * <p>
	 * This method will work as long as 'poLogger' endures during the life of the program.
	 *
	 * @param  poPrintStream IO stream to the new file, in which we are writing info messages during this instance of the program
	 * @param  poLogger the general log file for the program
	 * @return the IO stream that now wraps all desired forms of output on the behalf of an info message
	 * @see    PrintStream
	 */		
    public static PrintStream createInfoLoggingProxy(final PrintStream poPrintStream, final Logger poLogger) {
    	
        return new PrintStream(poPrintStream) {
        	
            public void print(final String psLogMsg) {
            	poPrintStream.print(psLogMsg);
            	poLogger.info(psLogMsg);
            }
        };
    }

	public static String getStackTrace(final Throwable throwable) {
		
	     final StringWriter sw = new StringWriter();
	     final PrintWriter  pw = new PrintWriter(sw, true);
	     
	     throwable.printStackTrace(pw);
	     
	     return sw.getBuffer().toString();
	}
	
    /**
	 * Attempts to initialize the member properties using the 'OnixValidate.properties' file in a specified directory.
	 *
	 * @param  psConfigDirectory Directory which supposedly contains the properties file
	 * @return None
	 * @see    IOException 
	 */		
	public static void initPropertiesFromFiles(String psConfigDirectory) 
			throws IOException {
		
		String sSearchPropsURL = psConfigDirectory + FS_SEP + "OnixValidate.properties";
		
		s_oSearchExtractLog.debug("Program Properties file set to (" + sSearchPropsURL + ")");
		
		try (FileInputStream fProperties = new FileInputStream(sSearchPropsURL)) {				
			s_oProperties.load(fProperties);				
		}
    }

    /**
	 * Attempts to initialize the member properties in three prioritized attempts:
	 *
	 * 1.) From a properties file inside a config directory (specified by the ONIX_VALIDATE_CFG environment variable)
	 * 2.) From the resources file bundled with the application
	 * 3.) From a properties file inside a default config directory
	 * <p>
	 * #2 will be the most likely candidate used.
	 *
	 * @return None
	 * @see    IOException 
	 */		
	public static void initMembers() 
			throws IOException {
		
		String sDebugMode    = "";
		String sInputDir     = "";
		String sOutputDir    = "";
		String sFailedDir    = "";
		String sLocalDtdRoot = "";
		String sPLNumOfDays  = "";
		String sMaxEntityCnt = "64000";
		File   oLocalCfgDir  = new File(CONST_DEFAULT_CFG_DIR);
		
		StringBuilder       sbPropListing = new StringBuilder("");
		Map<String, String> envVars       = System.getenv();

		String sOnixValidateCfgDir = envVars.get("ONIX_VALIDATE_CFG");				
		if ((sOnixValidateCfgDir != null) && !sOnixValidateCfgDir.isEmpty())
			initPropertiesFromFiles(sOnixValidateCfgDir);
		else {
			
			boolean bInitFromResources = initPropertiesFromResources();

			if (!bInitFromResources) {
				if (oLocalCfgDir.exists() && oLocalCfgDir.isDirectory())
					initPropertiesFromFiles(CONST_DEFAULT_CFG_DIR);
			}
		}
		
		sDebugMode    = s_oProperties.getProperty("debugMode");
		sInputDir     = s_oProperties.getProperty("inputDir");
		sOutputDir    = s_oProperties.getProperty("outputDir");
		sFailedDir    = s_oProperties.getProperty("failedDir");
		sLocalDtdRoot = s_oProperties.getProperty("localDtdRoot");
		sPLNumOfDays  = s_oProperties.getProperty("purgeLogsOlderThanNumOfDays");
		sMaxEntityCnt = s_oProperties.getProperty("maxEntityCount");

		sbPropListing.append("\n----------\n");
		sbPropListing.append("PROPERTIES:\n");

		sbPropListing.append("Name(debugMode)          : (" + sDebugMode + ")\n");
		sbPropListing.append("Name(inputDir)           : (" + sInputDir + ")\n");
		sbPropListing.append("Name(outputDir)          : (" + sOutputDir + ")\n");
		sbPropListing.append("Name(failedDir)          : (" + sFailedDir + ")\n");
		sbPropListing.append("Name(localDtdRoot)       : (" + sLocalDtdRoot + ")\n");
		sbPropListing.append("Name(purgeLogsOlderThanNumOfDays)    : (" + sPLNumOfDays + ")\n");
		sbPropListing.append("Name(maxEntityCount)     : (" + sMaxEntityCnt + ")\r\n");
		sbPropListing.append("");
		sbPropListing.append("----------\n");
		
		s_bDebug = sDebugMode.equalsIgnoreCase("y"); 
	    
		s_oSearchExtractLog.info(sbPropListing.toString());
	}

    /**
	 * Attempts to initialize the member properties from resource files packaged within the application.
	 *
	 * @return boolean Indicates whether or not the properties were successfully initialized
	 * @see    IOException 
	 */	
	public static boolean initPropertiesFromResources() 
			throws IOException {
		
		boolean     bSuccess        = true;
		String      sEnvironment    = System.getProperty("env", "DEV");
		String      sResourcePath   = "";
		InputStream oResourceStream = null;
		
		if ((sEnvironment != null) && !sEnvironment.isEmpty()) {
			
			// Retrieve the program's property values for execution
			sResourcePath   = RS_SEP + sEnvironment + RS_SEP + "OnixValidate.properties";
		    oResourceStream = OnixValidator.class.getResourceAsStream(sResourcePath);
			
		    if (oResourceStream != null) {
				s_oSearchExtractLog.debug("Program Properties file set to (" + sResourcePath + ")");
				
				try {				
					s_oProperties.load(oResourceStream);
				}
				finally {
                    try { oResourceStream.close(); }
                    catch (Exception e) {}
			    }
		    }
		    else
		    	bSuccess = false;			
		}
		else {
			bSuccess = false;
		}
				
		return bSuccess;
    }
	
	/**
	 * Indicates whether or not the current platform is a Windows machine.
	 *
	 * @return boolean Indicates whether or not the properties were successfully initialized
	 */	
	public static boolean isWindows() {
        return (OS_NAME.indexOf("win") >= 0);
    }	

    public static void logDebug(String psDebugMsg) {
    	s_oSearchExtractLog.debug(psDebugMsg);
    }
    
    public static void logError(String psErrMsg) {
    	s_oSearchExtractLog.error(psErrMsg);
    }
    
    public static void logException(Exception poException) {

    	String sStackTrace = getStackTrace(poException);     	
    	logException(sStackTrace);
    }
    
    public static void logException(String psExceptionStackTrace) {    	
    	logError(psExceptionStackTrace);
    }    
    
    public static void logFatal(String psFatalMsg) {
    	// NOTE: Due to an issue with FATAL methods in log4j, we must resort to using the "error()" method
    	s_oSearchExtractLog.error("FATAL!  " + psFatalMsg);
    }        

    public static void logInfo(String psInfoMsg) {
    	s_oSearchExtractLog.info(psInfoMsg);
    }
    
	/**
	 * Iterates through files (supposedly ONIX) specified in a directory, replacing the original URL 
	 * mentioned in the DOCTYPE tag with an alternate one.
	 * <p>
	 * Since the EDITEUR organization no longer supports online validation (via DTD HTTP URLs pointing to their site), 
	 * the onus is upon the validator to place the DTD files on a local drive.  However, upon creating ONIX files, 
	 * vendors still place the HTTP URL in the DOCTYPE tag as a default value.  So, the validator must replace the default value
	 * with a URL that points to a local copy of the DTD instead.
	 *
	 * @param  psInboxDir the directory that contains all of our ONIX files to validate
	 * @param  psSearchUrl the substring of the URL to replace (Ex. "http://www.editeur.org/onix")
	 * @param  psReplaceUrl the string to replace the specified substring (Ex. "Y:/onix_dtds")
	 * @return None
	 */		    
    private static void replaceDtdReferences(String psInboxDir, String psSearchUrl, String psReplaceUrl)
    		throws FileNotFoundException, IOException, XMLStreamException, Exception {
    	
    	validateDirectory(psReplaceUrl, "Local ONIX Root");
    	
    	File   oTargetDir   = new File(psInboxDir);		    	
		File[] aSourceFiles = oTargetDir.listFiles();
				
		for (File onixFile : aSourceFiles) {

			if (onixFile.isFile()) {
				
				File onixTmpFile = new File(onixFile.getAbsoluteFile() + ".tmp");
				
                try (FileReader fr = new FileReader(onixFile))
                { 
                	try (FileOutputStream onixTmpStream = new FileOutputStream(onixTmpFile))
                	{ 
	                	boolean bFoundDTD = false;
	                	
		                String sTempLine = "";
		                
		                try (BufferedReader br = new BufferedReader(fr)) {
		
		                    while ((sTempLine = br.readLine()) != null) {
		                    	
	                        	if (!bFoundDTD) {
	                        		bFoundDTD = sTempLine.contains("DOCTYPE");
	                        		
	                        		if (bFoundDTD)
	                        			sTempLine = sTempLine.replace(psSearchUrl, psReplaceUrl) + NWL_CHAR;
	                        		else
	                        			sTempLine += NWL_CHAR;
	                        	}
	                        	else
	                        		sTempLine += NWL_CHAR;
	                        	
	                        	byte[] contentInBytes = sTempLine.getBytes();
	
	                        	onixTmpStream.write(contentInBytes);
		                    }
		                }
                	}
                }
				
                FileUtils.forceDelete(onixFile);
                
				FileUtils.moveFile(onixTmpFile, onixFile);
			}
	    }    	
    }
    
	/**
	 * Iterates through files (supposedly ONIX) specified in a directory, validating each one in turn.
	 * <p>
	 * This function will validate each ONIX file (with respect to its specified DTD).  Valid files 
	 * will be moved to a success directory while invalid files will be moved to a separate failed directory.
	 *
	 * @param  psInboxDir the directory that contains all of our ONIX files to validate
	 * @param  psOutputDir the directory to which all valid ONIX files will be moved
	 * @param  psFailedDir the directory to which all invalid ONIX files will be moved
	 * @return None
	 */		    
    private static void validateOnixFiles(String psInboxDir, String psOutputDir, String psFailedDir)
    		throws FileNotFoundException, IOException, XMLStreamException, ParserConfigurationException, SAXException {
        	
        File   oTargetDir   = new File(psInboxDir);
        File   oSuccessDir  = new File(psOutputDir);
        File   oFailedDir   = new File(psFailedDir);
    	File[] aSourceFiles = oTargetDir.listFiles();
    		
    	for (File onixFile : aSourceFiles) {
    		
    		if (onixFile.isFile()) {

    			try {
		    		if (OnixFileValidator.ValidateFile(onixFile))
		    		{
		    			File validFile = new File(psOutputDir + FS_SEP + onixFile.getName());
		
		    			FileUtils.moveFile(onixFile, validFile);
		    		}
		    		else
		    		{
		    			File invalidFile = new File(psFailedDir + FS_SEP + onixFile.getName());
		
		    			FileUtils.moveFile(onixFile, invalidFile);    			
		    		}
    			}
			    catch (IOException e) {
		        	OnixValidator.logError("ERROR!  Could not correctly parse ONIX file(" + onixFile.getAbsolutePath() + ").");
					OnixValidator.logException(e);
					
	    			File invalidFile = new File(psFailedDir + FS_SEP + onixFile.getName());
	    			FileUtils.moveFile(onixFile, invalidFile);
			    }    			
    		}
    	}    	
    }    

	/**
	 * Ensures that the specified directory exists on the system.
	 *
	 * @param  psTargetDirectory the directory whose existence we wish to validate
	 * @param  psTargetDesc an additional description about the directory for logging purposes
	 * @return None
	 */		        
	private static void validateDirectory(String psTargetDirectory, String psTargetDesc) 
			throws Exception {
		
		File targetDirectory = new File(psTargetDirectory);
		
	    if (!targetDirectory.exists()) {
	        String sErrMsg = psTargetDesc + " Directory (" + psTargetDirectory + ") does not exist!";
			logFatal(sErrMsg);
	   	    throw new Exception(sErrMsg);
	    }
	    else if (!targetDirectory.isDirectory()) {
	        String sErrMsg = psTargetDesc + " Directory (" + psTargetDirectory + ") does not exist!";
	    	logFatal(sErrMsg);
	    	throw new Exception(sErrMsg);
	    }		
    }    
}

