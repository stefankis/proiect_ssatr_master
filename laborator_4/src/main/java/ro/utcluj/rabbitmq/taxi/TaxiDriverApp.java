/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ro.utcluj.rabbitmq.taxi;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

/**
 *
 * @author mihai
 */
public class TaxiDriverApp extends javax.swing.JFrame implements DeliverCallback, Runnable{
    
    private String DRIVER_QUEUE_NAME="driver-";
    private ArrayList<String> activeOrders = new ArrayList<>();
    private TaxiDriverApp.ListOrderesModel listModel = new ListOrderesModel();
    private String driverId;
    private String currentOrder = null;
    private long orderTime;
    private long finalTime;
    /**
     * Creates new form TaxiDriverApp
     */
    public TaxiDriverApp() {
        initComponents();
        new Thread(this).start();
    }
    
     public TaxiDriverApp(String driverId) {
        initComponents();
        this.driverId = driverId;
        DRIVER_QUEUE_NAME = "driver-"+driverId;
        this.setTitle("Taxi Driver App: "+driverId);
        new Thread(this).start();
    }
   
   
    public void run(){
        try {
            startReceiver();
        } catch (IOException ex) {
            Logger.getLogger(TaxiDriverApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(TaxiDriverApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Start consuming messages received on driver queue. This is a blocking method and must be called from inside a separate thread similar as client.
     * @throws IOException
     * @throws TimeoutException 
     */
    public void startReceiver() throws IOException, TimeoutException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // Change if your RabbitMQ server is not on localhost
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(DRIVER_QUEUE_NAME, false, false, false, null);
        //!!! Bind this queue to fanout queue "booking-requests". In this way any message sent to fanout queue is redirected to this driver queue.
        channel.queueBind(DRIVER_QUEUE_NAME, "booking-requests", "");
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        System.out.println("Start consuming messages...");
        channel.basicConsume(DRIVER_QUEUE_NAME, true, this, consumerTag -> { });
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
        listOrders = new javax.swing.JList<>();
        bTake = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        tfCurrentOrder = new javax.swing.JTextField();
        bComplete = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        listOrders.setModel(listModel);
        jScrollPane1.setViewportView(listOrders);

        bTake.setText("Take Order");
        bTake.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bTakeActionPerformed(evt);
            }
        });

        jLabel1.setText("Order in progress:");

        tfCurrentOrder.setEditable(false);

        bComplete.setText("Order Complete");
        bComplete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bCompleteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(bComplete, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tfCurrentOrder))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1)
                    .addComponent(bTake, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(tfCurrentOrder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(14, 14, 14)
                        .addComponent(bComplete)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bTake)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Send client confirmation that order has been accepted. 
     * @param destination
     * @param msg
     * @throws Exception 
     */
    public void sendOrderConfirmationToClient(String destination, String msg) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // Change this if your RabbitMQ is not running locally
        try (Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {
            channel.queueDeclare(destination, false, false, false, null);
            channel.basicPublish("", destination, null, msg.getBytes());
            //channel.basicPublish(destination, "", null, msg.getBytes());
            System.out.println(" [x] Sent '" + msg + "'");
        }
    }
    
    public void sendOrderConfirmationToOtherDrivers(String destination, String msg) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // Change this if your RabbitMQ is not running locally
        try (Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {
            channel.basicPublish(destination, "", null, msg.getBytes());
            System.out.println(" [x] Sent '" + msg + "'");
        }
    }
    
    
    private void bTakeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bTakeActionPerformed
        try {
            //we confirm order is accepted by sending to client queue the name of the driver
            currentOrder = "orderconfirmation_"+this.listOrders.getSelectedValue();
            sendOrderConfirmationToClient(currentOrder, driverId);
            //we also inform other drivers the order has been confirmed by sending name of the client with prefix TAKEN on fanout queue, this is turn will be redirected to all drivers who will know to DELETE this order from their list. See bellow handle method. 
            sendOrderConfirmationToOtherDrivers("booking-requests", "TAKEN "+listOrders.getSelectedValue());
            this.tfCurrentOrder.setText(this.listOrders.getSelectedValue());
            listOrders.updateUI();
            this.bTake.setEnabled(false);
            orderTime = System.currentTimeMillis();
            System.out.println("confirmare" + orderTime);
        } catch (Exception ex) {
            Logger.getLogger(TaxiDriverApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_bTakeActionPerformed

    private void bCompleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bCompleteActionPerformed
        this.bTake.setEnabled(true);
        try {
            sendOrderConfirmationToClient(currentOrder, driverId);
            currentOrder = null;
            this.tfCurrentOrder.setText("");
            finalTime = System.currentTimeMillis();
            System.out.println("Cost: " + (finalTime-orderTime)/1000*2.5);
        } catch (Exception ex) {
            Logger.getLogger(TaxiDriverApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }//GEN-LAST:event_bCompleteActionPerformed

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
            java.util.logging.Logger.getLogger(TaxiDriverApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TaxiDriverApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TaxiDriverApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TaxiDriverApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TaxiDriverApp().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bComplete;
    private javax.swing.JButton bTake;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList<String> listOrders;
    private javax.swing.JTextField tfCurrentOrder;
    // End of variables declaration//GEN-END:variables

  
    /**
     * Call when a message is received on driver queue.
     * @param string
     * @param dlvr
     * @throws IOException 
     */
       @Override
        public void handle(String string, Delivery dlvr) throws IOException {
            String message = new String(dlvr.getBody(), "UTF-8");
            
            if(message.startsWith("TAKEN")){
                //if e message starting with TAKEN is received means a order has been confirmed and mut be deleted from the list of waiting order.           
                //we remove the TAKEN prefix and remaingin message is client id whcih will be deleted from array list.
                message = message.split("\\s+")[1];
                activeOrders.remove(message);
            }
            else if(!activeOrders.contains(message)){
                //if message not start with TAKEN it is a new order and we add it in array list
                activeOrders.add(message);
            }
            
            //we call this method to refresh the user interface
            this.listOrders.updateUI();
        }
        
    /**
     * This class is used to client a list model required to display list of order inside JList UI component. 
     * 
     */
    class ListOrderesModel implements ListModel{
  
        @Override
        public int getSize() {
            return activeOrders.size();
        }
        @Override
        public Object getElementAt(int index) {
            return activeOrders.get(index);
        }
        @Override
        public void addListDataListener(ListDataListener l) {
            //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
        @Override
        public void removeListDataListener(ListDataListener l) {
            //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }
}
