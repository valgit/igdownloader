import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextField;

public class InstagramDownloaderUI
  extends JFrame
  implements ActionListener
{
  JTextField htField;
  JButton toggle;
  JButton choose;
  JFileChooser fileChooser;
  JLabel chosenOutputLabel;
  JCheckBox debugChkBox;
  Boolean running = false;
  String RUN_TEXT = "Run";
  String STOP_TEXT = "Stop";
  String DEFAULT_TAG = "mvsstudio";
  File outputDir;
  RequestMgr reqMgr;
    private String APIurl;
  
  public InstagramDownloaderUI() {
    initComponents();
    checkForConfigAndMaybeStart();
    setDefaultCloseOperation(3);
    setTitle("Instaprint - MVS");
  } 
  
  private void initComponents()
  {
    setLayout(null);
    
    JMenuBar menuBar = new JMenuBar();
    JMenu menu = new JMenu("File");
    JMenuItem menuItem = new JMenuItem("Save", KeyEvent.VK_T);
    menuItem.addActionListener(this);
    menu.add(menuItem);
    menuBar.add(menu);
    this.setJMenuBar(menuBar);
    
    JLabel label = new JLabel();
    label.setText("Hashtag:");
    
    JLabel outputLabel = new JLabel();
    outputLabel.setText("Output:");
    
    this.chosenOutputLabel = new JLabel();
    this.chosenOutputLabel.setText("");
    
    this.htField = new JTextField();
    this.htField.setText(this.DEFAULT_TAG);
    
    this.toggle = new JButton();
    this.toggle.setActionCommand("toggle");
    this.toggle.setText(this.RUN_TEXT);
    this.toggle.addActionListener(this);
    
    this.choose = new JButton();
    this.choose.setActionCommand("chooseDir");
    this.choose.setText("...");
    this.choose.addActionListener(this);
    
    this.fileChooser = new JFileChooser();
    this.fileChooser.setFileSelectionMode(1);
    JLabel debugLabel = new JLabel();
    debugLabel.setText("Debug:");
    debugLabel.setBounds(20, 250, 60, 20);
    add(debugLabel);
    this.debugChkBox = new JCheckBox();
    this.debugChkBox.setBounds(80, 250, 20, 20);
    add(this.debugChkBox);
    
    label.setBounds(20, 10, 70, 20);
    add(label);
    
    this.htField.setBounds(100, 10, 230, 20);
    add(this.htField);
    
    outputLabel.setBounds(20, 40, 80, 20);
    add(outputLabel);
    
    this.chosenOutputLabel.setBounds(110, 40, 150, 20);
    add(this.chosenOutputLabel);
    
    this.choose.setBounds(300, 40, 30, 20);
    add(this.choose);
    
    this.toggle.setBounds(125, 70, 100, 20);
    this.toggle.setEnabled(false);
    add(this.toggle);
    
    setSize(350, 150);
  } 
  
  public void toggleRunningState() {
    this.running = !this.running;
    this.toggle.setText(this.running ? this.STOP_TEXT : this.RUN_TEXT);
  } 
  
  public void actionPerformed(ActionEvent e) {
    System.out.println(e.getActionCommand());
    if ("toggle".equals(e.getActionCommand())) {
      startRequestMgr();
    } else if ("chooseDir".equals(e.getActionCommand())) {
      handleStartOutputDirChoice();
    } else if ("Save".equals(e.getActionCommand())) {
      saveConfig();
    }
  } 
  
  private void startRequestMgr() {
    if (this.htField.getText().equals("")) {
      this.htField.setText(this.DEFAULT_TAG);
    } 
    if (this.running) {
      System.out.println("Was running. Trying to cancel");
      if (this.reqMgr != null) {
        this.reqMgr.end();
      } else {
        System.out.println("req mgr is null");
      } 
    } else {
      this.reqMgr = new RequestMgr(this.htField.getText(), this.outputDir, getPrevDownloaded(this.outputDir),APIurl);
      //this.reqMgr.setDebug(Boolean.valueOf(this.debugChkBox.isSelected()));
	  this.reqMgr.setDebug(true);
      System.out.println("Was not running. Trying to start");
      this.reqMgr.start();
    } 
    toggleRunningState();
  } 
  
  private ArrayList<String> getPrevDownloaded(File outputDir) {
    File[] files = outputDir.listFiles(new JpegFilter());
    ArrayList results = new ArrayList();
    for (int i = 0; i < files.length; i++) {
      results.add(files[i].getName());
    } 
    return results;
  } 
  
  private void handleStartOutputDirChoice() {
    int result = this.fileChooser.showOpenDialog(this);
    if (result == 0) {
      this.toggle.setEnabled(true);
      this.outputDir = this.fileChooser.getSelectedFile();
      this.chosenOutputLabel.setText(this.outputDir.getPath());
    } 
  } 
  
  private void saveConfig() {
	  PrintWriter writer;
	try {
		writer = new PrintWriter(System.getProperty("user.home") + "/igdownloader.config", "UTF-8");
		writer.println(this.htField.getText());
		writer.println(this.outputDir.getAbsolutePath());
		writer.close();
	} catch (FileNotFoundException | UnsupportedEncodingException e) {
		e.printStackTrace();
	}
  }
  
  private void checkForConfigAndMaybeStart() {
	  Path p = Paths.get(System.getProperty("user.home") + "/igdownloader.config");
	  String config = null;
	  try {
	    	config = new String(Files.readAllBytes(p));
	  } catch (IOException e) {
		    return;
	  }
	  
      String[] configParts = config.split(System.lineSeparator());
		  
	  //if (configParts.length == 3)
       {
		  String hashtag = configParts[0];
		  String output = configParts[1];
		  APIurl = configParts[2]; // API key

		  this.htField.setText(hashtag);
          htField.setText(APIurl);
		  this.chosenOutputLabel.setText(output);
		  this.outputDir = new File(output);
		  this.toggle.setEnabled(true);
		  this.startRequestMgr();
	  } 
	  
  }
} 
