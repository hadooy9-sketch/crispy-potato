
public class Student extends User {

    private String year; // maybe we can add a seniority aspect to it, so everyone involved knows how
                         // credible an answer is

    public Student(String userName, String password, String year) {
        super(userName, password);
        this.year = year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String year() {
        return year;
    }

}
