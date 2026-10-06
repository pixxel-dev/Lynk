import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

public class GenerateKey {
    public static void main(String[] args) throws Exception {
        System.out.println("Generating Keystore...");
        
        File appDir = new File("../app");
        if (!appDir.exists()) {
            appDir = new File("app"); // Fallback if run from project root
        }
        
        String keystorePath = new File(appDir, "lynk_release.keystore").getAbsolutePath();

        String[] cmd = {
            "keytool", "-genkeypair", "-v",
            "-keystore", keystorePath,
            "-alias", "lynk",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-storepass", "lynk123",
            "-keypass", "lynk123",
            "-dname", "CN=Lynk, OU=Lynk, O=Lynk, L=City, ST=State, C=US"
        };
        
        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println(line);
        }
        
        BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while ((line = err.readLine()) != null) {
            System.err.println(line);
        }
        
        int exitCode = p.exitValue();
        System.out.println("Exit code: " + exitCode);
        
        if (exitCode == 0) {
            System.out.println("Keystore successfully created at: " + keystorePath);
        } else {
            System.err.println("Failed to create keystore.");
        }
    }
}
