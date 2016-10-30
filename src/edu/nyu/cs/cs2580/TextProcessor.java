package edu.nyu.cs.cs2580;

/**
 * Created by naman on 10/29/2016.
 */
public class TextProcessor {
    private static final String[] removalRegex= {"((http://)|(https://))[^\\s]*[\\s]",
            "(\\[[0-9]*\\])",
            "(\\[edit\\])" ,
            "\\p{Punct}",
            "[\\s]((the)|(or)|(and)|(be)|(of)|(for)|(to)|(is)|(was)|(it)|(has)|(had)|(etc)|(shall)|(a)|(but)|(him)|(his)|(if)|(an)|(in))[\\s]",
            "[\\s][^\\s][\\s]"};

    public static String regexRemoval(String _text){
        for(String regex : removalRegex){
            _text = _text.replaceAll(regex," ");
        }
        return _text;
    }
}
