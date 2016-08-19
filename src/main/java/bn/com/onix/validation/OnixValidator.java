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

public class OnixValidator {
	
	public static final char FS_SEP = File.separatorChar;
	public static final char RS_SEP = '/';
	
	public  static String NWL_CHAR = "\n";
	private static String OS_NAME  = System.getProperty("os.name").toLowerCase(); 
		        
	public static final String CONST_DEFAULT_CFG_DIR  = "cfg";
	public static final String CONST_DEFAULT_LOG_DIR  = "log";
	
	public static final String CONST_DEFAULT_ONIX_DTD_URL_BASE = "http://www.editeur.org/onix/";
    
    private static boolean s_bDebug  = false;
    
    private static int     s_mnPurgeLogsOlderThanNumDays = 7;

    private static Properties s_oProperties  = new Properties();
    
    public static Logger s_oSearchExtractLog = null;
    	
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
	
    public static PrintStream createErrorLoggingProxy(final PrintStream poPrintStream, final Logger poLogger) {
    	
        return new PrintStream(poPrintStream) {
        	
            public void print(final String psLogMsg) {
            	poPrintStream.print(psLogMsg);
            	poLogger.error(psLogMsg);
            }
        };
    }	
	
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
	
	public static void initPropertiesFromFiles(String psConfigDirectory) 
			throws IOException {
		
		String sSearchPropsURL = psConfigDirectory + FS_SEP + "OnixValidate.properties";
		
		s_oSearchExtractLog.debug("Program Properties file set to (" + sSearchPropsURL + ")");
		
		try (FileInputStream fProperties = new FileInputStream(sSearchPropsURL)) {				
			s_oProperties.load(fProperties);				
		}
    }

	public static void initMembers() 
			throws IOException {
		
		String sDebugMode    = "";
		String sInputDir     = "";
		String sOutputDir    = "";
		String sFailedDir    = "";
		String sLocalDtdRoot = "";
		String sPLNumOfDays  = "";
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

		sbPropListing.append("\n----------\n");
		sbPropListing.append("PROPERTIES:\n");

		sbPropListing.append("Name(debugMode)          : (" + sDebugMode + ")\n");
		sbPropListing.append("Name(inputDir)           : (" + sInputDir + ")\n");
		sbPropListing.append("Name(outputDir)          : (" + sOutputDir + ")\n");
		sbPropListing.append("Name(failedDir)          : (" + sFailedDir + ")\n");
		sbPropListing.append("Name(localDtdRoot)       : (" + sLocalDtdRoot + ")\n");
		sbPropListing.append("Name(purgeLogsOlderThanNumOfDays)    : (" + sPLNumOfDays + ")\n");
		sbPropListing.append("");
		sbPropListing.append("----------\n");
		
		s_bDebug = sDebugMode.equalsIgnoreCase("y"); 
	    
		s_oSearchExtractLog.info(sbPropListing.toString());
	}
			
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

