package Errors;

public class MyException {
    private int lineNo;
    private char exceptionType;

    public MyException(int lineNo, char exceptionType) {
        this.lineNo = lineNo;
        this.exceptionType = exceptionType;
    }

    public String toString() {
        return String.valueOf(lineNo) + " " + exceptionType;
    }
}

