package bn.com.onix.validation;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

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
			        // do something more useful in each of these handlers
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
		  
		    Document doc = builder.parse(pOnixFile.getAbsolutePath());		        
		}
		
		return OnixFileIsValid;
	}
}
