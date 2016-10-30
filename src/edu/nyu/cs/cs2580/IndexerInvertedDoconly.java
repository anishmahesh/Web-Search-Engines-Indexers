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
    File[] fileNames = new File(dir).listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return !name.equals(".DS_Store");
      }
    });
    System.out.println("Construct index from: " + dir);

    processFiles(dir);

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


  private void processFiles(String dir) throws IOException {
    File[] fileNames = new File(dir).listFiles();
    HTMLParse htmlParse = new HTMLParse();
    int i=0;
    for (File file : fileNames) {
      if(file.isFile()) {
        i++;
        HTMLDocument htmlDocument = htmlParse.getDocument(file);
        DocumentIndexed doc = new DocumentIndexed(_documents.size());

        processDocument(htmlDocument.getBodyText(), doc);

        doc.setTitle(htmlDocument.getTitle());
        doc.setUrl(htmlDocument.getUrl());
        _documents.add(doc);
        ++_numDocs;
      }else if(file.isDirectory()){
        //not recursively going inside a directory
        continue;
        //processFiles(dir+file.getName());
      }else if(file.isHidden()){
        continue;
      }
    }
  }

  private void processDocument(String content, DocumentIndexed doc) {
    Scanner s = new Scanner(content);

    Set<String> uniqueTermsInDoc = new HashSet<>();
    Stemmer stemmer = new Stemmer();
    while (s.hasNext()) {
      String term = s.next();
      stemmer.add(term.toCharArray(), term.length());
      stemmer.stem();
      uniqueTermsInDoc.add(stemmer.toString());
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
    List<Integer> idArray = new ArrayList<>();
    int maxId = -1;
    int sameDocId = -1;
    boolean allQueryTermsInSameDoc = true;
    for(String term : query._tokens){
      idArray.add(next(term,docid));
    }
    for(int id : idArray){
      if(id == -1){
        return null;
      }
      if(sameDocId == -1){
        sameDocId = id;
      }
      if(id != sameDocId){
        allQueryTermsInSameDoc = false;
      }
      if(id > maxId){
        maxId = id;
      }
      if(allQueryTermsInSameDoc){
        return _documents.get(sameDocId);
      }
    }
    return nextDoc(query, maxId-1);
  }

  public int next(String queryTerm, int docid){
    return binarySearchResultIndex(queryTerm, docid);
  }

  private Vector<Integer> getPostingListOfTerm(String term){
        return _postings.get(_dictionary.get(term));
  }

  private int binarySearchResultIndex(String term, int current){
      Vector <Integer> PostingList = getPostingListOfTerm(term);
      int lt = PostingList.size()-1;
      if(lt == 0 || PostingList.get(lt) <= current){
          return -1;
      }
      if(PostingList.get(1)>current){
          return PostingList.get(1);
      }
      return PostingList.get(binarySearch(PostingList,1,lt,current));
  }

  private int binarySearch(Vector<Integer> PostingList, int low, int high, int current){
    int mid;
    while(high - low > 1) {
      mid = (low + high) / 2;
      if (PostingList.get(mid) <= current) {
        low = mid;
      } else {
        high = mid;
      }
    }
    return high;
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
