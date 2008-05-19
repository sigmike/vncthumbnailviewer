//
//  Copyright (C) 2007-2008 David Czechowski  All Rights Reserved.
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

import java.awt.*;
import java.io.*;
import net.n3.nanoxml.*; // Source available at http://nanoxml.cyberelf.be/
import java.util.*;
import java.net.*;//TEMP

//
// This Vector-based List is used to maintain of a list of VncViewers
//
// This also contains the ability to load/save it's list of VncViewers to/from
//    an external xml file.
//

class VncViewersList extends Vector {

  //
  // Constructor.
  //
  public VncViewersList(VncThumbnailViewer v)
  {
    super();
    
    tnViewer = v;
  }

  private VncThumbnailViewer tnViewer;

  public static boolean isHostsFileEncrypted(String filename) {
    boolean encrypted = false;
    
    try {
      File file = new File(filename);
      URL url = file.toURL();
      filename = url.getPath();
            
      IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
      IXMLReader reader = StdXMLReader.fileReader(filename);
      parser.setReader(reader);
      IXMLElement root = (IXMLElement) parser.parse();

      if(root.getFullName().equalsIgnoreCase("Manifest") ) {
        String e = root.getAttribute("Encrypted", "0");
        if(Integer.parseInt(e) == 1) {
          encrypted = true;
        }
      }

    } catch (Exception e) {
      System.out.println("Error testing file for encryption.");
      System.out.println(e.getMessage());
    }
    
    return encrypted;
    // this returns false even if there is a problem reading the file
  }


  public void loadHosts(String filename, String encPassword) {
    try {
      File file = new File(filename);
      URL url = file.toURL();
      filename = url.getPath();

      IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
      IXMLReader reader = StdXMLReader.fileReader(filename);
      parser.setReader(reader);
      IXMLElement root = (IXMLElement) parser.parse();

      if(root.getFullName().equalsIgnoreCase("Manifest") ) {
        boolean encrypted = (1 == Integer.parseInt( root.getAttribute("Encrypted", "0") ) );
        float version = Float.parseFloat( root.getAttribute("Version", "0.9") );
        System.out.println("Loading file...  file format version " + version + " encrypted(" + encrypted + ")");
        
        if(encrypted && encPassword == null) {
          // FIX-ME: do something
          System.out.println("ERROR: Password needed to properly read file.");
        }
        
        Enumeration enm = root.enumerateChildren();
        while (enm.hasMoreElements()) {
          IXMLElement e = (IXMLElement)enm.nextElement();
          
          if(e.getFullName().equalsIgnoreCase("Connection")) {
            boolean success = parseConnection(e, encrypted, encPassword);
          }
          else {
            System.out.println("Load: Ignoring " + e.getFullName());
          }
        }
        
      } else {
        System.out.println("Malformed file, missing manifest tag.");
        System.out.println("Found " + root.getFullName());
      }

    } catch (Exception e) {
      System.out.println("Error loading file.\n" + e.getMessage() );
    }
  }


  private boolean parseConnection(IXMLElement e, boolean isEncrypted, String encPass) {
    String host = e.getAttribute("Host", null);
    String prt = e.getAttribute("Port", null);

    boolean success = true;
    
    if(prt == null || host == null) {
      System.out.println("Missing Host or Port attribute");
      success = false;
    } else {
      int port = Integer.parseInt(prt);

      int secType = Integer.parseInt(e.getAttribute("SecType", "1"));
      String password = e.getAttribute("Password", null);
      String username = e.getAttribute("Username", null);
      String userdomain = e.getAttribute("UserDomain", null);
      String compname = e.getAttribute("CompName", null);
      String comment = e.getAttribute("Comment", null);
            
      if(isEncrypted) {
        if(password != null)
          password = DesCipher.decryptData(password, encPass);
        if(username != null)
          username = DesCipher.decryptData(username, encPass);
        if(userdomain != null)
          userdomain = DesCipher.decryptData(userdomain, encPass);
        if(compname != null)
          compname = DesCipher.decryptData(compname, encPass);
        if(comment != null)
          comment = DesCipher.decryptData(comment, encPass);
      }
      
      // Error Checking:
      switch(secType) {
        case 1: // none
          if(password != null || username != null) {
            System.out.println("WARNING: Password or Username specified for NoAuth");
          }
        case 2: // vnc auth
          if(password == null) {
            System.out.println("ERROR: Password missing for VncAuth");
            success = false;
          }
          if(username != null) {
            System.out.println("WARNING: Username specified for VncAuth");
          }
          break;
        case -6: // ms-logon
          if(password == null || username != null) {
            System.out.println("ERROR: Password or Username missing for MsAuth");
            success = false;
          }
          break;
        case 5: // ra2
        case 6: // ra2ne
        case 16: // tight
        case 17: // ultra
        case 18: // tls
        case 19: // vencrypt
          System.out.println("ERROR: Incomplete security type (" + secType + ") for Host: " + host + " Port: " + port);
        case 0: // invalid
        default:
          // Error
          success = false;
          break;
      }

      // Launch the Viewer:
      System.out.println("LOAD Host: " + host + " Port: " + port + " SecType: " + secType);
      if(success) {
        if(getViewer(host, port) == null) {
          VncViewer v = launchViewer(host, port, password, username, userdomain);
          //v.setCompName(compname);
          //v.setComment(comment);
        }
        // else - the host is already open
      }
    }

    return success;
  }


