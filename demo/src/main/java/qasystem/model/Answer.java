package qasystem.model;
import java.util.ArrayList;
public class Answer {
    // Fields
    private String text;
    private String submittedBy;
    private ArrayList<Review> reviews;

    // Constructor
    public Answer(String text, String submittedBy) {
        this.text = text;
        this.submittedBy = submittedBy;
        this.reviews = new ArrayList<>();
    }

    // Public Methods

    public String getText() {
        return text;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void addReview(Review review) {
        reviews.add(review);
    }

    public ArrayList<Review> getReviews() {
        return reviews;
    }

    public boolean editAnswer(String newText) {
        this.text = newText;
        return true;
    }
}

