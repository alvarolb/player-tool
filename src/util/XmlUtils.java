package util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class XmlUtils {

    /**
     * Looks through all child elements of the specified root (recursively) and
     * returns the elements that corresponds to all parameters.
     *
     * @param root the Element where the search should begin
     * @param tagName the name of the node we're looking for
     * @param keyAttributeName the name of an attribute that the node has to
     * have
     * @param keyAttributeValue the value that attribute must have
     * @return list of Elements in the tree under root that match the specified
     * parameters.
     * @throws NullPointerException if any of the arguments is null.
     */
    private static final String TAG = XmlUtils.class.getName();

    public static List<Element> locateElements(Element root, String tagName, String keyAttributeName, String keyAttributeValue) {
        ArrayList<Element> result = new ArrayList<>();
        NodeList nodes = root.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++) {
            node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            // is this the node we're looking for?
            if (node.getNodeName().equals(tagName)) {
                String attr = ((Element) node).getAttribute(keyAttributeName);

                if (attr != null && attr.equals(keyAttributeValue)) {
                    result.add((Element) node);
                }
            }

            // look inside.

            List<Element> childs = locateElements((Element) node, tagName, keyAttributeName, keyAttributeValue);

            if (childs != null) {
                result.addAll(childs);
            }

        }

        return result;
    }

    public static Element locateElement(Element root, String tagName, String keyAttributeName, String keyAttributeValue) {
        if (root == null) {
            return null;
        }
        NodeList nodes = root.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++) {
            node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue; // is this the node we're looking for?
            }
            if (node.getNodeName().equals(tagName)) {
                String attr = ((Element) node).getAttribute(keyAttributeName);
                if (attr != null && attr.equals(keyAttributeValue)) {
                    return (Element) node;
                }
            }

            // look inside.
            Element child = locateElement((Element) node, tagName, keyAttributeName, keyAttributeValue);

            if (child != null) {
                return child;
            }
        }

        return null;
    }

    public static int getInteger(Element parentNode) {
        final String srcText = getText(parentNode);

        int result = 0;

        try {
            result = Integer.parseInt(srcText);
        } catch (Exception e) {
        }

        return result;
    }
    
    public static double getDouble(Element parentNode) {
        final String srcText = getText(parentNode);

        double result = 0;

        try {
            result = Double.parseDouble(srcText);
        } catch (Exception e) {
        }

        return result;
    }
    
    
    public static double getDouble(Element parentNode, double defaultValue) {
        final String srcText = getText(parentNode);

        double result = defaultValue;

        try {
            result = Double.parseDouble(srcText);
        } catch (Exception e) {
        }

        return result;
    }

    public static String getText(Element parentNode) {

        if (parentNode == null) {
            return null;
        }

        NodeList nodes = parentNode.getChildNodes();

        if (nodes == null || nodes.getLength() < 1) {
            return null;
        }

        Node node;
        StringBuilder data = new StringBuilder();

        for (int i = 0; i < nodes.getLength(); i++) {
            node = nodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                data.append(((Text) node).getData().toString());
            }
        }

        return data.toString();
    }

    public static Element findChild(Element parent, String tagName) {
        if (parent == null || tagName == null) {
            //Log.e(TAG, "Parent or tagname were null! " + "parent = " + parent + "; tagName = " + tagName);
            return null;
        }
        NodeList nodes = parent.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++) {
            node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getNodeName().equals(tagName)) {
                return (Element) node;
            }
        }

        return null;
    }

    public static Element findChildByChain(Element parent, String[] tagNames) {

        if (parent == null || tagNames == null) {
            //Log.e(TAG, "Parent or tagname were null! " + "parent = " + parent + "; tagName = " + tagNames);
            return null;
        }

        if (tagNames.length == 0) {
            return parent;
        }

        NodeList nodes = parent.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++) {
            node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getNodeName().equals(tagNames[0])) {
                String[] newTags = new String[tagNames.length - 1];
                System.arraycopy(tagNames, 1, newTags, 0, newTags.length);

                return findChildByChain((Element) node, newTags);
            }
        }

        return null;
    }

    public static List<Element> findChildrenByChain(Element parent, String[] tagNames) {
        if (parent == null || tagNames == null) {
            throw new NullPointerException("Parent or tagname were null! " + "parent = " + parent + "; tagName = ");
        }

        ArrayList<Element> result = new ArrayList<Element>();

        if (tagNames.length == 0) {
            result.add(parent);
            return result;
        }

        NodeList nodes = parent.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++) {
            node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getNodeName().equals(tagNames[0])) {
                String[] newTags = new String[tagNames.length - 1];
                System.arraycopy(tagNames, 1, newTags, 0, newTags.length);

                result.addAll(findChildrenByChain((Element) node, newTags));
            }
        }

        return result;
    }

    /**
     * Returns the children elements with the specified tagName for the
     * specified parent element.
     *
     * @param parent The parent whose children we're looking for.
     * @param tagName the name of the child to find
     * @return List of the children with the specified name
     * @throws NullPointerException if parent or tagName are null
     */
    public static List<Element> findChildren(Element parent, String tagName) {

        if (parent == null || tagName == null) {
            //Log.e(TAG, "Parent or tagname were null! " + "parent = " + parent + "; tagName = " + tagName);
            return null;
        }

        ArrayList<Element> result = new ArrayList<Element>();
        NodeList nodes = parent.getChildNodes();
        Node node;
        int len = nodes.getLength();
        for (int i = 0; i < len; i++) {
            node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getNodeName().equals(tagName)) {
                result.add((Element) node);
            }
        }

        return result;
    }

    /**
     * Converts a XML String representing a date to Date
     *
     * @param xmlDate string representing the XML date
     * @return Date instance if can be converted, null otherwise
     */
    public static Date getXMLDate(String xmlDate) {
        if (xmlDate == null) {
            return null;
        }
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return simpleDateFormat.parse(xmlDate);
        } catch (ParseException e) {
            //Log.e(TAG, "Error while converting XML date:" + e.getMessage());
        }
        return null;
    }

    /**
     * Converts a Date to a XML string date
     *
     * @param date instance of Date to convert
     * @return String instance of the converted date, null otherwise
     */
    /*
     public static String getXMLDate(Date date){
     if(date==null) return null;
     String stringDate = null;
     try{
     GregorianCalendar gc = new GregorianCalendar();
     gc.setTime(date);
     XMLGregorianCalendar xmlTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
     stringDate = xmlTime.toXMLFormat();
     }catch(DatatypeConfigurationException e){
     //Log.e(TAG, e.getMessage());
     }
     return stringDate;
     }
     */
    /**
     * Creates a Document from a given byte buffer
     *
     * @param buffer buffer content
     * @return Document instance of the document, null otherwise
     */
    public static Document createDocumentFromBuffer(byte[] buffer) {
        if (buffer == null) {
            return null;
        }

        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setIgnoringComments(true);
        DocumentBuilder docBuilder;
        Document doc = null;
        try {
            docBuilder = dbfactory.newDocumentBuilder();
            ByteArrayInputStream in = new ByteArrayInputStream(buffer);
            doc = docBuilder.parse(in);
        } catch (Exception ex) {
            //Log.e(TAG, ex.getMessage());
        }

        return doc;
    }

    /**
     * Creates a Document from a given file
     *
     * @param file file path
     * @return Document instance of the document, null otherwise
     */
    public static Document createDocumentFromFile(File file) {
        if (file == null) {
            return null;
        }

        Document cache = null;
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            cache = docBuilder.parse(file);
        } catch (ParserConfigurationException e) {
            //Log.e(TAG, e.getMessage());
        } catch (SAXException e) {
            //Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            //Log.e(TAG, e.getMessage());
        }
        return cache;
    }

    /**
     * Saves a given doc into a file
     *
     * @param doc xml document to save
     * @param file file path to store the result
     */
    public static void saveDocToFile(Document doc, File file) {
        if (doc == null || file == null) {
            return;
        }

        try {
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(OutputKeys.INDENT, "no");

            Result result = new StreamResult(file);

            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
        } catch (TransformerConfigurationException e) {
            //Log.e(TAG, e.getMessage());
        } catch (TransformerException e) {
            //Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Creates an empty XML document
     *
     * @return the instance of Document created, null otherwise
     */
    public static Document createDocument() {
        Document doc = null;

        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder;
            docBuilder = dbfac.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            //Log.e(TAG, e.getMessage());
        }

        return doc;
    }
}
