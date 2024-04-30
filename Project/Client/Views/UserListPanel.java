package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;


public class UserListPanel extends JPanel {
    private JPanel userListArea;
    private static Logger logger = Logger.getLogger(UserListPanel.class.getName());


    public UserListPanel() {
        super(new BorderLayout(10, 10));
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentY(Component.BOTTOM_ALIGNMENT);

        // wraps a viewport to provide scroll capabilities
        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        // scroll.setBorder(BorderFactory.createEmptyBorder());
        // no need to add content specifically because scroll wraps it

        userListArea = content;

        wrapper.add(scroll);
        this.add(wrapper, BorderLayout.CENTER);

        userListArea.addContainerListener(new ContainerListener() {

            @Override
            public void componentAdded(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    userListArea.revalidate();
                    userListArea.repaint();
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    userListArea.revalidate();
                    userListArea.repaint();
                }
            }

        });
    }

    protected void addUserListItem(long clientId, String clientName) {
        //UCID - ob75 - April 22, 2024
        logger.log(Level.INFO, "Adding user to list: " + clientName);
        UserListItem uli = new UserListItem(clientId, clientName);
        uli.setPreferredSize(new Dimension(userListArea.getWidth(), 20));
        uli.setMaximumSize(uli.getPreferredSize());
        // add to container
        userListArea.add(uli);
    }

    protected void removeUserListItem(long clientId) {
        logger.log(Level.INFO, "removing user list item for id " + clientId);
        Component[] cs = userListArea.getComponents();
        for (Component c : cs) {
            if (c.getName().equals(clientId + "")) {
                userListArea.remove(c);
                break;
            }
        }
    }

    protected void clearUserList() {
        Component[] cs = userListArea.getComponents();
        for (Component c : cs) {
            userListArea.remove(c);
        }
    }

    //UCID - ob75 - April 22, 2024
    protected void highlightLastMessage(long clientId) {
        Component[] cs = userListArea.getComponents();
        for (Component c : cs) {
            if (c instanceof UserListItem) {
                UserListItem uli = (UserListItem) c;
                uli.setLastMessage(clientId);
            }
        }
    }
    //UCID - ob75 - April 24, 2024
    protected void colorClients(long clientId, boolean isMuted){
        Component[] cs = userListArea.getComponents();
        for (Component c : cs) {
            if (c instanceof UserListItem) {
                UserListItem uli = (UserListItem) c;
                uli.setClientColor(clientId, isMuted);
            }
        }
    } 
}
