package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.HashSet;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class JauditJprJwsMembership {
    
    public static void main(String[] args) throws Exception{
        
        if(args.length < 2) {
            System.out.println("Insuffucient Arguments: " + args.length);
            System.exit(1);
        }
        
        File jprFile = new File(args[0].trim());
        File jwsFile = new File(args[1].trim());    
        
        if(!jprFile.exists() || !jwsFile.exists()){
            System.out.println("Jpr or jws does not exist");
            System.exit(1);
        }
        HashSet<String> listOfJprs = new HashSet<String>();
        XMLDocument doc = XMLParserHelper.getXMLDocument(jwsFile.getAbsolutePath());
        NodeList rootNodes = doc.getElementsByTagName("workspace");
        if(rootNodes == null || rootNodes.getLength() != 1) {
            System.out.println("Incorrect number of root nodes - jws:workspace..");
            System.exit(1);
        }
        Node root = rootNodes.item(0);
        Node n = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(root, "list", "n", "listOfChildren");
        
        NodeList children = n.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node hashNode = children.item(i);
            if(!hashNode.getNodeName().equals("hash"))
                continue;
            Node urlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(hashNode, "url", "n", "URL");
            String jprValue = XMLParserHelper.getAttributeValue(urlNode, "path");
            if(!isEmpty(jprValue))
                listOfJprs.add(jprValue);
        }
        
        int jwsIndex = args[1].trim().lastIndexOf("/");
        if(jwsIndex != -1) {
            String jwsRoot = args[1].trim().substring(0, jwsIndex);
           // System.out.println("jwsRoot: " + jwsRoot);
            int jprIndex = args[0].trim().indexOf(jwsRoot);
            String jprValue = args[0].trim().substring(jprIndex + jwsRoot.length() + 1);
            //System.out.println("jprValue: " + jprValue);
            if(!listOfJprs.contains(jprValue)) {
                System.out.println("The jpr file " + args[0].trim() + " is not associated with the workspace "
                                   + args[1].trim() + ". This file cannot be audited correctly till it is associated with a jws.");
                System.exit(1);
            } else {
                System.out.println("jpr found in jws");
                System.exit(0);
            }
        }
    }
    
    
    private static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }
}
