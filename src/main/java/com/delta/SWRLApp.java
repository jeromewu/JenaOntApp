package com.delta;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Resource;
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
      System.out.println(target.getProperty(subClassOf).getObject());
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
        JSONObject obj = iterator.next();

        String propertyNS = (String)obj.get("propertyNS");
        String property = (String)obj.get("property");
        String valueNS = (String)obj.get("valueNS");
        String value = (String)obj.get("value");

        Property p = model.getProperty(prefixMap.get(propertyNS), property);
        if(valueNS != "xsd") {
          Resource r = model.getResource(getURI(prefixMap.get(valueNS), value));
          target.addProperty(p, r);
        } else {
          ; 
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
}
