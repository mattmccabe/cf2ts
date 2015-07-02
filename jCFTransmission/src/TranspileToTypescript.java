/**
 * @author ianhickey
 *
 */
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;


/**
 * @author ianhickey
 * Accepts a source and a destination, and updates a Coldfusion Component with the options specified.
 * Merges two components methods and properties by parsing source_1, and source_2 and outputting source_3.
 * 
 * The product of the two source files will contain all properties and methods from both in the output.
 * 
 * This process is additive, and when confronted with two properties with the same name, the property from
 * the second source file is left out of the product UNLESS overwrite = true in the options configuration.
 * 
 * Additionally, a custom prefix can be enforced by passing that prefix. In that case, all properties and method
 * from the second source must contain that prefix in the name.
 */
public class TranspileToTypescript extends CFTransmissionBaseListener {
	
	public String cfcFileName = "";
	public String tsFileName = "";
	String componentName = "";
	String propertyName = "";
	String propertyType = "";
	String propertyObject = "";
	String methodName = "";
	Parser myParser;
	TokenStream tokens = null;
	TokenStreamRewriter rewriter;
	int position = 0;
	String actionType = "get";
	ArrayList<String> propertyList; 
	StringBuilder propertyAttributes;
	boolean isInserted;
	boolean isRemoved = false;
	boolean isMany2Many = false;
	boolean isOne2Many = false;
	boolean isMany2One = false;
	String classTemplate = "";
	String moduleName = "ngSlatwall";
	String baseClass = "hibachiEntity";
	HashMap<String, Object> gettersandsetters;
	
	/**
	 * Constructor
	 * @param parser
	 * @param tokens2
	 * @param cfcFileName
	 * @param tsFileName
	 * @param actionType2
	 */
	public TranspileToTypescript(CFTransmissionParser parser, CommonTokenStream tokens2, String cfcFileName, String tsFileName,
			String actionType2) {
		super();
		this.myParser = parser;
		this.tokens = tokens2;
		this.tsFileName = tsFileName;
		this.cfcFileName = cfcFileName;
		this.actionType = actionType2;
		rewriter = new TokenStreamRewriter(tokens);
		propertyList = new ArrayList<String>();
		propertyAttributes = new StringBuilder();
		gettersandsetters = new HashMap<String, Object>(); 
	}
	/**
	 * Turns the component definition into doc comments.
	 */
	@Override
	public void enterComponent(CFTransmissionParser.ComponentContext componentCtx){
		
		List<CFTransmissionParser.KeyValueContext> keyValueList = componentCtx.keyValue();
		int i = 0; //index
		for (CFTransmissionParser.KeyValueContext keyVal : keyValueList){
			int listSize = keyValueList.size();
			
			if (keyVal.getText().toLowerCase().contains("displayname=")){
				componentName = keyVal.getText().replace(" ", "").split("\"")[1];	
			}
			
			rewriter.replace(keyVal.getStart(), keyVal.getStop(), "\t*@" + keyVal.getText().replace(" ", "").replace("=", " ") + "\n");
			if (i + 1 == listSize){
				
				
				rewriter.insertAfter(keyVal.getStop(),
						"\nmodule slatwall { \n    class " + componentName); //+ " extends "+ baseClass +"\n");
				
			
				rewriter.insertAfter(keyVal.getStop(),
						"*/");
				
			}
			i++;
		}
		
		String openingCommentToken = "\n/**\n\t*@description This " + componentName 
				+ " class was generated using the cfc component located in /Slatwall/model/entity\n";
		rewriter.insertBefore(componentCtx.getStart(), openingCommentToken);
		rewriter.delete(componentCtx.getStart());
		
	}
	
