import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

// Enum for the three possible roles
enum Role {
    ADMIN,
    STUDENT,
    REVIEWER
}

//single system user
class User {
    String username;
    String password;
    ArrayList<Role> roles;

    User(String username, String password) {
        this.username = username;
        this.password = password;
        this.roles = new ArrayList<>();
    }

    // Assign one or more roles to this user
    void addRole(Role role) {
        // add role to roles list if not already present
    }

    boolean hasRole(Role role) {
        // return true if roles.contains(role)
        return false;
    }
}

// Question asked by a Student, with a list of potential Answers
class Answer {
    String text;
    String submittedBy;
    ArrayList<Review> reviews;

    Answer(String text, String submittedBy) {
        this.text = text;
        this.submittedBy = submittedBy;
        this.reviews = new ArrayList<>();
    }
}

class Review {
    String reviewText;
    String reviewedBy;

    Review(String reviewText, String reviewedBy) {
        this.reviewText = reviewText;
        this.reviewedBy = reviewedBy;
    }
}

class Question {
    String text;
    String askedBy;
    boolean resolved;
    ArrayList<Answer> potentialAnswers;

    Question(String text, String askedBy) {
        this.text = text;
        this.askedBy = askedBy;
        this.resolved = false;
        this.potentialAnswers = new ArrayList<>();
    }
}

// --------------------------------------------------------------------------
// The core system class — equivalent role to "EmployeeList" in the C++ ref.
// Owns all users and questions; exposes the functional I/O operations.
// --------------------------------------------------------------------------
class QASystem {
    private ArrayList<User> users;
    private LinkedList<Question> questions; // linked list, like the C++ reference
    private User currentUser;

    QASystem() {
        users = new ArrayList<>();
        questions = new LinkedList<>();
        currentUser = null;
    }

    // ---- First-user / setup flow -----------------------------------------

    boolean isFirstUser() {
        // TODO: return true if users.isEmpty()
        return users.isEmpty();
    }

    // Special screen for the very first user to set up username/password
    void setupFirstUser(String username, String password) {
        // TODO: create User, assign Role.ADMIN, add to users
        // TODO: require re-login after this (handled by caller/menu flow)
    }

    // ---- Login / logout -----------------------------------------------

    boolean login(String username, String password) {
        // TODO: find matching user, verify password, set currentUser
        // TODO: if user has exactly one role, route straight to that
        //       role's home screen (handled by caller after login returns true)
        return false;
    }

    void logout() {
        // TODO: clear currentUser
    }

    // ---- Admin actions ------------------------------------------------

    void inviteUser(String contactInfo, ArrayList<Role> rolesToAssign) {
        // TODO: generate one-time invite code
        // TODO: set a deadline for joining
        // TODO: store pending invite (new class, e.g., Invitation, if desired)
    }

    // ---- Student actions ------------------------------------------------

    void askQuestion(String text) {
        // TODO: create new Question, add to questions list
        // TODO: return/display list of potential answers (initially empty)
    }

    ArrayList<Question> findRelatedQuestions(String keywordOrText) {
        // TODO: search questions list for similar/related text
        // TODO: return matches so student can avoid duplicate question
        return new ArrayList<>();
    }

    ArrayList<Question> listUnresolvedQuestions() {
        // TODO: filter questions where resolved == false
        return new ArrayList<>();
    }

    void proposeAnswer(Question q, String answerText) {
        // TODO: create new Answer, add to q.potentialAnswers
    }

    // ---- Reviewer actions -----------------------------------------------

    void createReview(Answer answer, String reviewText) {
        // TODO: create new Review, add to answer.reviews
    }

    // ---- Display / output helpers (like displayEmployeeIDs) -------------

    void displayQuestions() {
        // TODO: iterate LinkedList<Question> and print each question + status
    }

    void displayAnswersForQuestion(Question q) {
        // TODO: iterate q.potentialAnswers and print each
    }
}

// --------------------------------------------------------------------------
// Main — menu-driven console I/O loop, same shape as the C++ reference:
// print menu -> read choice -> switch -> call system method -> repeat
// --------------------------------------------------------------------------
public class SystemOutline {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        QASystem system = new QASystem();

        // ---- First-run setup screen (only if no users exist yet) --------
        if (system.isFirstUser()) {
            // TODO: prompt for username/password, call setupFirstUser()
            // TODO: force re-login after setup completes
        }

        boolean running = true;
        while (running) {
            // TODO: if no currentUser -> show login menu
            // TODO: if currentUser has multiple roles -> show role-select menu
            //       else -> route straight to that role's home menu

            System.out.println("Menu:");
            System.out.println("1. Login");
            System.out.println("2. Logout");
            System.out.println("3. [Admin] Invite user");
            System.out.println("4. [Student] Ask a question");
            System.out.println("5. [Student] View related questions");
            System.out.println("6. [Student] View unresolved questions");
            System.out.println("7. [Student] Propose an answer");
            System.out.println("8. [Reviewer] Create a review");
            System.out.println("9. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1: {
                    // TODO: read username/password, call system.login(...)
                    break;
                }
                case 2: {
                    system.logout();
                    break;
                }
                case 3: {
                    // TODO: (admin only) read invite info, call inviteUser(...)
                    break;
                }
                case 4: {
                    // TODO: (student only) read question text, call askQuestion(...)
                    break;
                }
                case 5: {
                    // TODO: (student only) read keyword, call findRelatedQuestions(...)
                    break;
                }
                case 6: {
                    // TODO: (student only) call listUnresolvedQuestions(), display results
                    break;
                }
                case 7: {
                    // TODO: (student only) select a question, read answer text,
                    //       call proposeAnswer(...)
                    break;
                }
                case 8: {
                    // TODO: (reviewer only) select an answer, read review text,
                    //       call createReview(...)
                    break;
                }
                case 9: {
                    System.out.println("Exiting the program.");
                    running = false;
                    break;
                }
                default:
                    System.out.println("Invalid choice. Please try again.");
            }

            System.out.println();
        }

        scanner.close();
    }
}