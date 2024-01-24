/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ro.utcluj.rabbitmq.proiect.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import ro.utcluj.rabbitmq.proiect.RabbitMQUtil;
import java.util.List;

/**
 *
 * @author kisst
 */
public class SupportTeamApp extends javax.swing.JFrame {

    private DefaultListModel<String> ticketListModel;
    private HashMap<String, List<String>> ticketCommentsMap;
    private JList<String> ticketList;
    private JTextArea ticketDetailsArea;
    private JTextField newCommentField;
    private JButton addCommentButton;
    private RabbitMQUtil rabbitMQUtil;
    private static final String TICKET_EXCHANGE_NAME = "ticketUpdates";
    private static final String COMMENT_EXCHANGE_NAME = "commentUpdates";
    private JLabel typingLabel = new JLabel();
    public SupportTeamApp() {
        rabbitMQUtil = new RabbitMQUtil();
        initializeUI();
        startListeningForTicketUpdates();
        startListeningForCommentUpdates();
        startListeningForTypingUpdates(typingLabel);
        ticketCommentsMap = new HashMap<>();
    }
    
    private void initializeUI() {
        setTitle("Support Team - Sistem pentru tichete de suport");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        ticketListModel = new DefaultListModel<>();
        ticketList = new JList<>(ticketListModel);
        ticketList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ticketList.addListSelectionListener(e -> viewTicketDetails());

        ticketDetailsArea = new JTextArea();
        ticketDetailsArea.setEditable(false);

        newCommentField = new JTextField();

        addCommentButton = new JButton("Adaugă mesajul la tichet");
        addCommentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addCommentToTicket();
            }
        });
        
        newCommentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (Character.isLetterOrDigit(e.getKeyChar())) {
                    String[] numeOrdinale = {"primul", "al doilea", "al treilea", "al patrulea", "al cincilea", "al saselea", "al saptelea"};
                    String ordineTicket = "";
                    String selectedTicket = ticketList.getSelectedValue();
                    if (selectedTicket != null) {
                        int selectedIndex = ticketList.getSelectedIndex();
                        if (selectedIndex >= 0 && selectedIndex < numeOrdinale.length) {
                            ordineTicket = numeOrdinale[selectedIndex];
                        }
                        if (selectedIndex == 0) {
                            sendTypingUpdate("Echipa de suport scrie un mesaj în " + ordineTicket + " tichet, cu titlul '" + selectedTicket + "'. Nu uita să verifici mesajul!");
                        } else {
                            sendTypingUpdate("Echipa de suport scrie un mesaj în tichetul " + ordineTicket + ", cu titlul '" + selectedTicket + "'. Nu uita să verifici mesajul!");
                        }
                    }
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || newCommentField.getText().isEmpty()) {
                    sendTypingUpdate("");
                }
            }
        });
        
        addCommentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                    sendTypingUpdate("");
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(ticketList), new JScrollPane(ticketDetailsArea));
        splitPane.setDividerLocation(300);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(newCommentField, BorderLayout.CENTER);
        bottomPanel.add(addCommentButton, BorderLayout.EAST);
        bottomPanel.add(typingLabel, BorderLayout.SOUTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void startListeningForTicketUpdates() {
        try {
            Channel channel = rabbitMQUtil.createChannel();
            String queueName = channel.queueDeclare().getQueue();
            channel.exchangeDeclare(TICKET_EXCHANGE_NAME, "fanout", true);
            channel.queueBind(queueName, TICKET_EXCHANGE_NAME, ""); // Legătura cu exchange-ul fanout

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                SwingUtilities.invokeLater(() -> {
                    String[] messageParts = message.split(": ", 2);
                    if (messageParts.length == 2 && "New Ticket".equals(messageParts[0])) {
                        ticketListModel.addElement(messageParts[1]); // Adăugăm doar conținutul tichetului
                    }
                });
            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void addCommentToTicket() {
        String comment = newCommentField.getText();
        if (comment != null && !comment.trim().isEmpty()) {
            String selectedTicket = ticketList.getSelectedValue();
            if (selectedTicket != null) {
                sendCommentUpdate(selectedTicket, "Support: "+ comment);
                newCommentField.setText(""); // Clear the comment field
            }
        }
    }
    
    private void sendTypingUpdate(String message) {
        try (Channel channel = rabbitMQUtil.createChannel()) {
            channel.exchangeDeclare("supportTypingUpdates", "fanout", true); // Schimbați numele schimbului la "supportTypingUpdates"
            channel.basicPublish("supportTypingUpdates", "", null, message.getBytes("UTF-8")); // Schimbați numele schimbului aici
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendCommentUpdate(String ticket, String comment) {
        try (Channel channel = rabbitMQUtil.createChannel()) {
            String message = "Comment on " + ticket + ": " + comment;
            channel.exchangeDeclare(COMMENT_EXCHANGE_NAME, "fanout", true);
            channel.basicPublish(COMMENT_EXCHANGE_NAME, "", null, message.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void startListeningForTypingUpdates(JLabel typingLabel) {
        try {
            Channel channel = rabbitMQUtil.createChannel();
            String queueName = channel.queueDeclare().getQueue();
            channel.exchangeDeclare("userTypingUpdates", "fanout", true);
            channel.queueBind(queueName, "userTypingUpdates", "");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                SwingUtilities.invokeLater(() -> {
                    typingLabel.setText(message);
                });
            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void startListeningForCommentUpdates() {
        try {
            Channel channel = rabbitMQUtil.createChannel();
            String queueName = channel.queueDeclare().getQueue();
            channel.exchangeDeclare(COMMENT_EXCHANGE_NAME, "fanout", true);
            channel.queueBind(queueName, COMMENT_EXCHANGE_NAME, "");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                SwingUtilities.invokeLater(() -> {
                    String[] messageParts = message.split(": ", 2);
                    if (messageParts.length == 2 && messageParts[0].startsWith("Comment on")) {
                        String ticket = messageParts[0].substring("Comment on ".length());
                        String comment = messageParts[1];
                        if(comment.contains("Support: ")) {
                            comment = comment.replace("Support: ", "Me: ");
                        }
                        List<String> commentsModel = ticketCommentsMap.getOrDefault(ticket, new ArrayList<>());
                        if (!commentsModel.contains(comment)) {
                            commentsModel.add(comment);
                            ticketCommentsMap.put(ticket, commentsModel);
                            if (ticket.equals(ticketList.getSelectedValue())) {
                                viewTicketDetails();
                            }
                        }
                        
                        if (ticket.equals(ticketList.getSelectedValue())) {
                            viewTicketDetails();
                        }
                    }
                });
            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void viewTicketDetails() {
        String selectedTicket = ticketList.getSelectedValue();
        if (selectedTicket != null) {
            ticketDetailsArea.setText("Detalii tichet \n" + "Subiect: " + selectedTicket + "\nConținutul tichetului \n");
            List<String> commentsModel = ticketCommentsMap.getOrDefault(selectedTicket, new ArrayList<>());
            if (commentsModel.isEmpty()) {
                ticketDetailsArea.append("\nNu există conversații active pentru acest tichet.");
            }
            for (String comment : commentsModel) {
                ticketDetailsArea.append("\n" + comment);
            }
        }
    }


    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jList1.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(jList1);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        jTextField1.setText("jTextField1");

        jButton1.setText("jButton1");

        jLabel1.setText("jLabel1");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jScrollPane1)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 735, Short.MAX_VALUE)
                        .addComponent(jTextField1)
                        .addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(29, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addGap(3, 3, 3)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(37, 37, 37)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(135, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SupportTeamApp().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList<String> jList1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
