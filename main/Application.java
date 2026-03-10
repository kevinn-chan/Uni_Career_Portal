package internship_project;

import internship_project.enums.ApplicationStatus; // PENDING, SUCCESSFUL, UNSUCCESSFUL
import internship_project.enums.WithdrawalRequest;

public class Application {
  private final String studentId;
  private final int internshipId;
  private ApplicationStatus status;  // default PENDING
  private WithdrawalRequest withdrawalStatus;
  private boolean accepted;          // student’s final acceptance

  public Application(String studentId, int internshipId) {
    this.studentId = studentId;
    this.internshipId = internshipId;
    this.status = ApplicationStatus.PENDING;
    this.accepted = false;
    this.withdrawalStatus = WithdrawalRequest.REJECTED;
  }

  public String getStudentId() { return studentId; }
  public int getInternshipId() { return internshipId; }
  public ApplicationStatus getStatus() { return status; }
  public void setStatus(ApplicationStatus s) { this.status = s; }
  public boolean isAccepted() { return accepted; }
  public void setAccepted(boolean a) { this.accepted = a; }
  public WithdrawalRequest getWithdrawalStatus() { return withdrawalStatus; }
  public void setWithdrawalStatus(WithdrawalRequest w) { this.withdrawalStatus = w; }

}
