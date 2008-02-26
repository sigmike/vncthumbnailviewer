//
//  Copyright (C) 2007 David Czechowski.  All Rights Reserved.
//  Copyright (C) 2001-2004 HorizonLive.com, Inc.  All Rights Reserved.
//  Copyright (C) 2002 Constantin Kaplinsky.  All Rights Reserved.
//  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
//
//  This is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This software is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this software; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
//  USA.
//

//
// VncThumbnailViewer.java - a unique VNC viewer.  This class creates an empty frame
// into which multiple vncviewers can be added.
//


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.Math.*;
import java.net.*;

public class VncThumbnailViewer extends Frame
    implements WindowListener, ComponentListener, ContainerListener, MouseListener, ActionListener  {
  public static void main(String argv[])  {    VncThumbnailViewer t = new VncThumbnailViewer();

    String h = new String("");
    String pw = new String("");
    int p = 0;
    for(int i = 0; i < argv.length; i += 2) {
      if(argv.length < (i+2) ) {
        System.out.println("ERROR: No value found for parameter " + argv[i]);
        break;
      }
      String param = argv[i];
      String value = argv[i+1];
      if(param.equalsIgnoreCase("host")) {
        h = value;
      }
//      if(param.equalsIgnoreCase("hostname")) {
//        try {
//          InetAddress ip = InetAddress.getByName(argv[i+1]);
//          h = ip.getHostAddress();
//        } catch(UnknownHostException e) {
//          h = "0.0.0.0";
//        }
//      }
      if(param.equalsIgnoreCase("port")) {
        p = Integer.parseInt(value);
      }
      if(param.equalsIgnoreCase("password")) {
        pw = value;
      }
      if(param.equalsIgnoreCase("encpassword")) {
        pw = AddHostDialog.readEncPassword(value);
      }

      if(h != "" && p != 0 && pw != "") {
        t.launchViewer(h, p, pw, null);
        h = "";
        p = 0;
        pw = "";
      }
    }
    
  }
  
  
  AbstractList viewersList;
  AddHostDialog hostDialog;
  MenuItem newhostMenuItem, loadhostsMenuItem, savehostsMenuItem, exitMenuItem;
  Frame soloViewer;
  int widthPerThumbnail, heightPerThumbnail;
  int thumbnailRowCount;
  VncThumbnailViewer() {
    viewersList = new Vector();
    thumbnailRowCount = 0;
    widthPerThumbnail = 0;
    heightPerThumbnail = 0;

    setTitle("DJC Thumbnail Viewer");
    addWindowListener(this);
    addComponentListener(this);
    addMouseListener(this);

    GridLayout grid = new GridLayout();
    setLayout(grid);
    setSize(Toolkit.getDefaultToolkit().getScreenSize());
    setVisible(true);
    
    setMenuBar(new MenuBar());    getMenuBar().add( createFileMenu() );

    soloViewer = new Frame();
    soloViewer.setSize(200,200);
    soloViewer.addWindowListener(this);
    soloViewer.addComponentListener(this);
    soloViewer.validate();
  }


  public void launchViewer(String host, int port, String password, String user) {
  //public void launchViewer(String host, int port, String password) { // MS-Logon
    String args[] = new String[8]; // FIX-ME: can this be longer (8) than it should be (6)?
    args[0] = "host";
    args[1] = host;
    args[2] = "port";
    args[3] = Integer.toString(port);
    args[4] = "password";
    args[5] = password;
    
    if(user != null) {
      args[6] = "username";
      args[7] = user;
    }

    // launch a new viewer
    VncViewer v = new VncViewer();
    v.mainArgs = args;
    v.inAnApplet = false;
    v.inSeparateFrame = false;
    v.showControls = true;
    v.showOfflineDesktop = true;
    v.vncFrame = this;
    v.init();
    v.options.viewOnly = true;
    v.options.autoScale = true; // false, because ThumbnailViewer maintains the scaling
    v.options.scalingFactor = 10;
    v.addContainerListener(this);
    v.start();
    
    viewersList.add(v);
    addViewer(v);
  }


  void addViewer(VncViewer v) {
    int r = (int)Math.sqrt(viewersList.size() - 1) + 1;//int r = (int)Math.sqrt(this.getComponentCount() - 1) + 1;
    if(r != thumbnailRowCount) {
      thumbnailRowCount = r;
      ((GridLayout)this.getLayout()).setRows(thumbnailRowCount);
//      ((GridLayout)this.getLayout()).setColumns(thumbnailRowCount);
      resizeThumbnails();
    }
    this.add(v);
    this.validate();
  }


  void removeViewer(VncViewer v) {
    this.remove(v);
    this.validate();

    int r = (int)Math.sqrt(viewersList.size() - 1) + 1;//int r = (int)Math.sqrt(this.getComponentCount() - 1) + 1;
    if(r != thumbnailRowCount) {
      thumbnailRowCount = r;
      ((GridLayout)this.getLayout()).setRows(thumbnailRowCount);
//      ((GridLayout)this.getLayout()).setColumns(thumbnailRowCount);
      resizeThumbnails();
    }
  }


  void soloHost(VncViewer v) {
    if(v.vc == null)
      return;

    if(soloViewer.getComponentCount() > 0)
      soloHostClose();

    soloViewer.setVisible(true);
    soloViewer.setTitle(v.host);
    this.remove(v);
    soloViewer.add(v);
    this.validate();
    soloViewer.validate();

    if(!v.rfb.closed()) {
      v.vc.enableInput(true);
    }
    updateCanvasScaling(v, soloViewer.getWidth(), soloViewer.getHeight());
  }


  void soloHostClose() {
    VncViewer v = (VncViewer)soloViewer.getComponent(0);
    v.enableInput(false);
    updateCanvasScaling(v, widthPerThumbnail, heightPerThumbnail);
    soloViewer.removeAll();
    addViewer(v);
    soloViewer.setVisible(false);
  }


  private void updateCanvasScaling(VncViewer v, int maxWidth, int maxHeight) {
    int fbWidth = v.vc.rfb.framebufferWidth;
    int fbHeight = v.vc.rfb.framebufferHeight;
    int f1 = maxWidth * 100 / fbWidth;
    int f2 = maxHeight * 100 / fbHeight;
    int sf = Math.min(f1, f2);
    if (sf > 100) {
      sf = 100;
    }

    v.vc.maxWidth = maxWidth;
    v.vc.maxHeight = maxHeight;
    v.vc.scalingFactor = sf;
    v.vc.scaledWidth = (fbWidth * sf + 50) / 100;
    v.vc.scaledHeight = (fbHeight * sf + 50) / 100;

    //Fix: invoke a re-paint of canvas?
    //Fix: invoke a re-size of canvas?
    //Fix: invoke a validate of viewer's gridbag?
  }


  void resizeThumbnails() {
    int newWidth = this.getWidth() / thumbnailRowCount;
    int newHeight = (this.getHeight() - 16*thumbnailRowCount) / thumbnailRowCount; // 50*thumbnailRowCount

    if(newWidth != widthPerThumbnail || newHeight != heightPerThumbnail) {
      widthPerThumbnail = newWidth;
      heightPerThumbnail = newHeight;

      ListIterator l = viewersList.listIterator();
      while(l.hasNext()) {
        VncViewer v = (VncViewer)l.next();
        //v.
        if(!soloViewer.isAncestorOf(v)) {
          if(v.vc != null) { // if the connection has been established
            updateCanvasScaling(v, widthPerThumbnail, heightPerThumbnail);
          }
        }
      }

    }

  }

  private void quit() {
    // Called by either File->Exit or Closing of the main window
    System.out.println("Closing window");
    ListIterator l = viewersList.listIterator();
    while(l.hasNext()) {
      ((VncViewer)l.next()).disconnect();
    }
    this.dispose();

    System.exit(0);
  }


  private Menu createFileMenu()  {
    Menu fileMenu = new Menu("File");
    newhostMenuItem = new MenuItem("Add New Host");
    loadhostsMenuItem = new MenuItem("Load List of Hosts");
    savehostsMenuItem = new MenuItem("Save List of Hosts");
    exitMenuItem = new MenuItem("Exit");

    newhostMenuItem.addActionListener(this);
    loadhostsMenuItem.addActionListener(this);
    savehostsMenuItem.addActionListener(this);
    exitMenuItem.addActionListener(this);
    
    fileMenu.add(newhostMenuItem);    fileMenu.add(loadhostsMenuItem);    fileMenu.add(savehostsMenuItem);    fileMenu.addSeparator();
    fileMenu.add(exitMenuItem);
    
    loadhostsMenuItem.enable(false); // FIX-ME: remove when feature is added
    savehostsMenuItem.enable(false); // FIX-ME: remove when feature is added
        
    return fileMenu;  }


  // Window Listener Events:
  public void windowClosing(WindowEvent evt) {
    if(soloViewer.isShowing()) {
      soloHostClose();
    }

    if(evt.getComponent() == this) {
      quit();
    }

  }

  public void windowActivated(WindowEvent evt) {}
  public void windowDeactivated (WindowEvent evt) {}
  public void windowOpened(WindowEvent evt) {}
  public void windowClosed(WindowEvent evt) {}
  public void windowIconified(WindowEvent evt) {}
  public void windowDeiconified(WindowEvent evt) {}


  // Component Listener Events:
  public void componentResized(ComponentEvent evt) {
    if(evt.getComponent() == this) {
      if(thumbnailRowCount > 0) {
        resizeThumbnails();
      }
    }
    else { // resize soloViewer
      VncViewer v = (VncViewer)soloViewer.getComponent(0);
      updateCanvasScaling(v, soloViewer.getWidth(), soloViewer.getHeight());
    }

  }

  public void componentHidden(ComponentEvent  evt) {}  public void componentMoved(ComponentEvent evt) {}
  public void componentShown(ComponentEvent evt) {}


  // Mouse Listener Events:
  public void mouseClicked(MouseEvent evt) {
    if(evt.getClickCount() == 2) {
      Component c = evt.getComponent();
      if(c instanceof VncCanvas) {
        soloHost( ((VncCanvas)c).viewer );
      }
    }
    
  }
  public void mouseEntered(MouseEvent evt) {}  public void mouseExited(MouseEvent evt) {}  public void mousePressed(MouseEvent evt) {}  public void mouseReleased(MouseEvent evt) {}


  // Container Listener Events:
  public void componentAdded(ContainerEvent evt) {
    // This detects when a vncviewer adds a vnccanvas to it's container
    if(evt.getChild() instanceof VncCanvas) {
      VncViewer v = (VncViewer)evt.getContainer();
      v.vc.addMouseListener(this);
      v.buttonPanel.addContainerListener(this);
      v.buttonPanel.disconnectButton.addActionListener(this);
      updateCanvasScaling(v, widthPerThumbnail, heightPerThumbnail);
    }

    // This detects when a vncviewer's Disconnect button had been pushed
    else if(evt.getChild() instanceof Button) {
      Button b = (Button)evt.getChild();
      if(b.getLabel() == "Hide desktop") {
        b.addActionListener(this);
      }
    }

  }
  
  public void componentRemoved(ContainerEvent evt) {}
  
  
  // Action Listener Event:
  public void actionPerformed(ActionEvent evt) {
    if( evt.getSource() instanceof Button && ((Button)evt.getSource()).getLabel() == "Hide desktop") {
      VncViewer v = (VncViewer)((Component)((Component)evt.getSource()).getParent()).getParent();
      this.remove(v);
      viewersList.remove(v);
    }
    if(evt.getSource() == newhostMenuItem) {
      hostDialog = new AddHostDialog(this);
    }
    if(evt.getSource() == savehostsMenuItem) {
    }
    if(evt.getSource() == loadhostsMenuItem) {
    }
    if(evt.getSource() == exitMenuItem) {
      quit();
    }

  }
  
}