  public void saveToEncryptedFile(String filename, String encPassword) {
    if(encPassword == null || encPassword.length() == 0) {
      System.out.println("WARNING: Saving to encrypted file with empty passkey");
    }
    saveHosts(true, filename, encPassword);
  }
  
  
  public void saveToFile(String filename) {
    saveHosts(false, filename, null);
  }


  private void saveHosts(boolean isEncrypted, String filename, String encPassword) {

    IXMLElement manifest = new XMLElement("Manifest");
    manifest.setAttribute("Encrypted", (isEncrypted? "1" : "0") );
    manifest.setAttribute("Version", Float.toString(VncThumbnailViewer.VERSION));
    
    ListIterator l = listIterator();
    while(l.hasNext()) {
      VncViewer v = (VncViewer)l.next();
      String host = v.host;
      String port = Integer.toString(v.port);
      String password = v.passwordParam;
      String username = v.usernameParam;
      //String userdomain = 
      //String compname =
      //String comment =
      String sectype = "1";
      if(password != null && password.length() != 0) {
        sectype = "2";
        if(username != null && username.length() != 0) {
          sectype = "-6";
        }
      }
      
      if(isEncrypted) {
        if(sectype != "1")
          password = DesCipher.encryptData(password,encPassword);
        if(sectype == "-6") {
          username = DesCipher.encryptData(username,encPassword);
          //userdomain = encryptData(userdomain,encPassword);
        }
        //compname = encryptData(compname,encPassword);
        //comment = encryptData(comment,encPassword);
      }

      IXMLElement c = manifest.createElement("Connection");
      manifest.addChild(c);
      c.setAttribute("Host", host);
      c.setAttribute("Port", port);
      c.setAttribute("SecType", sectype);
      if(sectype == "2" || sectype == "-6") {
        c.setAttribute("Password", password);
        if(sectype == "-6") {
          c.setAttribute("Username", username);
          //c.setAttribute("UserDomain", userdomain);
        }
      }
      //c.setAttribute("CompName", "");
      //c.setAttribute("Comment", "");
    }
    
    try {
      PrintWriter o = new PrintWriter( new FileOutputStream(filename) );
      XMLWriter writer = new XMLWriter(o);
      o.println("<?xml version=\"1.0\" standalone=\"yes\"?>");
      writer.write(manifest, true);
      
    } catch (IOException e) {
      System.out.print("Error saving file.\n" + e.getMessage() );
    }
    
  }


  public VncViewer launchViewer(String host, int port, String password, String user, String userdomain) {
    VncViewer v = launchViewer(tnViewer, host, port, password, user, userdomain);
    add(v);
    tnViewer.addViewer(v);
    
    return v;
  }

  public static VncViewer launchViewer(VncThumbnailViewer tnviewer, String host, int port, String password, String user, String userdomain) {
    String args[] = new String[4];
    args[0] = "host";
    args[1] = host;
    args[2] = "port";
    args[3] = Integer.toString(port);

    if(password != null && password.length() != 0) {
      int newlen = args.length + 2;
      String[] newargs = new String[newlen];
      System.arraycopy(args, 0, newargs, 0, newlen-2);
      newargs[newlen-2] = "password";
      newargs[newlen-1] = password;
      args = newargs;
    }

    if(user != null && user.length() != 0) {
      int newlen = args.length + 2;
      String[] newargs = new String[newlen];
      System.arraycopy(args, 0, newargs, 0, newlen-2);
      newargs[newlen-2] = "username";
      newargs[newlen-1] = user;
      args = newargs;
    }

    if(userdomain != null && userdomain.length() != 0) {
      int newlen = args.length + 2;
      String[] newargs = new String[newlen];
      System.arraycopy(args, 0, newargs, 0, newlen-2);
      newargs[newlen-2] = "userdomain";
      newargs[newlen-1] = userdomain;
      args = newargs;
    }

    // launch a new viewer
    System.out.println("Launch Host: " + host + ":" + port);
    VncViewer v = new VncViewer();
    v.mainArgs = args;
    v.inAnApplet = false;
    v.inSeparateFrame = false;
    v.showControls = true;
    v.showOfflineDesktop = true;
    v.vncFrame = tnviewer;
    v.init();
    v.options.viewOnly = true;
    v.options.autoScale = true; // false, because ThumbnailViewer maintains the scaling
    v.options.scalingFactor = 10;
    v.addContainerListener(tnviewer);
    v.start();
    
    return v;
  }


  public VncViewer getViewer(String hostname, int port) {
    VncViewer v = null;

    ListIterator l = listIterator();
    while(l.hasNext()) {
      v = (VncViewer)l.next();
      if(v.host == hostname && v.port == port) {
        return v;
      }
    }

    return null;
  }

  public VncViewer getViewer(Container c) {
    VncViewer v = null;

    ListIterator l = listIterator();
    while(l.hasNext()) {
      v = (VncViewer)l.next();
      if(c.isAncestorOf(v)) {
        return v;
      }
    }

    return null;
  }

  public VncViewer getViewer(Button b) {
    VncViewer v;

    ListIterator l = listIterator();
    while(l.hasNext()) {
      v = (VncViewer)l.next();
      if(v.getParent().isAncestorOf(b)) {
        return v;
      }
    }

    return null;
  }

}
