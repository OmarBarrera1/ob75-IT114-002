package Project.Client.Views;

import java.awt.Color;
import java.awt.GridLayout;


import javax.swing.JEditorPane;
import javax.swing.JPanel;


import Project.Client.ClientUtils;

public class UserListItem extends JPanel {
    //UCID - ob75 - April 24, 2024
    private JEditorPane usernameContainer;
    private JPanel lastMessage;
    private JPanel colorClient;

    public UserListItem(long clientId, String clientName) {
        // Set a layout that will organize the children vertically
        this.setLayout(new GridLayout(1, 3));
        this.setName(Long.toString(clientId));
        lastMessage = new JPanel();
        colorClient = new JPanel();

        this.lastMessage.setVisible(false);
        this.colorClient.setVisible(true);

        this.add(lastMessage);
        this.add(colorClient);

        JEditorPane textContainer = new JEditorPane("text/plain", clientName);
        this.usernameContainer = textContainer;
        textContainer.setEditable(false);
        textContainer.setName(Long.toString(clientId));

        ClientUtils.clearBackground(textContainer);
        this.add(textContainer);
    }

    //UCID - ob75 - April 22, 2024
    public void setLastMessage(long clientId) {
        if (getName().equals(Long.toString(clientId))) {
            lastMessage.setVisible(true);
            lastMessage.setBackground(Color.GREEN);
        } else {
            lastMessage.setVisible(false);
        }
    }

    //UCID - ob75 - April 22, 2024
    public void setClientColor(long clientId, boolean isMuted){
    System.out.print(String.format(" %s and %s", clientId, isMuted));
    if (getName().equals(Long.toString(clientId))) {
        if(isMuted){
            colorClient.setVisible(true);
            colorClient.setBackground(Color.GRAY);
        }
        else{
            colorClient.setVisible(false);
        }
        colorClient.revalidate();
        colorClient.repaint();
    }
} 
    
}