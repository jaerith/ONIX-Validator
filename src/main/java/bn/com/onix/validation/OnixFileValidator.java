package bn.com.onix.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * OnixFileValidator --- This simple class will validate a XML file against its respective DTD/XSD.
 * @author    Aaron Kendall
 */
public class OnixFileValidator {

	private static Object OnixFileLock = new Object();
	
	private static boolean OnixFileIsValid = true;
	private static String  CurrentOnixFile = "";
	
	public static boolean ValidateFile(File pOnixFile) 
			throws org.xml.sax.SAXException, java.io.IOException, javax.xml.parsers.ParserConfigurationException  {

		synchronized (OnixFileLock) {
			
		    OnixFileIsValid = true;
		    CurrentOnixFile = pOnixFile.getAbsolutePath();
		    			
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		    domFactory.setValidating(true);
		  
		    DocumentBuilder builder = domFactory.newDocumentBuilder();
		    builder.setErrorHandler(new ErrorHandler() {
			
		        @Override
			    public void error(SAXParseException exception) throws org.xml.sax.SAXException {

		        	OnixValidator.logError("ERROR!  Could not correctly parse ONIX file(" + CurrentOnixFile + ").");
			        OnixValidator.logException(exception);
			        OnixFileIsValid = false;
			    }
		    
			    @Override
			    public void fatalError(SAXParseException exception) throws org.xml.sax.SAXException {
			    	
		        	OnixValidator.logError("ERROR!  Could not correctly parse ONIX file(" + CurrentOnixFile + ").");			    	
			    	OnixValidator.logException(exception);
			        OnixFileIsValid = false;
			    }

			    @Override
			    public void warning(SAXParseException exception) throws org.xml.sax.SAXException {
			        exception.printStackTrace();
			    }
	        });

		    // NOTE: It is necessary to use an InputStream here since the file will remain registered as "locked"		    
		    //       on Windows unless you take control and create a scope around its file handle
		    try (InputStream onixFileStream = new FileInputStream(pOnixFile.getAbsolutePath())) {
		        Document doc = builder.parse(onixFileStream);		        
		    }
	        catch (SAXException exception) {
	        	OnixValidator.logError("ERROR!  Could not correctly parse ONIX file(" + CurrentOnixFile + ").");			    	
		    	OnixValidator.logException(exception);	        	
	            OnixFileIsValid = false;
	        }
	        catch (IOException exception) {
	        	OnixValidator.logError("ERROR!  Could not correctly parse ONIX file(" + CurrentOnixFile + ").");			    	
		    	OnixValidator.logException(exception);	        	
	            OnixFileIsValid = false;
	        }
		}
		
		return OnixFileIsValid;
	}
}
