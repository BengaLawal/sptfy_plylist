package spotify.main;

import fileProcessor.FileProcessor;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No command-line arguments provided.");
            return;
        }

        String filePath = args[0];
        FileProcessor data = new FileProcessor(filePath);
        List<List<String>> data_ = data.getData();
        System.out.print(data_);
    }
}