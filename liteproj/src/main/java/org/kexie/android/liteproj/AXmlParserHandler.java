package org.kexie.android.liteproj;

import android.text.TextUtils;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import java.util.ArrayList;

import fr.xgouchet.axml.Attribute;
import fr.xgouchet.axml.CompressedXmlParserListener;

final class AXmlParserHandler
        implements CompressedXmlParserListener
{
    private final ArrayList<Branch> mStack;
    private Document mDocument;

    AXmlParserHandler()
    {
        mStack = new ArrayList<>();
    }

    Document getDocument()
    {
        return mDocument;
    }

    @Override
    public void startDocument()
    {
        mDocument = DocumentFactory.getInstance().createDocument();
        mStack.add(mDocument);
    }

    @Override
    public void endDocument()
    {

    }

    @Override
    public void startPrefixMapping(String prefix,
                                   String uri)
    {

    }

    @Override
    public void endPrefixMapping(String prefix,
                                 String uri)
    {

    }

    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attribute[] attributes)
    {
        Element element = TextUtils.isEmpty(uri)
                ? mStack.get(mStack.size() - 1).addElement(localName)
                : mStack.get(mStack.size() - 1).addElement(qName, uri);
        for (Attribute attribute : attributes)
        {
            if (TextUtils.isEmpty(attribute.getNamespace()))
            {
                element.addAttribute(attribute.getName(),
                        attribute.getValue());
            } else
            {
                element.addAttribute(new QName(attribute.getName(),
                                new Namespace(attribute.getPrefix(),
                                        attribute.getNamespace())),
                        attribute.getValue());
            }
        }
        mStack.add(element);
    }

    @Override
    public void endElement(String uri,
                           String localName,
                           String qName)
    {
        mStack.remove(mStack.size() - 1);
    }

    @Override
    public void characterData(String data)
    {
        Branch branch = mStack.get(mStack.size() - 1);
        if (branch instanceof Element)
        {
            ((Element) branch).addCDATA(data);
        }
    }

    @Override
    public void processingInstruction(String target,
                                      String data)
    {

    }

    @Override
    public void text(String data)
    {
        Branch branch = mStack.get(mStack.size() - 1);
        if (branch instanceof Element)
        {
            ((Element) branch).addText(data);
        }
    }
}
