import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class Git {

    public static void initGitRepo() throws IOException {
        File gitDir = new File("git");
        File objectsDir = new File("./git/objects");
        File indexFile = new File("./git/index");
        File head = new File("./git/HEAD");

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
        head.createNewFile();

    }

    public String commit(String summary, String author) throws NoSuchAlgorithmException, IOException
    //creates a commit given the author and a summary. and all the changes since the most recent commit
    //how to find the date is found here: https://www.geeksforgeeks.org/java-current-date-time/
    {   
        if(getFileText("./git/index").length() == 0)
        {
            return "Nothing to commit.";
        }
        BufferedReader bReader = new BufferedReader(new FileReader("./git/HEAD"));
        String head = bReader.readLine();
        bReader.close();
        Date d = new Date();

        String treeText = getTreeText(head);
        String treeHash = sha1FromText(treeText);
        switchIndex(treeText, treeHash);
        
        String commitText = "tree: " + treeHash; 
        commitText += "\n" + "parent: " + head;
        commitText += "\n" + "author: " + author;
        commitText += "\n" + "date: " + d;
        commitText += "\n" + "message: " + summary;

        String commitHash = sha1FromText(commitText); // this creates the hash
        switchHead(commitHash); // this updates the HEAD
        String commitPathName = "./git/objects/" + commitHash; //this part actually creates the commit file in the objects folder
        File newCommit = new File(commitPathName);
        newCommit.createNewFile();
        BufferedWriter bWriter = new BufferedWriter(new FileWriter(commitPathName));
        bWriter.write(commitText);
        bWriter.close();

        return commitHash;
    }

    public static String getTreeText(String head) throws IOException
    {
        String indexText = getFileText("./git/index"); //takes the new changes since the old version
        String previousCommit = getFileText("./git/objects/" + head); //these three lines get the old version of the index
        String previousIndexHash = previousCommit.substring(previousCommit.indexOf("tree: ") + 6);
        String previousIndexText = getFileText("./git/objects/" + previousIndexHash);
        indexText = previousIndexText + "\n" + indexText; //combines the old version with the new changes
        return indexText;
    }

    public static void switchIndex(String treeText, String treeHash) throws IOException
    {
        File index = new File("./git/index");
        index.delete();
        File indexInObjects = new File("./git/objects/" + treeHash);
        indexInObjects.createNewFile();
        BufferedWriter bWriter = new BufferedWriter(new FileWriter(indexInObjects);
        bWriter.write(treeText);
        bWriter.close();
    }

    public static String getFileText(String pathName) throws IOException
    {
        BufferedReader bReader = new BufferedReader(new FileReader(pathName));
        String fileText = "";
        while(bReader.ready())
        {
            fileText += bReader.readLine();
        }
        bReader.close();
        return fileText;
    }

    public static String sha1(Path file) throws NoSuchAlgorithmException, IOException {
        //gets SHA1 given pathName
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] byteArr = md.digest(Files.readAllBytes(file));
        BigInteger n = new BigInteger(1, byteArr);
        String hash = n.toString(16);
        while (hash.length() < 40) {
            hash = "0" + hash;
        }
        return hash;
    }

    public static String sha1FromText(String textToSHA1) throws NoSuchAlgorithmException
    //gets SHA1 given a string of text
    {
        byte[] bytes = textToSHA1.getBytes();
        MessageDigest  md = MessageDigest.getInstance("SHA-1");
        byte[] byteArr = md.digest(bytes);
        BigInteger n = new BigInteger(1, byteArr);
        String hash = n.toString(16);
        while (hash.length() < 40) {
            hash = "0" + hash;
        }
        return hash;
    }

    public static void switchHead(String commitHash) throws IOException
    {
        File head = new File("./git/HEAD");
        head.delete();
        head.createNewFile();
        BufferedWriter bWriter = new BufferedWriter(new FileWriter("./git/HEAD"));
        bWriter.write(commitHash);
        bWriter.close();
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
        for (File subfile : fileName.listFiles()) {
            if (subfile.isDirectory()) {
                createTree(subfile);
            }
            createNewBlob(subfile.toPath());
        }
    }
}