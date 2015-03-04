package com.delta;

import java.util.Date;
import java.text.SimpleDateFormat;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
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
import com.hp.hpl.jena.reasoner.rulesys.Rule;

public class SWRLApp 
{
    public static void main( String[] args )
    {
      String RDFFilePath = args[0];
      String RuleFilePath = args[1];

      Model model = getModel(RDFFilePath);
      String rules = readFile(RuleFilePath, Charset.defaultCharset());
      Reasoner reasoner = getReasoner(rules);
      InfModel inf = ModelFactory.createInfModel(reasoner, model);
      
      printAllStatement(inf);
      System.out.println(rules);
      System.out.println(getTimeStamp());
    }

    private static String getTimeStamp() {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS+08:00").format(new Date());
    }

    private static Model getModel(String filePath) {
      return FileManager.get().loadModel("file:"+filePath);
    }

    private static Reasoner getReasoner(String rules) {
      return new GenericRuleReasoner(Rule.parseRules(rules));
    }

    private static String readFile(String filePath, Charset encoding) {
      String ret = new String();
      try {
        byte[] encoded = Files.readAllBytes(Paths.get(filePath));
        ret = new String(encoded, encoding);
      } catch(IOException e) {
        ;
      } 
      return ret;
    }

    private static void printAllStatement(Model model) {
      StmtIterator iter = model.listStatements();
      while(iter.hasNext()) {
        Statement stmt = iter.nextStatement();
        Resource subject = stmt.getSubject();
        Property predicate = stmt.getPredicate();
        RDFNode object = stmt.getObject();

        System.out.print(subject.toString());
        System.out.print(" " + predicate.toString() + " ");
        if(object instanceof Resource) {
          System.out.print(object.toString());
        } else {
          System.out.print("\"" + object.toString() + "\"");
        }
        System.out.println(" .");
      }
    }
}
