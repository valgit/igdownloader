import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Logger
{
  FileWriter log;
  BufferedWriter out;
  Boolean enabled = Boolean.valueOf(false);
  
  public Logger(String name) {
    try {
      this.log = new FileWriter("./" + name + ".txt");
    } catch (IOException e) {
      e.printStackTrace();
    } 
    this.out = new BufferedWriter(this.log);
  } 
  
  public void log(String str) {
    if (!this.enabled.booleanValue()) {
      return;
    } 
    try {
      this.out.write(str);
      this.out.newLine();
      this.out.flush();
    } catch (IOException e) {
      e.printStackTrace();
    } 
  } 
  
  public void close() {
    try {
      this.out.close();
    } catch (IOException e) {
      e.printStackTrace();
    } 
  } 
  
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  } 
} 
