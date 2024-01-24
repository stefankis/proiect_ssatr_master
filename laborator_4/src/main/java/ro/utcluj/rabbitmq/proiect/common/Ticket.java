package ro.utcluj.rabbitmq.proiect.common;

import java.util.ArrayList;
import java.util.List;

public class Ticket {
    private String ticketId;
    private String content;
    private List<Comment> comments;
    private String status;

    public Ticket(String ticketId, String content) {
        this.ticketId = ticketId;
        this.content = content;
        this.comments = new ArrayList<>();
        this.status = "Open"; // Default status
    }

    // Getters
    public String getTicketId() {
        return ticketId;
    }

    public String getContent() {
        return content;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Add a comment to the ticket
    public void addComment(Comment comment) {
        if (comment != null) {
            comments.add(comment);
        }
    }

    // Override toString() method for easy printing
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ticket ID: ").append(ticketId).append("\n");
        sb.append("Content: ").append(content).append("\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Comments:\n");
        for (Comment comment : comments) {
            sb.append(comment.toString()).append("\n");
        }
        return sb.toString();
    }
}
