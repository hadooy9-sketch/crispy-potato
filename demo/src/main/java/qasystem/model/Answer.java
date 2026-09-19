package qasystem.model;

public class Answer {
    // Fields
    private String text;
    private String submittedBy;
    private ArrayList<Review> reviews;

    // Constructor
    public Answer(String text, String submittedBy)

    // Public Methods

    public String getText();

    public String getSubmittedBy();

    public void addReview(Review review);

    public ArrayList<Review> getReviews();

    public boolean editAnswer(String newText);
}

