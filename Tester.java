import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
public class Tester {
    public static void initGitRepoTester() throws IOException {

        File gitDir = new File("./git");
        File objectsDir = new File("./git/objects");
        File indexFile = new File("./git/index");

        Git.initGitRepo();

        boolean repoCreated = gitDir.exists() && objectsDir.exists() && indexFile.exists();

        // delete files in directories first
        indexFile.delete();
        objectsDir.delete();
        gitDir.delete();

        boolean checkDeletedFiles = !(gitDir.exists() && objectsDir.exists() && indexFile.exists());

        if (repoCreated && checkDeletedFiles) {
            System.out.println("Created repo and deleted files.");
        } else {
            if (repoCreated) {
                System.out.println("Created repo.");
            } else {
                System.out.println("Did not create repo.");
            }

            if (checkDeletedFiles) {
                System.out.println("Deleted files.");
            } else {
                System.out.println("Did not delete files.");
            }
        }

    }
    public static void createNewBlobTester(Path path) throws NoSuchAlgorithmException, IOException {
        Git.createNewBlob(path);

        String hash = Git.sha1(path);
        Path blobPath = Paths.get("./git/objects/" + hash);
        if (!Files.exists(blobPath)) {
            System.out.println("blob doesn't exist: " + blobPath);
        } else {
            System.out.println("blob exists: " + blobPath);
        }

        String hash1 = Git.sha1(path);
        String hash2 = Git.sha1(blobPath);

        if (hash1.equals(hash2)) {
            System.out.println("the content of file is the same");
        }

        try (BufferedReader br = new BufferedReader(new FileReader("./git/index"))) {
            String line;
            boolean indexLineExists = false;
            while ((line = br.readLine()) != null) {
                if (line.equals(path.getFileName() + " " + hash)) {
                    indexLineExists = true;
                    break;
                }
            }

            if (!indexLineExists) {
                System.out.println("no index entry: " + path.getFileName());
                return;
            }
        }

        Files.deleteIfExists(path);
        Files.deleteIfExists(blobPath);

        if (!Files.exists(path) && !Files.exists(blobPath)) {
            System.out.println("files were deleted");
        }

        System.out.println("everything was created (blob + index entry)");

    }
    public static void SHATester() throws IOException, NoSuchAlgorithmException {

        //sha1 tester
        File f = new File("./git/testFile.txt");
        if (!f.exists()) {
            Path testFile = Paths.get("./git/testFile.txt");
            testFile.toFile().createNewFile();
        }

        // it exists, so write to it
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(("testFile.txt")));
            writer.write("hello world");
            writer.close();
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }

        System.out.println("hash: " + Git.sha1(Paths.get("./git/testFile.txt")));
        System.out.println("expected hash: " + "2aae6c35c94fcfb415dbe95f408b9ce91ee846ed");
    }
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        Git.initGitRepo();
        //initGitRepoTester();

        // testing blob
        // Paths.get("./git/testFile.txt")
        //Git.createNewBlob("testFile.txt");
        //createNewBlobTester(testFile);
        //Git.resetGit();
        File testFolder = new File ("testTreeFolder");
        File sample1 = new File ("./testTreeFolder/sample1.txt");
        testFolder.mkdir();
        sample1.createNewFile();
        Git.createNewBlob("testTreeFolder");
    }
}
