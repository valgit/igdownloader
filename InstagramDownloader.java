import java.io.File;
import javax.swing.SwingUtilities;

public class InstagramDownloader
{
  public static void main(String[] args)
  {
    InstagramDownloader ip = new InstagramDownloader();
    ip.createRunFolder();
    SwingUtilities.invokeLater(new InstagramDownloaderRunnable());
  } 
  
  private void createRunFolder()
  {
    Boolean exists = Boolean.valueOf(new File("./instaprint").exists());
    if (!exists.booleanValue()) {
      new File("./instaprint").mkdir();
    } 
  } 
} 
