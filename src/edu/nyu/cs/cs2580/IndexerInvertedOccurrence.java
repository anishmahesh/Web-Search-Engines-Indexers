package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import edu.nyu.cs.cs2580.SearchEngine.Options;
import javafx.geometry.Pos;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer implements Serializable {

  private static int FILE_COUNT_FOR_INDEX_SPLIT = 500;

  private static int TERM_COUNT_FOR_INDEX_SPLIT = 500;

  int indexCount = 0;

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

  //Stores the index of each docid in posting list in sorted order(acc to doc ids)
  private Map<Integer,Vector<Integer>> _skipList = new HashMap<>();

  public IndexerInvertedOccurrence() {
  }

  public IndexerInvertedOccurrence(Options options) {
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

    HTMLParse htmlParse = new HTMLParse();
    int fileNum = 0;
    for (File fileName : fileNames) {
      HTMLDocument htmlDocument = htmlParse.getDocument(fileName);
      DocumentIndexed doc = new DocumentIndexed(_documents.size());

//      System.out.println(fileName.getName());
      processDocument(htmlDocument.getBodyText(), doc);

      doc.setTitle(htmlDocument.getTitle());
      doc.setUrl(htmlDocument.getUrl());
      doc.setDocTermFrequency(1);
      _documents.add(doc);
      ++_numDocs;

      if (fileNum == FILE_COUNT_FOR_INDEX_SPLIT) {
        indexCount++;
        System.out.println("Constructing partial index number: " + indexCount);

        persistToFile(indexCount);
        fileNum = 0;
      }

      fileNum++;

    }

    System.out.println("Created partial indexes. Now merging them");

    mergeIndex();

    System.out.println("Splitting index file based on number of terms");

    splitIndexFile();

    System.out.println(
            "Indexed " + Integer.toString(_numDocs) + " docs with " +
                    Long.toString(_terms.size()) + " terms.");

    _postings = null;

    String indexFile = _options._indexPrefix + "/objects.idx";
    System.out.println("Store other objects to: " + indexFile);
    ObjectOutputStream writer =
            new ObjectOutputStream(new FileOutputStream(indexFile));
    writer.writeObject(this);
    writer.close();

  }

  private void splitIndexFile() throws IOException {
    String indexFile = _options._indexPrefix + "/corpus.tsv";
    BufferedReader reader = new BufferedReader(new FileReader(indexFile));

    String partFile = _options._indexPrefix + "/index-part-0.tsv";
    BufferedWriter writer = new BufferedWriter(new FileWriter(partFile, true));

    int count = -1;
    int partCount = 0;
    String line;
    while((line = reader.readLine()) != null) {
      count++;

      if (count == TERM_COUNT_FOR_INDEX_SPLIT) {
        count = 0;
        partCount++;
        writer.close();
        partFile = _options._indexPrefix + "/index-part-" + partCount + ".tsv";
        writer = new BufferedWriter(new FileWriter(partFile, true));
        writer.flush();
      }

      writer.write(line + "\n");
      line = reader.readLine();
      writer.write(line + "\n");


    }

    reader.close();
    writer.close();
    File index = new File(indexFile);
    index.delete();
  }

  private void mergeIndex() throws IOException {
    String indexFile = _options._indexPrefix + "/corpus.tsv";
    String firstFile = _options._indexPrefix + "/tempIndex1.tsv";

    File index = new File(indexFile);
    File first = new File(firstFile);

    for (int i = 2; i <= indexCount; i++) {
      String secondFile = _options._indexPrefix + "/tempIndex" + i + ".tsv";

      File second = new File(secondFile);

      File mergedFile = mergeFiles(first, second);

      first.delete();
      second.delete();
      mergedFile.renameTo(index);
      first = new File(indexFile);
    }

  }

  private File mergeFiles(File first, File second) throws IOException {
    String tempFile = _options._indexPrefix + "/temp.tsv";

    File temp = new File(tempFile);
    BufferedWriter writer = new BufferedWriter(new FileWriter(temp, true));

    BufferedReader firstReader = new BufferedReader(new FileReader(first));
    BufferedReader secondReader = new BufferedReader(new FileReader(second));

    String lineInFirstFile = firstReader.readLine();
    String lineInSecondFile = secondReader.readLine();

    while ((lineInFirstFile != null) && (lineInSecondFile != null)) {

      if (Integer.parseInt(lineInFirstFile) < Integer.parseInt(lineInSecondFile)) {
        writer.write(lineInFirstFile + "\n");
        lineInFirstFile = firstReader.readLine();
        writer.write(lineInFirstFile + "\n");

        lineInFirstFile = firstReader.readLine();
      }
      else if (Integer.parseInt(lineInSecondFile) > Integer.parseInt(lineInFirstFile)) {
        writer.write(lineInSecondFile + "\n");
        lineInSecondFile = secondReader.readLine();
        writer.write(lineInSecondFile + "\n");

        lineInSecondFile = secondReader.readLine();
      }
      else {
        writer.write(lineInFirstFile + "\n");
        lineInFirstFile = firstReader.readLine();
        lineInSecondFile = secondReader.readLine();
        writer.write(lineInFirstFile + "\t" + lineInSecondFile + "\n");

        lineInFirstFile = firstReader.readLine();
        lineInSecondFile = secondReader.readLine();
      }

    }

    while (lineInFirstFile != null) {
      writer.write(lineInFirstFile + "\n");
      lineInFirstFile = firstReader.readLine();
    }

    while (lineInSecondFile != null) {
      writer.write(lineInSecondFile + "\n");
      lineInSecondFile = secondReader.readLine();
    }

    firstReader.close();
    secondReader.close();
    writer.close();

    return temp;
  }

  private void persistToFile(int index) throws IOException {
    String indexFile = _options._indexPrefix + "/tempIndex" + index + ".tsv";
    BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));

    List<Integer> termIds = new ArrayList<>();
    termIds.addAll(_postings.keySet());
    Collections.sort(termIds);

    for (Integer termId: termIds) {
      writer.write(termId.toString() + "\n");

      Vector<Integer> docOccs = _postings.get(termId);
      for (int i = 0; i < docOccs.size(); i++) {

        writer.write(docOccs.get(i).toString() + "\t");
      }
      writer.write("\n");
    }

    writer.close();
    _postings.clear();
  }

  private void processDocument(String content, DocumentIndexed doc) {
    Scanner s = new Scanner(content);

    Map<String, Vector<Integer>> termOccurenceMap = new HashMap<>();

    int offset = 0;
    Stemmer stemmer = new Stemmer();
    while (s.hasNext()) {
      String term = s.next();
      stemmer.add(term.toCharArray(), term.length());
      stemmer.stem();
      term = stemmer.toString();

      if (!termOccurenceMap.containsKey(term)) {
        Vector<Integer> occurence = new Vector<>();
        occurence.add(doc._docid);
        occurence.add(1);
        occurence.add(offset);
        termOccurenceMap.put(term, occurence);
      }
      else {
        Vector<Integer> occurence = termOccurenceMap.get(term);
        occurence.set(1, occurence.get(1) + 1);
        occurence.add(offset);
      }
      offset++;
    }

    for (String token : termOccurenceMap.keySet()) {
      int idx;
      if (_dictionary.containsKey(token)) {
        idx = _dictionary.get(token);
      } else {
        idx = _terms.size();
        _terms.add(token);
        _dictionary.put(token, idx);
      }

      if (_postings.containsKey(idx)) {
        _postings.get(idx).addAll(termOccurenceMap.get(token));
      }
      else {
        _postings.put(idx, termOccurenceMap.get(token));
      }

    }
    s.close();
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
    String indexFile = _options._indexPrefix + "/objects.idx";
    System.out.println("Loading index objects other than postings list from: " + indexFile);

    ObjectInputStream reader =
            new ObjectInputStream(new FileInputStream(indexFile));
    IndexerInvertedOccurrence loaded = (IndexerInvertedOccurrence) reader.readObject();

    this._skipList = loaded._skipList;
    // Compute numDocs and totalTermFrequency b/c Indexer is not serializable.
    this._numDocs = _documents.size();

    this._dictionary = loaded._dictionary;
    this._terms = loaded._terms;
    this._termCorpusFrequency = loaded._termCorpusFrequency;
    this._termDocFrequency = loaded._termDocFrequency;
    this._documents = loaded._documents;
    reader.close();
  }

  @Override
  public Document getDoc(int docid) {
    return null;
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}.
   */
  @Override
  public Document nextDoc(Query query, int docid) {

      System.out.println("loading individual indexes");
      for(String token:query._tokens){
        if(!_postings.containsKey(token)){
          try {
            loadIndexOnFlyForTerm(token);
          } catch (IOException e) {
            e.printStackTrace();
          } catch (ClassNotFoundException e) {
            e.printStackTrace();
          }
        }
      }
    System.out.println("mini indexes loaded");

      if(query instanceof QueryPhrase){
          return nextDocPhrase(query, docid);
      } else {
          return nextDocIndividualTokens(query, docid);
      }
  }

  public Document nextDocIndividualTokens(Query query, int docid) {
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

  public Document nextDocPhrase(Query query, int docid){
      return null;
  }

  public int next(String queryTerm, int docid){
    return binarySearchResultIndex(queryTerm, docid);
  }

  public Vector<Integer> getPostingListforTerm(String term){
    return _postings.get(_dictionary.get(term));
  }

  public Vector<Integer> getSkipListforTerm(String term){
    return _skipList.get(_dictionary.get(term));
  }

  private int binarySearchResultIndex(String term, int current){
    Vector <Integer> PostingList = getPostingListforTerm(term);
    Vector <Integer> SkipList = getSkipListforTerm(term);
    int lt = SkipList.size()-1;
    if(lt == 0 || PostingList.get(SkipList.get(lt)) <= current){
      return -1;
    }
    if(PostingList.get(1)>current){
      return PostingList.get(1);
    }
    return PostingList.get(binarySearch(PostingList,SkipList,1,lt,current));
  }

  private int binarySearch(Vector<Integer> PostingList, Vector<Integer> SkipList, int low, int high, int current){
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

  @Override
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

  public int totalTermsInDocument(int docid) {
    return 1;
  }

  public void loadIndexOnFlyForTerm(String term) throws IOException, ClassNotFoundException {
    int termId = _dictionary.get(term);
    loadMiniIndex((int)(termId/TERM_COUNT_FOR_INDEX_SPLIT));
  }

  private void loadMiniIndex(int indexNo)throws FileNotFoundException{

    File idxFolder = new File(_options._indexPrefix);
    File[] indexFiles= idxFolder.listFiles();

    if(indexNo < indexFiles.length) {
      StringBuilder fileName = new StringBuilder(_options._indexPrefix).append("index-part-").append(indexNo).append(".tsv");

      FileInputStream in = new FileInputStream(fileName.toString());
      BufferedReader br = new BufferedReader(new InputStreamReader(in));

      try {

        Scanner sc = new Scanner(new File(fileName.toString()));

        while (sc.hasNext()) {
          int termId = Integer.parseInt(sc.next());
          int docid = Integer.parseInt(sc.next());
          int noOfOccurances = Integer.parseInt(sc.next());
          Vector<Integer> termPostingList = new Vector<>();
          termPostingList.add(docid);
          termPostingList.add(noOfOccurances);
          for(int i=0;i<noOfOccurances;i++){
            termPostingList.add(Integer.parseInt(sc.next()));
          }
          _postings.put(termId,termPostingList);
        }
        sc.close();
      }
      catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

}
