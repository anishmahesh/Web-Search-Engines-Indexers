package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer {

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

  private Map<Integer, Vector<Byte>> _postings = new HashMap<>();

  private Map<Integer, Vector<Integer>> _decodedPostings = new HashMap<>();

  //Stores the index of each docid in posting list in sorted order(acc to doc ids)
  private Map<Integer,Vector<Integer>> _skipList = new HashMap<>();

  private Query queryObject = null;


  public IndexerInvertedCompressed(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
    String dir = _options._corpusPrefix;
    File[] fileNames = new File(dir).listFiles();
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
    System.out.println("Inside Directory : "+dir);
    File[] fileNames = new File(dir).listFiles();
    System.out.println("Construct index from: " + dir);
    HTMLParse htmlParse = new HTMLParse();
    System.out.println("File List : "+fileNames);
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
        processFiles(dir+file.getName());
      }else if(file.isHidden()){
        continue;
      }
    }
  }

  private void processDocument(String content, DocumentIndexed doc) {
    Scanner s = new Scanner(content);

    Map<String, Vector<Byte>> termOccurenceMap = new HashMap<>();
    Map<Integer,Integer> lastTermIndex = new HashMap<>();

    int offset = 0;

    while (s.hasNext()) {

      String term = s.next();

      if (!termOccurenceMap.containsKey(term)) {
        Vector<Integer> occurence = new Vector<>();
        Vector<Integer> skipIndex =  new Vector<>();
        occurence.add(doc._docid);
        occurence.add(1);
        occurence.add(offset);
        lastTermIndex.put(_dictionary.get(term), offset);
        skipIndex.add(0);
        _skipList.put(_dictionary.get(term),skipIndex);
        termOccurenceMap.put(term, IndexCompressor.vByteEncoder(occurence));
      } else {
        Vector<Integer> occurence = IndexCompressor.vByteDecoder(termOccurenceMap.get(term));
        occurence.set(1, occurence.get(1) + 1);

        //Magic happens here
        _skipList.get(_dictionary.get(term)).add(termOccurenceMap.size());
        int currentPointer = lastTermIndex.get(term);
        occurence.add(offset-currentPointer);
        lastTermIndex.put(_dictionary.get(term),offset);
      }
      offset++;
    }

    doc.setTotalTerms(offset);

    for (String token : termOccurenceMap.keySet()) {
      int idx;
      if (_dictionary.containsKey(token)) {
        idx = _dictionary.get(token);
        _postings.get(idx).addAll(termOccurenceMap.get(token));
      } else {
        idx = _terms.size();
        _terms.add(token);
        _dictionary.put(token, idx);
        _postings.put(idx, termOccurenceMap.get(token));
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
      if(query instanceof QueryPhrase){
          return nextDocPhrase(query, docid);
      } else {
          return nextDocIndividualTokens(query, docid);
      }
  }

  public Document nextDocIndividualTokens(Query query, int docid){
      List<Integer> idArray = new ArrayList<>();
      int maxId = -1;
      int sameDocId = -1;
      boolean allQueryTermsInSameDoc = true;
      if( queryObject != query){
          queryObject = query;
          _decodedPostings.clear();
      }
      for(String term : query._tokens){
          if(!_decodedPostings.containsKey(_dictionary.get(term))){
              _decodedPostings.put(_dictionary.get(term), getPostingListforTerm(term));
          }
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

  public Document nextDocPhrase(Query query, int docid){
      return null;
  }

  public int next(String queryTerm, int docid){
    return binarySearchResultIndex(queryTerm, docid);
  }

  public Vector<Integer> getPostingListforTerm(String term){
    return IndexCompressor.vByteDecoder(_postings.get(_dictionary.get(term)));
  }

  public Vector<Integer> getDecodedPostingListforTerm(String term){
    return _decodedPostings.get(_dictionary.get(term));
  }

  public Vector<Integer> getSkipListforTerm(String term){
    return _skipList.get(_dictionary.get(term));
  }

  private int binarySearchResultIndex(String term, int current){
    Vector<Integer> PostingList = getDecodedPostingListforTerm(term);
    Vector<Integer> SkipList = getSkipListforTerm(term);
    int lt = SkipList.size()-1;
    if(lt == 0 || PostingList.get(SkipList.get(lt)) <= current){
      return -1;
    }
    if(PostingList.get(1)>current){
      return PostingList.get(1);
    }
    return PostingList.get(binarySearch(PostingList,SkipList,1,lt,current,term));
  }

  private int binarySearch(Vector<Integer> PostingList, Vector<Integer> SkipList, int low, int high, int current,String term){
    int mid;
    while(high - low > 1) {
      mid = (low + high) / 2;
      if (PostingList.get(SkipList.get(mid)) <= current) {
        low = mid;
      } else {
        high = mid;
      }
    }
    return SkipList.get(high);
  }

  public int corpusDocFrequencyByTerm(String term) {
    Vector<Integer> PostingList = getPostingListforTerm(term);
    int corpusDocFrequencyByTerm = 0;
    for(int i=0; i< PostingList.size()-1;){
      corpusDocFrequencyByTerm++;
      i += PostingList.get(i+1) + 2;
    }
    return corpusDocFrequencyByTerm;
  }

  @Override
  public int corpusTermFrequency(String term) {
    Vector<Integer> PostingList = getPostingListforTerm(term);
    int corpusTermFrequency = 0;
    for(int i=0; i< PostingList.size()-1;){
      corpusTermFrequency += PostingList.get(i+1);
      i += PostingList.get(i+1) + 2;
    }
    return corpusTermFrequency;
  }

  /**
   * @CS2580: Implement this to work with your RankerFavorite.
   */
  @Override
  public int documentTermFrequency(String term, int docid) {
    Vector<Integer> PostingList = getPostingListforTerm(term);
    for(int i=0; i< PostingList.size()-1;){
      if(docid == PostingList.get(i)){
        return  PostingList.get(i+1);
      } else {
        i += PostingList.get(i+1) + 2;
      }
    }
    return 0;
  }
}
