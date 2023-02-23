package Errors;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

public class ErrorLog {
    private static ArrayList<MyException> errors = new ArrayList<>();

    public static void addError(MyException e) {
        errors.add(e);
    }

    public static int getSize() {
        return errors.size();
    }

    public static void errorPopBack(int oldSize) {
        while (errors.size() != oldSize) {
            errors.remove(errors.size() - 1);
        }
    }

    public static ArrayList<MyException> getErrorLog(){
        return errors;
    }

    public static void printErrorLog(FileWriter writer) throws IOException {
        for (MyException error : errors) {
            writer.write(error + "\n");
        }
        writer.flush();
        writer.close();
    }
}
