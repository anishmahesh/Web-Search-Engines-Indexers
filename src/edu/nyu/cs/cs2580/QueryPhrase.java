package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {
  public Vector<String> _consecutiveTokens = new Vector<String>();

  public QueryPhrase(String query) {
    super(query);
  }

  @Override
  public void processQuery() {
    if(_query == null){
      return;
    }
    String queryString;
    String tokenString = "";
    Pattern pattern = Pattern.compile("([\"'])(?:(?=(\\\\?))\\2.)*?\\1");
    Matcher matcher = pattern.matcher(_query);
    while (matcher.find()) {
      queryString = matcher.group(1);
      queryString = TextProcessor.regexRemoval(queryString);
      Scanner s = new Scanner(queryString);
      Stemmer stemmer = new Stemmer();
      while (s.hasNext()) {
        String term = s.next();
        stemmer.add(term.toCharArray(), term.length());
        stemmer.stem();
        tokenString.concat(stemmer.toString());
      }
      s.close();
      _consecutiveTokens.add(tokenString);
      _query.replaceFirst("([\"'])(?:(?=(\\\\?))\\2.)*?\\1","");
    }

    _query = TextProcessor.regexRemoval(_query);
    Scanner s1 = new Scanner(_query);
    Stemmer stemmer = new Stemmer();
    while (s1.hasNext()) {
      String term = s1.next();
      stemmer.add(term.toCharArray(), term.length());
      stemmer.stem();
      _tokens.add(stemmer.toString());
    }
    s1.close();
  }
}
