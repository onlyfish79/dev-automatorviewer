/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.uiautomator.tree;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class UiHierarchyXmlLoader {

    private BasicTreeNode mRootNode;
    private List<Rectangle> mNafNodes;
    private List<BasicTreeNode> mNodeList;
    private static int  index = 0;
    private File sScreenshotFile = new File("/tmp/uiautomatorviewer/screenshot.png");
    private BufferedImage screenShot;
    //add by helen
    private UiNode mTmpNode ;
    
    public UiHierarchyXmlLoader() {
    	try {
    	    screenShot = ImageIO.read(sScreenshotFile);
    	}catch(IOException e) {
    		
    	}
    }

    /**
     * Uses a SAX parser to process XML dump
     * @param xmlPath
     * @return
     */
    public BasicTreeNode parseXml(String xmlPath) {
        mRootNode = null;
        mNafNodes = new ArrayList<Rectangle>();
        mNodeList = new ArrayList<BasicTreeNode>();
        // standard boilerplate to get a SAX parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = null;
        try {
            parser = factory.newSAXParser();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        }
        // handler class for SAX parser to receiver standard parsing events:
        // e.g. on reading "<foo>", startElement is called, on reading "</foo>",
        // endElement is called
        DefaultHandler handler = new DefaultHandler(){
            BasicTreeNode mParentNode;
            BasicTreeNode mWorkingNode;
            @Override
            public void startElement(String uri, String localName, String qName,
                    Attributes attributes) throws SAXException {
            	
                boolean nodeCreated = false;
                // starting an element implies that the element that has not yet been closed
                // will be the parent of the element that is being started here
                mParentNode = mWorkingNode;
                if ("hierarchy".equals(qName)) {
                	System.out.println("*****UiNode, hierarchy, can screenshot...");
                    int rotation = 0;
                    for (int i = 0; i < attributes.getLength(); i++) {
                        if ("rotation".equals(attributes.getQName(i))) {
                            try {
                                rotation = Integer.parseInt(attributes.getValue(i));
                            } catch (NumberFormatException nfe) {
                                // do nothing
                            }
                        }
                    }
                    mWorkingNode = new RootWindowNode(attributes.getValue("windowName"), rotation);
                    nodeCreated = true;
                } else if ("node".equals(qName)) {
                    UiNode tmpNode = new UiNode();
                    String classValue = attributes.getValue("class");
//                    if (classValue.equals("android.widget.TextView") == false){
                    if (1==1) { 
	                    for (int i = 0; i < attributes.getLength(); i++) {
	                    	String keyValue = attributes.getQName(i);
//	                    	System.out.println("***print attributes values: " + classValue);
	                        tmpNode.addAtrribute(attributes.getQName(i), attributes.getValue(i));
	                        
	                    	if (keyValue.equals("bounds") && tmpNode.width > 0 && tmpNode.height > 0) {
	                    	    index += 1;
		                        if(sScreenshotFile.exists()) {
			                    	try {		  
			                    		System.out.println("tmpNode keyValue: " + keyValue + ", tmpNode xpath:" + tmpNode.getXpath() + ", tmpNode x: " + 
			                    	tmpNode.x + ", y: " + tmpNode.y + ", width: " + tmpNode.width + ", height: " + tmpNode.height);     
			                        	BufferedImage dest = screenShot.getSubimage(tmpNode.x, tmpNode.y, tmpNode.width, tmpNode.height);
			                        	String dstFilePath = "/tmp/uiautomatorviewer/dst-"+index+".png";
			                    		System.out.println("UiNode, node, can screenshot, save filePath: " + dstFilePath);
			                        	File outputfile = new File(dstFilePath);
			                        	ImageIO.write(dest, "PNG", outputfile);
			                    	}catch(IOException e) {
			                    		e.printStackTrace();
			                    	}

		                        }else {
		                        	System.out.println(sScreenshotFile.getAbsolutePath() + " not exist....");
		                        }
	                    	}
	                    }
	                    mWorkingNode = tmpNode;
	                    nodeCreated = true;
	                    // check if current node is NAF
	                    String naf = tmpNode.getAttribute("NAF");
	                    if ("true".equals(naf)) {
	                        mNafNodes.add(new Rectangle(tmpNode.x, tmpNode.y,
	                                tmpNode.width, tmpNode.height));
	                    }
                    }
                }
                // nodeCreated will be false if the element started is neither
                // "hierarchy" nor "node"
                if (nodeCreated) {
                    if (mRootNode == null) {
                        // this will only happen once
                        mRootNode = mWorkingNode;
                    }
                    if (mParentNode != null) {
                        mParentNode.addChild(mWorkingNode);
                        //System.out.println(mNodeList.size());
                        if(mWorkingNode.getParent()!=null){
                        	String xpath = ((UiNode)mWorkingNode).getXpath();
                        	((UiNode)mWorkingNode).addAtrribute("xpath",xpath);
                        }
                        mNodeList.add(mWorkingNode);

                        //System.out.println(mNodeList.size());
                    }
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                //mParentNode should never be null here in a well formed XML
                if (mParentNode != null) {
                    // closing an element implies that we are back to working on
                    // the parent node of the element just closed, i.e. continue to
                    // parse more child nodes
                    mWorkingNode = mParentNode;
                    mParentNode = mParentNode.getParent();
                }
            }
        };
        try {
            parser.parse(new File(xmlPath), handler);
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return mRootNode;
    }

    /**
     * Returns the list of "Not Accessibility Friendly" nodes found during parsing.
     *
     * Call this function after parsing
     *
     * @return
     */
    public List<Rectangle> getNafNodes() {
        return Collections.unmodifiableList(mNafNodes);
    }

    public List<BasicTreeNode> getAllNodes(){
        return mNodeList;
    }
}
