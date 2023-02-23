import CodeGen.PCodeExecutor;
import Components.Token;
import Errors.ErrorLog;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Compiler {
    private static ArrayList<String> stringBuf = new ArrayList<>();

    public static void readToBuffer(String pathname) throws IOException {
        File file = new File(pathname);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String str;
        while ((str = br.readLine()) != null) {
            str += "\n";
            stringBuf.add(str);
        }
    }

    public static void writeToLog(String pathname) throws IOException {

    }

    public static void main(String[] args) throws Exception {
        // 1-readIn
        String inputpath = "testfile.txt";
        String outputpath = "pcoderesult.txt";

//        String inputpath = "D:\\testBox\\C\\testfile1.txt";
//        String inputpath = args[0];
//        String inputpath = "D:\\G3S1资料\\编译原理\\实验\\testfile.txt";
//        String outputpath = "error.txt";
//        String outputpath = "D:\\G3S1资料\\编译原理\\实验\\my_ans.txt";
//        String outputpath = args[1];
        readToBuffer(inputpath);
        // 2-lexer
        ArrayList<Token> tokens = new Lexer(stringBuf).lexing();
//        for (int i = 0; i < tokens.size(); i++) {
//            if (tokens.get(i).getType().equals(TokenType.IDENFR))
//            System.out.println(tokens.get(i));
//        }
         //3-writeOut
        Parser parser = new Parser(tokens);
//        BufferedWriter writer = new BufferedWriter(new FileWriter(outputpath));
//        writer.write((parser.parse()).toString());
//        writer.close();
//        System.out.println(parser.parse());
//        ErFunc.writeErrorLog();
        parser.parse();
        //parser.printPCode("D:\\G3S1资料\\编译原理\\实验\\pcodeMY.txt");
        PCodeExecutor pCodeExecutor = new PCodeExecutor(parser.getCodes());
        pCodeExecutor.run();
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputpath));
        for (String s : pCodeExecutor.getprintList()) {
            writer.write(s);
        }
        writer.close();
        // error--------------------------------------------------
//        ErrorLog.printErrorLog(new FileWriter("error.txt"));
        //pCodeExecutor.print();

//        HashMap<Integer,String> map = new HashMap<>();
//        for (int i=0;i<10;i++) {
//            map.put (i*100, Integer.toString (i));
//        }
//        for (Integer key : map.keySet ()) {
//            System.out.println (key);
//        }
//        System.out.println("---------------------");
//        for (String s : map.values()) {
//            System.out.println(s);
//        }
    }



}
