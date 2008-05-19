import java.awt.*;
import java.awt.event.*;

public class HostsFilePasswordDialog extends Dialog implements ActionListener {

  private Button ok;
  private Button cancel;
  private TextField passField;
  private String password;
  private boolean result;
  
  public HostsFilePasswordDialog(Frame parent, boolean isSaving) {
    super(parent);
    
    result = false;
    password = null;

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    setLayout(gridbag);
    setModal(true);
    setResizable(false);
    Point loc = parent.getLocation();
    Dimension dim = parent.getSize();
    loc.x += (dim.width/2)-50;
    loc.y += (dim.height/2)-50;
    setLocation(loc);
    
    // Password:
    Label p = new Label("Password:", Label.CENTER);
    p.setFont(new Font(null, Font.BOLD, 24));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(p, c);
    add(p);
    // To protect login info...
    if(isSaving) {
      Label e1 = new Label("To protect login information", Label.CENTER);
      Label e2 = new Label("enter a password to encrypt", Label.CENTER);
      Label e3 = new Label("this file with.  (To leave file", Label.CENTER);
      Label e4 = new Label("unencrypted, click NONE)", Label.CENTER);
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(e1, c);
      gridbag.setConstraints(e2, c);
      gridbag.setConstraints(e3, c);
      gridbag.setConstraints(e4, c);
      add(e1); add(e2); add(e3); add(e4);
    }
    // [__________________]
    passField = new TextField(20);
    passField.setEchoChar('*');
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(passField, c);
    add(passField);
    // ( None )
    if(isSaving) {
      cancel = new Button("None");
      cancel.addActionListener(this);
      c.anchor = GridBagConstraints.CENTER;
      c.fill = GridBagConstraints.NONE;
      c.gridwidth = 1;
      gridbag.setConstraints(cancel, c);
      add(cancel);
    }
    // ( OK )
    ok = new Button("OK");
    ok.addActionListener(this);
    c.anchor = GridBagConstraints.CENTER;
    c.fill = GridBagConstraints.NONE;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(ok, c);
    add(ok);

    pack();
    //setLocation(100, 200);
    setVisible(true);
  }
  
  public boolean getResult() {
    return result;
  }
  
  public String getPassword() {
    return password;
  }
  
  public void actionPerformed(ActionEvent evt) {
    if(evt.getSource() == ok) {
      result = true;
    } else {
      result = false;
    }
    password = passField.getText();
    this.setVisible(false);
    this.dispose();
  }
  
}