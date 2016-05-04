import java.io.File;

public class JpegFilter implements java.io.FileFilter
{
  public boolean accept(File pathname) {
    return (pathname.getPath().toLowerCase().endsWith("jpeg")) || (pathname.getPath().toLowerCase().endsWith("jpg"));
  } 
} 
