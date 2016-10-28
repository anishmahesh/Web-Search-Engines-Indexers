package edu.nyu.cs.cs2580;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.io.File;
import java.io.IOException;

/**
 * Created by sanchitmehta on 26/10/16.
 */
public class HTMLParse {

    HTMLDocument _currentDocument;

    public HTMLDocument getDocument(File fileName) throws IOException {

        org.jsoup.nodes.Document doc = Jsoup.parse(fileName, "UTF-8", "https://en.wikipedia.org/wiki/Courant_Institute_of_Mathematical_Sciences");
        Element body = doc.body();
        doc.title().replaceFirst("- Wikipedia, the free encyclopedia","");
        String bodyText = new String(body.text().toLowerCase());
        //bodyText.replaceAll("\\p{P}", " ");
        String title = doc.title();
        _currentDocument = new HTMLDocument(bodyText,title,fileName.getAbsolutePath());
        stemBodyText();
        return _currentDocument;
    }

    public void stemBodyText(){
        Stemmer s = new Stemmer();
        s.add(_currentDocument.getBodyText().toCharArray(), _currentDocument.getBodyText().length());
        s.stem();
        _currentDocument.setBodyText(s.toString());
    }


}
