package com.delta;

import com.delta.OntUtility;

import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Statement;

import com.hp.hpl.jena.reasoner.Reasoner;

import com.hp.hpl.jena.util.PrintUtil;

public class SWRLApp 
{
    final static String targetLocalName = "target";
    final static String targetNameSpace = "http://www.delta.com.tw/runtime#";

    public static void main( String[] args ) throws Exception
    {
      String rdfFilePath = args[0];
      String ruleFilePath = args[1];
      String JSONFilePath = args[2];

      Map<String, String> abbrMap = OntUtility.getAbbrMap(ruleFilePath);

      Model model = OntUtility.getModel(rdfFilePath);
      Resource target = OntUtility.createResourceByJSONFile(
          model, 
          OntUtility.getURI(targetNameSpace, targetLocalName), 
          JSONFilePath, 
          abbrMap);
      Reasoner reasoner = OntUtility.getReasoner(ruleFilePath);
      InfModel infModel = ModelFactory.createInfModel(reasoner, model);
      
      Property subClassOf = infModel.getProperty(abbrMap.get("rdfs"), "subClassOf");
      target = infModel.getResource(OntUtility.getURI(targetNameSpace, targetLocalName));
      
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
          if(subject.getURI().equals(OntUtility.getURI(targetNameSpace, targetLocalName)) && 
             property.getNameSpace().equals(abbrMap.get("rdfs")) && 
             property.getLocalName().equals("subClassOf")) {
            System.out.println(PrintUtil.print(statement.getObject()));
          }
        }
      }
    }
}
