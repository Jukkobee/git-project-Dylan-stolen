import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Git {

    public static void initGitRepo() throws IOException {
        File gitDir = new File("git");
        File objectsDir = new File("./git/objects");
        File indexFile = new File("./git/index");

        if (gitDir.exists() && objectsDir.exists() && indexFile.exists()) {
            System.out.println("Git Repository already exists");
            return;
        }

        if (!gitDir.exists()) {
            gitDir.mkdir();
        }

        if (!objectsDir.exists()) {
            objectsDir.mkdir();
        }

        if (!indexFile.exists()) {
            indexFile.createNewFile();
        }

    }


    public static String sha1(Path file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] byteArr = md.digest(Files.readAllBytes(file));
        BigInteger n = new BigInteger(1, byteArr);
        String hash = n.toString(16);
        while (hash.length() < 40) {
            hash = "0" + hash;
        }

        return hash;
    }

    public static void createNewBlob(Path path) throws IOException, NoSuchAlgorithmException {

        File file = path.toFile();
        if (file.isDirectory()) {
            File temp = File.createTempFile("miniIndex", null);
            BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
            for (File subfile: file.listFiles()) {
                if (!subfile.equals(file) && subfile.exists()) {
                    createNewBlob(subfile.toPath());
                    writer.write(blobOrTree(subfile) + " " + sha1(subfile.toPath()) + " " + relPath(subfile));
                }
            }
            writer.close();
            file = temp;
        }

        String sha1num = sha1(file.toPath());
        Path hash = Paths.get("./git/objects/" + sha1num);

        if (Files.exists(hash)) {
            Files.delete(hash);
        }

        Files.copy(file.toPath(), hash);

        BufferedReader br = new BufferedReader(new FileReader(("./git/index")));
        String input = blobOrTree(path.toFile()) + " " + sha1num + " " + relPath(path.toFile());
        while (br.ready()) {
            if (br.readLine().equals(input)) {
                return;
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./git/index", true))) {
            writer.write(input);
            writer.newLine();
            if (isTree(path.toFile())) {
                createTree(path.toFile());
            }
            writer.close();
        }
    }

    public static void createNewBlob(String filename) throws NoSuchAlgorithmException, IOException {
        createNewBlob(Paths.get(filename));
    }

    public static void resetGit () {
        File fodder = new File ("git/");
        if (fodder.exists()) {
            deleteDir(fodder);
            fodder.delete();
        }
    }

    //deletes directories recursively (gets rid of the subfiles too)
    public static void deleteDir(File dir) {
        if (!dir.isDirectory()) {
            if (dir.isFile()) 
                dir.delete();
            else 
                throw new IllegalArgumentException();
        }
        if (dir.exists()) {
            for (File subfile:dir.listFiles()) {
                deleteDir(subfile);
            }
            dir.delete(); 
        }
    }

    public static String blobOrTree(File fileName) {
        if (fileName.isDirectory())
            return "tree";
        return "blob";
    }

    public static boolean isTree(File file) {
        return file.isDirectory();
    }

    public static String relPath(File fileName) {
        return fileName.getPath();
    }

    public static void createTree(File fileName) throws NoSuchAlgorithmException, IOException {
        if (!fileName.exists()) {
            return;
        }
        for (File subfile: fileName.listFiles()) {
            if (subfile.isDirectory()) {
                createTree(subfile);
            }
            createNewBlob(subfile.toPath());
        }
    }
}
