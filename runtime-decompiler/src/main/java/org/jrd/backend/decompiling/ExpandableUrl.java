package org.jrd.backend.decompiling;

import org.jrd.backend.core.OutputController;
import org.jrd.backend.data.Directories;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

public class ExpandableUrl {

    public static class MalformedURLToPath extends RuntimeException{
        public MalformedURLToPath(Throwable cause) {
            super(cause);
        }
    }
    public static class MalformedMacroExpansion extends RuntimeException{
        public MalformedMacroExpansion(Throwable cause) {
            super(cause);
        }
    }

    private String path;

    private ExpandableUrl(String s) {
        String path = expandEnvVars(s);
        if (!new File(path).exists()){
            if(s.isEmpty()){
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new FileNotFoundException("Filename empty."));
            } else {
                OutputController.getLogger().log(OutputController.Level.MESSAGE_ALL, new FileNotFoundException(path));
            }
        }
        this.path = s;
    }

    public static ExpandableUrl createFromPath(String path){
        return new ExpandableUrl(collapseEnvVars(path));
    }

    public static ExpandableUrl createFromStringUrl(String url) throws MalformedMacroExpansion{
        try {
            return createFromPath(new URL(expandEnvVars(url)).getPath());
        } catch (MalformedURLException e) {
            throw new MalformedMacroExpansion(e);
        }
    }

    static String expandEnvVars(String path){
        String pluginDir = unifySlashes(Directories.getXdgJrdBaseDir());
        String homeDir = unifySlashes(System.getProperty("user.home"));

        path = path.replace("${XDG_CONFIG_HOME}", pluginDir);
        path = path.replace("${HOME}", homeDir);

        return path;
    }

    private static String collapseEnvVars(String path){
        String pluginDir = unifySlashes(Directories.getXdgJrdBaseDir());
        String homeDir = unifySlashes(System.getProperty("user.home"));

        return collapseEnvVars(unifySlashes(path), homeDir, pluginDir);
    }

    static String collapseEnvVars(String path, String home, String xdgConfigHome){
        path = path.replace(xdgConfigHome, "${XDG_CONFIG_HOME}");
        path = path.replace(home, "${HOME}");

        return path;
    }

    public static String unifySlashes(String dir){
        dir = dir.replaceAll("\\\\", "/");
        if(isOsWindows() && !dir.startsWith("file") && dir.length() > 0 && dir.charAt(0) != '/' && dir.charAt(0) != '$' ){
            dir = "/" + dir;
        }

        return dir;
    }

    public static boolean isOsWindows(){
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    public URL getExpandedURL() throws MalformedURLException {
        return new URL("file", "", expandEnvVars(this.path));
    }

    public String getRawURL(){
        try {
            return collapseEnvVars(new URL("file", "", expandEnvVars(this.path)).toString());
        } catch (MalformedURLException e) {
            throw new MalformedMacroExpansion(e);
        }
    }

    public String getExpandedPath(){ // /C:/Users/user/path
        try {
            return getExpandedURL().getPath();
        } catch (MalformedURLException e) {
            throw new MalformedURLToPath(e);
        }
    }

    public String getRawPath(){ //${HOME}/path
        return collapseEnvVars(path);
    }

    public File getFile(){
        return new File(getExpandedPath());
    }

    @Override
    public String toString() {
        return path;
    }
}
