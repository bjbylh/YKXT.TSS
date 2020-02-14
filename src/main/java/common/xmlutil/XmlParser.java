package common.xmlutil;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.HashMap;
import java.util.Iterator;


/**
 * Created by lihan on 2019/12/12.
 */
public class XmlParser {
    public static HashMap<String, String> parser(String xmlString) {

        HashMap<String, String> ret = new HashMap<>();
        Document doc = null;
        try {
            doc = DocumentHelper.parseText(xmlString); // 将字符串转为XML
            Element rootElt = doc.getRootElement(); // 获取根节点
            System.out.println("根节点：" + rootElt.getName()); // 拿到根节点的名称
            Iterator iter = rootElt.elementIterator("J2000"); // 获取根节点下的子节点head
            // 遍历head节点
            while (iter.hasNext()) {
                Element recordEle = (Element) iter.next();
                Iterator oscu = recordEle.elementIterator("OSCU");

                while (oscu.hasNext()) {
                    Element ele = (Element) oscu.next();
                    String JD = ele.elementTextTrim("JD"); // 拿到head节点下的子节点title值
                    System.out.println(JD);
                    ret.put("JD", JD);
                    String JS = ele.elementTextTrim("JS");
                    System.out.println(JS);
                    ret.put("JS", JS);
                    String A = ele.elementTextTrim("A");
                    System.out.println(A);
                    ret.put("A", A);
                    String E = ele.elementTextTrim("E");
                    System.out.println(E);
                    ret.put("E", E);
                    String I = ele.elementTextTrim("I");
                    System.out.println(I);
                    ret.put("I", I);
                    String O = ele.elementTextTrim("O");
                    System.out.println(O);
                    ret.put("O", O);
                    String W = ele.elementTextTrim("W");
                    System.out.println(W);
                    ret.put("W", W);
                    String M = ele.elementTextTrim("M");
                    System.out.println(M);
                    ret.put("M", M);
                    return ret;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret.clear();
            ret.put("ERROR", e.getMessage());
            return ret;
        }
        return ret;
    }


    public static void main(String[] args) throws DocumentException {
        String xmlString = "\uFEFF<?xml version='1.0' encoding=\"gb2312\" ?>\n" +
                "<ORBIT>\n" +
                "    <J2000>\n" +
                "        <OSCU>\n" +
                "            <JD>25540</JD>\n" +
                "            <JS>32400.000000</JS>\n" +
                "            <A>7139979.794400</A>\n" +
                "            <E>0.0005256608</E>\n" +
                "            <I>98.6705509000</I>\n" +
                "            <O>45.7002402000</O>\n" +
                "            <W>265.5190993000</W>\n" +
                "            <M>182.8945976000</M>\n" +
                "        </OSCU>\n" +
                "    </J2000>\n" +
                "</ORBIT>".substring(1);
        HashMap<String, String> parser = XmlParser.parser(xmlString);
        System.out.println();
    }
}
