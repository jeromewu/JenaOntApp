package com.delta;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.lang.Float;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Short;
import java.lang.Byte;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.lang.Boolean;
import java.lang.String;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.InfModel;

import com.hp.hpl.jena.util.FileManager;

import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasonerFactory;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

import com.hp.hpl.jena.util.PrintUtil;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class SWRLApp 
{
    final static String targetLocalName = "target";
    final static String targetNameSpace = "http://www.delta.com.tw/runtime#";

    public static void main( String[] args ) throws Exception
    {
      String rdfFilePath = args[0];
      String ruleFilePath = args[1];
      String JSONFilePath = args[2];

      Map<String, String> prefixMap = getPrefixMap(ruleFilePath);
      registerPrefix(prefixMap);

      Model model = getModel(rdfFilePath);
      Resource target = createTargetResource(model, JSONFilePath, prefixMap);
      Reasoner reasoner = getReasoner(ruleFilePath);
      InfModel infModel = ModelFactory.createInfModel(reasoner, model);
      
      Property subClassOf = infModel.getProperty(prefixMap.get("rdfs"), "subClassOf");
      target = infModel.getResource(getURI(targetNameSpace, targetLocalName));
      
      //Output Result
      StmtIterator iter = infModel.listStatements();
      while(iter.hasNext()) {
        Statement statement = iter.nextStatement();
        Resource subject = statement.getSubject();
        Property property = statement.getPredicate();
        if(subject == null || 
           property == null || 
           subject.getURI() == null || 
           property.getNameSpace() == null || 
           property.getLocalName() == null ) {
          continue;
        } else {
          if(subject.getURI().equals(getURI(targetNameSpace, targetLocalName)) && 
             property.getNameSpace().equals(prefixMap.get("rdfs")) && 
             property.getLocalName().equals("subClassOf")) {
            System.out.println(PrintUtil.print(statement.getObject()));
          }
        }
      }
    }

    private static Model getModel(String filePath) {
      return FileManager.get().loadModel("file:"+filePath);
    }

    private static Reasoner getReasoner(String ruleFilePath) {
      Model model = ModelFactory.createDefaultModel();
      Resource config = model.createResource();
      config.addProperty(ReasonerVocabulary.PROPruleMode, "hybrid");
      config.addProperty(ReasonerVocabulary.PROPruleSet, ruleFilePath);
      return GenericRuleReasonerFactory.theInstance().create(config);
    }

    private static void printAllStatement(Model model) {
      StmtIterator iter = model.listStatements();
      while(iter.hasNext()) {
        System.out.println(PrintUtil.print(iter.nextStatement()));
      }
    }

    @SuppressWarnings("unchecked")
    private static Resource createTargetResource(Model model, String JSONFilePath, Map<String, String> prefixMap) throws Exception{
      Resource target = model.createResource(getURI(targetNameSpace, targetLocalName));
      JSONObject jsonObject = parseJSON(JSONFilePath);

      JSONArray args = (JSONArray)jsonObject.get("args");
      Iterator<JSONObject> iterator = args.iterator();
      while(iterator.hasNext()) {
        JSONObject jsonObj = iterator.next();

        String propertyNS = (String)jsonObj.get("propertyNS");
        String property = (String)jsonObj.get("property");
        String valueNS = (String)jsonObj.get("valueNS");
        String value = (String)jsonObj.get("value");

        Property ontProperty = model.getProperty(prefixMap.get(propertyNS), property);
        if(!valueNS.contains("xsd")) {
          Resource resource = model.getResource(getURI(prefixMap.get(valueNS), value));
          target.addProperty(ontProperty, resource);
        } else {
          Literal literal = getLiteral(model, valueNS, value);
          target.addProperty(ontProperty, literal);
        }
      }
      return target;
    }

    private static JSONObject parseJSON(String JSONFilePath) throws Exception {
      JSONParser parser = new JSONParser();
      return (JSONObject)parser.parse(new FileReader(JSONFilePath));
    }

    private static void registerPrefix(Map<String, String> prefixMap) {
      for(String key : prefixMap.keySet()) {
        PrintUtil.registerPrefix(key, prefixMap.get(key));
      }
    }

    private static String getURI(String nameSpace, String localName) {
      return nameSpace + localName;
    }

    private static Map<String, String> getPrefixMap(String ruleFilePath) throws Exception {
      Map<String, String> prefixMap = new HashMap<String, String>();
      BufferedReader bufferedReader = new BufferedReader(new FileReader(ruleFilePath));
      String line = new String("");
      while((line = bufferedReader.readLine()) != null) {
        if(line.startsWith("@prefix")) {
          String[] seg = line.split(" ");
          String prefix = seg[1].substring(0, seg[1].length()-1);
          String uri = extractString(seg[2], ".*\\< *(.*) *\\>*.");
          prefixMap.put(prefix, uri);
        }
      }
      return prefixMap;
    }

    private static String extractString(String input, String regexp) {
      Pattern pattern = Pattern.compile(regexp);
      Matcher matcher = pattern.matcher(input);
      matcher.find();
      return matcher.group(1);
    }

    private static Literal getLiteral(Model model, String valueNS, String value) {
      Literal literal = null;
      if(valueNS.contains("float")) {
        literal = model.createTypedLiteral(new Float(Float.parseFloat(value)));
      } else if(valueNS.contains("double")) {
        literal = model.createTypedLiteral(new Double(Double.parseDouble(value)));
      } else if(valueNS.contains("int")) {
        literal = model.createTypedLiteral(new Integer(Integer.parseInt(value)));
      } else if(valueNS.contains("long")) {
        literal = model.createTypedLiteral(new Long(Long.parseLong(value)));
      } else if(valueNS.contains("short")) {
        literal = model.createTypedLiteral(new Short(Short.parseShort(value)));
      } else if(valueNS.contains("byte")) {
        literal = model.createTypedLiteral(new Byte(Byte.parseByte(value)));
      } else if(valueNS.contains("integer")) {
        literal = model.createTypedLiteral(new BigInteger(value));
      } else if(valueNS.contains("decimal")) {
        literal = model.createTypedLiteral(new BigDecimal(value));
      } else if(valueNS.contains("boolean")) {
        literal = model.createTypedLiteral(new Boolean(Boolean.parseBoolean(value)));
      } else if(valueNS.contains("string")) {
        literal = model.createTypedLiteral(value);
      } else {
        literal = model.createTypedLiteral(value);
      }
      return literal;
    }
}
