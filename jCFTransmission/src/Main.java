import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * CFTransmission is a source-code instrumentation project aimed at reading in Coldfusion component source code and
 * from that generating numerous types of output such as annotated comments on the code, YAML, JSON, XML,
 * API documentation, etc. In short, CFTransmission is a code transpiler that supports numerous types of output.
 * 
 * CFTransmission will return a number of types of data that are Coldfusion compatible including Struct, Array, as well as
 * simply writing the output to a file. 
 * 
 * CFTransmission can return the modified properties, individually or as an entire modified source file.
 * @author ianhickey
 *
 */
public class Main {
	/**
	 * //This class allows us to access this project from the command line.
	 * @param args merge "file1,file2" source|struct|array 
	 * @throws IOException 
	 */
	@SuppressWarnings({ "unused", "static-access", "deprecation" })
	public static void main(String[] args) throws IOException {
		//|=================================================================================>>>
		// Get our lexer
				CFTransmissionListener listener = null;
				String cfcFileList = null;
				String[] cfcFiles = null;
				String action  = "";
				String cfcFile = System.getProperty("user.dir") + "/test/test-source.txt";
				String tsFile = "";
				String returnType = "source";
				String properties = "";
				Integer numberOfFiles = 0;
				String v = "1.0.0";
		//|=================================================================================>>>
		//Handle CLI
		Option srcfile  = OptionBuilder.withArgName( "/path/to/file/filename.cfc" )
	        .hasArgs(1)
	        .withValueSeparator()
	        .withDescription( "source file to transpile to typescript including to path to that file as /path/to/file/filename.cfc" )
	        .create( "s" );

		Option outfile = OptionBuilder.withArgName( "/path/to/output/directory/filename.ts" )
            .hasArgs(1)
            .withDescription( "output file for the transpiled typescript including to path to that file as /path/to/file/filename.ts" )
            .create( "o" );
		
		Option logfile = OptionBuilder.withArgName( "logfile=/path/to/log/directory/logfilename" )
            .hasArgs(2)
            .withDescription( "log file for log messages including to path to that file as /path/to/file/filename.txt" )
            .create( "l" );
		
		Option props = OptionBuilder.withArgName( "properties=value" )
            .hasArgs(2)
            .withDescription( "a list of properties to add to a cfc" )
            .create( "p" );
		
		Option nameChange = OptionBuilder.withArgName( "name=value" )
            .hasArgs(2)
            .withDescription( "change the name of an entity" )
            .create( "c" );
		
		Option actionOption = OptionBuilder.withArgName( "value" )
	            .hasArgs(1)
	            .withDescription( "the action to take: for example, transpile" )
	            .create( "action" );
		
		Option help = new Option("help","Display this help message");
		
		Option verbose = 	new Option( "verbose", "show all console messages as transpile is happening" );
		Option transpile = 	new Option( "transpile", "transpile a sourcefile to an outputfile" );
		Option get = 		new Option( "get", "returns the properties and methods from a sourcefile" );
		Option add = 		new Option( "add", "adds properties and or methods to a source file" );
		Option version = 	new Option( "version", "displays the version" );
		Options options = 	new Options();

		options.addOption( srcfile );
		options.addOption( outfile );
		options.addOption( logfile );
		options.addOption( verbose );
		options.addOption( transpile );
		options.addOption( get );
		options.addOption( add );
		options.addOption( version );
		options.addOption( props );
		options.addOption( nameChange );
		options.addOption( help );
		options.addOption( actionOption );
		
		//|-->create the parser<--|//
	    CommandLineParser parser = new DefaultParser();
	    CommandLine line = null;
	    
	    try 
	    {
	        // parse the command line arguments
	        line = parser.parse( options, args );
	    }
	    catch( ParseException exp ) {
	        // oops, something went wrong
	        System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
	    }
		
	    //Call constructors based on the command line
	    if( line.hasOption( "get" ) && line.hasOption("srcfile") ) 
	    {
	    	action = "get";
	    	cfcFile = line.getOptionValue("srcfile");
	    	CFTransmission.getPropertiesFromFile(cfcFile);
	    	
	    }
	    
	    else if( line.hasOption( "help" )) 
	    {
	    	System.out.println("Usage: -action {transpile, get, add, rename, ...} -s srcfile=/path/to/file/filename.cfc -o outfile=/path/to/output/directory/filename.ts -v (verbose)\n");
	    	
	    }
	    
	    else if(line.hasOption( "add" ) && line.hasOption("srcfile") )
	    {
	    	action = "add";
	    	cfcFile = line.getOptionValue("srcfile");
	    	CFTransmission.addPropertiesToFile("property name=\"customAddress\" persistent=\"false\";", cfcFile);
	    }
	    
	    else if( line.hasOption( "action" ))
	    {
	    	action = "transpile";
	    	cfcFile = line.getOptionValue( "s" );
	    	tsFile = line.getOptionValue( "o" );
	    	System.out.println(tsFile);
	    	CFTransmission.transpile(cfcFile, tsFile);
	    }
	    
	    
	}

}