	/**
	 * Transforms a component declaration into its annotated counterpart
	 */
	@Override
	public void exitComponent(CFTransmissionParser.ComponentContext componentCtx){
		String temp = "\n\n\t/** Getters and Setters */\n\n";
		
		Set<String> keys = gettersandsetters.keySet();
		for (String key : keys){
			temp += gettersandsetters.get(key) + "\n";
		}
		
		rewriter.replace(componentCtx.getStop(), 
				"\n\n\n"+ temp +"    }//<--end class\n}//<--end module");
		
		
		//THIS GENERATES THE TYPESCRIPT
		String generatedTypescript = rewriter.getText();
		//clean generated Typescript
		generatedTypescript = generatedTypescript.replaceAll("(?m)^[ \t]*\r?\n", "");
		
		
		//THIS WRITES IT TO FILE
		PrintWriter writer;
		try {
			writer = new PrintWriter(tsFileName, "UTF-8");
			writer.write(generatedTypescript);	
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * Transforms a property declaration into its annotated counterpart
	 */
	@Override
	public void enterPropertyDeclaration(CFTransmissionParser.PropertyDeclarationContext propertyCtx){
		propertyType = "";
		propertyName = "";
		propertyObject = "";
		isMany2Many = false;
		isOne2Many = false;
		isMany2One = false;
		rewriter.replace(propertyCtx.getStart(), "\n\t/**\n");
		List<CFTransmissionParser.KeyValueContext> keyValueList = propertyCtx.keyValue();
		for (CFTransmissionParser.KeyValueContext keyVal : keyValueList){
			
			if (keyVal.getText().replace(" ", "").toLowerCase().contains("name=")){
				propertyName = keyVal.getText().replace(" ", "").split("\"")[1];
				
			}
			
			if (keyVal.getText().replace(" ", "").toLowerCase().contains("ormtype") || keyVal.getText().replace(" ", "").toLowerCase().contains("type=")){
				propertyType = keyVal.getText().replace(" ", "").split("\"")[1];
				
			}
			if (keyVal.getText().replace(" ", "").toLowerCase().contains("cfc")){
				propertyObject = keyVal.getText().replace(" ", "").split("\"")[1];
				
			}
			
			if (keyVal.getText().replace(" ", "").toLowerCase().contains("fieldtype=\"many-to-one\"")){
				isMany2One = true;
							
			}
			if (keyVal.getText().replace(" ", "").toLowerCase().contains("fieldtype=\"one-to-many\"")){
				isOne2Many = true;
				
			}
			if (keyVal.getText().replace(" ", "").toLowerCase().contains("fieldtype=\"many-to-many\"")){
				isMany2Many = true;
				
			}
			rewriter.replace(keyVal.getStart(),  keyVal.getStop(), "\t*@" + keyVal.getText().replace("=", " ") + "\n");
		}
		
	}
	
	/**
	 * Transforms a property declaration into its annotated counterpart
	 */
	@Override
	public void exitPropertyDeclaration(CFTransmissionParser.PropertyDeclarationContext propertyCtx){
		
		rewriter.replace(propertyCtx.getStop(), "\t*/\n");
			
			if (propertyType.toLowerCase().equals("string")){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate  " + propertyName + ": "+ "string" +";\n");
				addGetterSetter(propertyName, "string", propertyObject);
			}
			else if(propertyType.toLowerCase().equals("timestamp")){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate  " + propertyName + ": "+ "string" +";\n");
				addGetterSetter(propertyName, "string", propertyObject);
			}
			else if(propertyType.toLowerCase().equals("boolean")){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate  " + propertyName + ": "+ "boolean" +";\n");
				addGetterSetter(propertyName, "boolean", propertyObject);
			}
			else if(propertyType.toLowerCase().equals("numeric")){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate  " + propertyName + ": "+ "number" +";\n");
				addGetterSetter(propertyName, "number", propertyObject);
			}
			else if(propertyType.toLowerCase().equals("currency")){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate " + propertyName + ": "+ "number" +";\n");
				addGetterSetter(propertyName, "number", propertyObject);
			}
			else if(propertyType.toLowerCase().equals("big_decimal")){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate " + propertyName + ": "+ "number" +";\n");
				addGetterSetter(propertyName, "number", propertyObject);
			}
			//Handle basic type
			else if(propertyType.toLowerCase().equals("integer")){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate " + propertyName + ": "+ "number" +";\n");
				addGetterSetter(propertyName, "number", propertyObject);
			}
			else if(propertyType.toLowerCase().equals("any")){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate " + propertyName + ": "+ "any" +";\n");
				addGetterSetter(propertyName, "any", propertyObject);
			}
			else if(propertyType.toLowerCase().equals("struct")){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate " + propertyName + ": "+ "any" +";\n");
				addGetterSetter(propertyName, "any", propertyObject);
			}
			else if(propertyType.toLowerCase().equals("array") && propertyObject.equals("")){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate " + propertyName + ": "+ "any[]" +";\n");
				addGetterSetter(propertyName, "any[]", propertyObject);
			}
			else if(isMany2Many){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate " + propertyName + ": "+"any[];\n");//propertyObject +"[];\n");
				addGetterSetter(propertyName, "any[]", propertyObject);
				
			}
			else if(isMany2One){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate " + propertyName + ": "+"any;\n");//propertyObject +";\n");
				addGetterSetter(propertyName, "any", propertyObject);
			}
			else if(isOne2Many){
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate " + propertyName + "s" + ": "+"any[];\n");//propertyObject +"[];\n");
				addGetterSetter(propertyName + "s", "any[]", propertyObject);
			}else {
				rewriter.insertAfter(propertyCtx.getStop(), "\tprivate " + propertyName + ": "+ "any" +";\n");
				addGetterSetter(propertyName, "any", propertyObject);
			}
	}
	public void addGetterSetter(String propertyName, String propertyType, String propertyObject){
		gettersandsetters.put(propertyName, "\tget"+ cap(propertyName) +"():"+ propertyType +" { return this."+ propertyName +"; }\n\tset"+ cap(propertyName) +"("+ propertyName +":"+ propertyType +") {this."+ propertyName +" = "+ propertyName +";}");
	}
	/**
	 * Transforms a function declaration into its annotated counterpart
	 */
	@Override
	public void enterFunctionDeclaration(CFTransmissionParser.FunctionDeclarationContext functionCtx){
		
		//		CFTransmissionParser.FunctionNameContext functionName = functionCtx.functionName();
		//		methodName = functionName.getText();
		rewriter.delete(functionCtx.getStart(), functionCtx.getStop());
	}
	
	/**
	 * Transforms a function declaration into its annotated counterpart
	 */
	@Override
	public void exitFunctionDeclaration(CFTransmissionParser.FunctionDeclarationContext functionCtx){
		//Grab the token stream so we can retrieve the token we want.
		
//		//End the comment
//		//writeDump("\n*/\n");
//		
//		//Grab all the function identifiers such as scope and return type.
//		List<TerminalNode> functionIdentifierList = functionCtx.Identifier();
//		Object[] fil = functionIdentifierList.toArray();
//		for (Object o : fil){
//			//writeDump(o.toString() + " ");
//		}
//		//Add the method name;
//		
//		//writeDump( "function " + methodName );
//		//writeDump("(");
//		Iterator<CFTransmissionParser.ArgumentDefinitionContext> arguments = functionCtx.argumentsDefinition().argumentDefinition().listIterator();
//		//While we have more arguments, print them.
//		while (arguments.hasNext()){
//			CFTransmissionParser.ArgumentDefinitionContext nextArgument = arguments.next();
//			if (nextArgument.getChildCount() > 0){
//				for (int i = 0; i <= nextArgument.getChildCount() - 1; i++){
//					if (i == 0){
//						//writeDump(" "); //add the opening space for style
//					}
//					if (nextArgument.getChild(i).getText().equals("=")){
//						//writeDump(nextArgument.getChild(i).getText());
//					}else{
//						//writeDump( nextArgument.getChild(i).getText() + " ");
//					}//<--end if
//				}//<--end for
//			}//<--end if
//		}//<--end while
//		//writeDump(")");
//		//writeDump ("\n");
		rewriter.delete(functionCtx.getStart(), functionCtx.getStop());
	}
	
	//<--Helper print method
	public static void writeDump(String text){
		System.out.print(text);
	}
	public String cap(String line){
		return Character.toUpperCase(line.charAt(0)) + line.substring(1);
	}

}


