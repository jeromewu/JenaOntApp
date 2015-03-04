package com.delta;

import java.util.Iterator;
import java.io.FileReader;
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
    final static String mealURI = "http://www.delta.com.tw/meal#";
    final static String wineURI = "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#";
    final static String foodURI = "http://www.w3.org/TR/2003/PR-owl-guide-20031209/food#";
    final static String rdfsURI = "http://www.w3.org/2000/01/rdf-schema#";

    public static void main( String[] args )
    {
      String rdfFilePath = args[0];
      String ruleFilePath = args[1];
      String JSONFilePath = args[2];

      Model model = getModel(rdfFilePath);
      Resource target = createTargetResource(model, JSONFilePath);
      Reasoner reasoner = getReasoner(ruleFilePath);
      InfModel infModel = ModelFactory.createInfModel(reasoner, model);
      
      registerPrefix();
      //printAllStatement(model);
      //printAllStatement(inf);
      Property subClassOf = infModel.getProperty(rdfsURI, "subClassOf");
      target = infModel.getResource(mealURI+"target");
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
    private static Resource createTargetResource(Model model, String JSONFilePath) {
      Resource target = model.createResource(mealURI+"target");
      JSONParser parser = new JSONParser();
      try {
        Object obj = parser.parse(new FileReader(JSONFilePath));
        JSONObject jsonObject = (JSONObject)obj;
        JSONArray input = (JSONArray)jsonObject.get("input");
        Iterator<JSONObject> iterator = input.iterator();
        while(iterator.hasNext()) {
          JSONObject t = iterator.next();
          Property p = model.getProperty(mealURI, (String)t.get("property"));
          Resource r = model.getResource((String)t.get("value"));
          target.addProperty(p, r);
        }
      } catch(Exception e) {
        e.printStackTrace();
      }
      return target;
    }

    private static void registerPrefix() {
      PrintUtil.registerPrefix("food", foodURI);
      PrintUtil.registerPrefix("wine", wineURI);
      PrintUtil.registerPrefix("meal", mealURI);
    }
}
