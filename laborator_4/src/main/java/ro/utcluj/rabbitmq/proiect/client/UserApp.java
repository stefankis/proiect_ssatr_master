/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ro.utcluj.rabbitmq.proiect.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import ro.utcluj.rabbitmq.proiect.RabbitMQUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 *
 * @author kisst
 */

public class UserApp extends javax.swing.JFrame {

    private DefaultListModel<String> ticketListModel;
    private JList<String> ticketList;
    private JTextArea ticketDetailsArea;
    private JTextField newCommentField;
    private JButton newTicketButton, addCommentButton;
    private HashMap<String, List<String>> ticketCommentsMap;
    private RabbitMQUtil rabbitMQUtil;
    private static final String TICKET_EXCHANGE_NAME = "ticketUpdates";
    private static final String COMMENT_EXCHANGE_NAME = "commentUpdates";
    private JLabel typingLabel = new JLabel();
    public UserApp() {
        rabbitMQUtil = new RabbitMQUtil();
        ticketCommentsMap = new HashMap<>();
        initializeUI();
        startListeningForCommentUpdates();
        startListeningForTypingUpdates(typingLabel);
    }

    private void initializeUI() {
        setTitle("Sistem pentru tichete de suport");
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

        newTicketButton = new JButton("Deschide un tichet nou");
        newTicketButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewTicket();
            }
        });
        
        newCommentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                String[] numeOrdinale = {"primul", "al doilea", "al treilea", "al patrulea", "al cincilea", "al saselea", "al saptelea"};
                String ordineTicket = "";
                String selectedTicket = ticketList.getSelectedValue();
                if (selectedTicket != null) {
                    int selectedIndex = ticketList.getSelectedIndex();
                    if (selectedIndex >= 0 && selectedIndex < numeOrdinale.length) {
                        ordineTicket = numeOrdinale[selectedIndex];
                    }
                    if (Character.isLetterOrDigit(e.getKeyChar())) {
                        if (selectedIndex == 0) {
                            sendTypingUpdate("Clientul scrie un mesaj în " + ordineTicket + " tichet, cu titlul '" + selectedTicket + "'. Nu uita să lași un update!");
                        } else {
                            sendTypingUpdate("Clientul scrie un mesaj în tichetul " + ordineTicket + " , cu titlul '" + selectedTicket + "'. Nu uita să lași un update!");
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

        addCommentButton = new JButton("Adaugă mesajul la tichet");
        addCommentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addCommentToTicket();
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
        add(newTicketButton, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void createNewTicket() {
        String ticketContent = JOptionPane.showInputDialog(this, "Introdu subiectul acestui tichet pe scurt:");
        if (ticketContent != null && !ticketContent.trim().isEmpty()) {
            ticketListModel.addElement(ticketContent);

            sendTicketUpdate(ticketContent);
        }
    }
    
    private void sendTicketUpdate(String ticketContent) {
        try (Channel channel = rabbitMQUtil.createChannel()) {
            String message = "New Ticket: " + ticketContent; 
            channel.exchangeDeclare(TICKET_EXCHANGE_NAME, "fanout", true);
            channel.basicPublish(TICKET_EXCHANGE_NAME, "", null, message.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void viewTicketDetails() {
        String selectedTicket = ticketList.getSelectedValue();
        if (selectedTicket != null) {
            ticketDetailsArea.setText("Detalii despre tichet \n" + "Subiect: " + selectedTicket + "\nConținutul tichetului \n");
            List<String> comments = ticketCommentsMap.getOrDefault(selectedTicket, new ArrayList<>());
            if (comments.isEmpty()) {
                ticketDetailsArea.append("\nNu există conversații active pentru acest tichet.");
            }
            for (String comment : comments) {
                ticketDetailsArea.append("\n" + comment);
            }
        }
    }

    private void addCommentToTicket() {
        String comment = newCommentField.getText();
        if (comment != null && !comment.trim().isEmpty()) {
            String selectedTicket = ticketList.getSelectedValue();
            if (selectedTicket != null) {
                sendCommentUpdate(selectedTicket, "Client: " + comment);
                newCommentField.setText("");
            }
        }
    }
    
    private void sendTypingUpdate(String message) {
        try (Channel channel = rabbitMQUtil.createChannel()) {
            channel.exchangeDeclare("userTypingUpdates", "fanout", true);
            channel.basicPublish("userTypingUpdates", "", null, message.getBytes("UTF-8"));
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
            channel.exchangeDeclare("supportTypingUpdates", "fanout", true);
            channel.queueBind(queueName, "supportTypingUpdates", "");

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
                        if(comment.contains("Client: ")) {
                            comment = comment.replace("Client: ", "Me: ");
                        }
                        System.out.println("Mesaj client: " + comment);
                        List<String> comments = ticketCommentsMap.getOrDefault(ticket, new ArrayList<>());
                        if (!comments.contains(comment)) {
                            comments.add(comment);
                            ticketCommentsMap.put(ticket, comments);
                            if (ticket.equals(ticketList.getSelectedValue())) {
                                viewTicketDetails();
                            }
                        }
                    }
                });
            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        } catch (Exception e) {
            e.printStackTrace();
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
        jButton2 = new javax.swing.JButton();
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

        jButton2.setText("jButton1");

        jLabel1.setText("jLabel1");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 706, Short.MAX_VALUE)
                        .addComponent(jTextField1, javax.swing.GroupLayout.Alignment.LEADING)))
                .addContainerGap(33, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(34, 34, 34)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(531, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addGap(2, 2, 2)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(99, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(421, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(105, 105, 105)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(UserApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(UserApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(UserApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UserApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new UserApp().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList<String> jList1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
