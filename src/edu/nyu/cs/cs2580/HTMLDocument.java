package edu.nyu.cs.cs2580;

/**
 * Created by sanchitmehta on 26/10/16.
 */
public class HTMLDocument {
    private String bodyText;
    private String title;

    public HTMLDocument(String bodyText,String title){
        this.bodyText = bodyText;
        this.title = title;
    }

    public String getBodyText(){
        return  this.bodyText;
    }

    public void setBodyText(String bodyText){
        this.bodyText = bodyText;
    }

    public String getTitle(){
        return  this.title;
    }

    public void setTitle(String title){
        this.title = title;
    }

}
