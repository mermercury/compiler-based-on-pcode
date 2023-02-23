package CodeGen;

import java.util.HashMap;

public class RetInfo {
    private int PC; // 运行完本函数后 跳转至哪条指令执行
    private HashMap<String, Var> varTable; // 保存之前运行环境的变量表
    private int stackPtr; // 栈顶指针
    private int paraNum;
    private int callArgsNum;
    private int nowParasNum;

    public RetInfo(int PC, HashMap<String, Var> varTable, int stackPtr, int paraNum, int callArgsNum, int nowParasNum) {
        this.PC = PC;
        this.varTable = varTable;
        this.stackPtr = stackPtr;
        this.paraNum = paraNum;
        this.callArgsNum = callArgsNum;
        this.nowParasNum = nowParasNum;
    }

    public int getPC() {
        return PC;
    }

    public HashMap<String, Var> getVarTable() {
        return varTable;
    }

    public int getStackPtr() {
        return stackPtr;
    }

    public int getParaNum() {
        return paraNum;
    }

    public int getCallArgsNum() {
        return callArgsNum;
    }

    public int getNowParasNum() {
        return nowParasNum;
    }
}
