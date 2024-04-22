package Project.Client.Views;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;

import Project.Client.ClientUtils;

public class UserListItem extends JPanel {
    private JEditorPane usernameContainer;
    private JButton lastMessage;

    public UserListItem(long clientId, String clientName) {
        // Set a layout that will organize the children vertically
        this.setLayout(new GridLayout(1, 3));
        this.setName(Long.toString(clientId));
        lastMessage = new JButton();
        
        this.lastMessage.setVisible(false);
        this.add(lastMessage);
        JEditorPane textContainer = new JEditorPane("text/plain", clientName);
        this.usernameContainer = textContainer;
        textContainer.setEditable(false);
        textContainer.setName(Long.toString(clientId));

        ClientUtils.clearBackground(textContainer);
        this.add(textContainer);
    }


    public void setLastMessage(long clientId) {
        if (getName().equals(Long.toString(clientId))) {
            lastMessage.setVisible(true);
            lastMessage.setBackground(Color.GREEN);
        } else {
            lastMessage.setVisible(false);
        }
    }
}