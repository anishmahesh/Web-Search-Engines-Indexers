package edu.nyu.cs.cs2580;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;
  private int _documentTermFrequency;

  public void setDocTermFrequency(int docTermFrequency){
    _documentTermFrequency = docTermFrequency;
  }

  public int getDocTermFrequency(){
    return _documentTermFrequency;
  }

  public DocumentIndexed(int docid) {
    super(docid);
  }
}
