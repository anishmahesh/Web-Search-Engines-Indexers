package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends Indexer implements Serializable {

  // Maps each term to their integer representation
  private Map<String, Integer> _dictionary = new HashMap<String, Integer>();
  // All unique terms appeared in corpus. Offsets are integer representations.
  private Vector<String> _terms = new Vector<String>();

  // Term document frequency, key is the integer representation of the term and
  // value is the number of documents the term appears in.
  private Map<Integer, Integer> _termDocFrequency =
          new HashMap<Integer, Integer>();
  // Term frequency, key is the integer representation of the term and value is
  // the number of times the term appears in the corpus.
  private Map<Integer, Integer> _termCorpusFrequency =
          new HashMap<Integer, Integer>();

  // Stores all Document in memory.
  private Vector<Document> _documents = new Vector<Document>();

  private Map<Integer, Vector<Integer>> _postings = new HashMap<>();

  public IndexerInvertedDoconly() {
  }

  public IndexerInvertedDoconly(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {

    String dir = "./data/wiki/";
    File[] fileNames = new File(dir).listFiles();
    System.out.println("Construct index from: " + dir);

    HTMLParse htmlParse = new HTMLParse();
    int i = 0;
    for (File fileName : fileNames) {
      i++;
      HTMLDocument htmlDocument = htmlParse.getDocument(fileName);
      DocumentIndexed doc = new DocumentIndexed(_documents.size());

      processLine(htmlDocument.getBodyText(), doc);

      doc.setTitle(htmlDocument.getTitle());
      doc.setUrl(htmlDocument.getUrl());
      _documents.add(doc);
      ++_numDocs;
//      if (i == 1000) break;
    }
    System.out.println(
            "Indexed " + Integer.toString(_numDocs) + " docs with " +
                    Long.toString(_terms.size()) + " terms.");

    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Store index to: " + indexFile);
    ObjectOutputStream writer =
            new ObjectOutputStream(new FileOutputStream(indexFile));
    writer.writeObject(this);
    writer.close();

    System.out.println("test");
  }

  private void processLine(String content, DocumentIndexed doc) {
    Scanner s = new Scanner(content);

    Set<String> uniqueTermsInDoc = new HashSet<>();
    while (s.hasNext()) {
      uniqueTermsInDoc.add(s.next());
    }

    for (String token : uniqueTermsInDoc) {
      int idx;
      if (_dictionary.containsKey(token)) {
        idx = _dictionary.get(token);
        _postings.get(idx).add(doc._docid);
      } else {
        idx = _terms.size();
        _terms.add(token);
        _dictionary.put(token, idx);
        Vector<Integer> docIds = new Vector<>();
        docIds.add(doc._docid);
        _postings.put(idx, docIds);
      }

    }
    s.close();
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
  }

  @Override
  public Document getDoc(int docid) {
    return null;
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}
   */
  @Override
  public Document nextDoc(Query query, int docid) {
    return null;
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    return 0;
  }

  @Override
  public int corpusTermFrequency(String term) {
    return 0;
  }

  @Override
  public int documentTermFrequency(String term, int docid) {
    SearchEngine.Check(false, "Not implemented!");
    return 0;
  }
}
