package CodeGen;

import Components.Function;
import Errors.ErrorLog;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class PCodeExecutor {
    private ArrayList<PCode> codes;
    private ArrayList<RetInfo> retInfos = new ArrayList<>();
    private ArrayList<Integer> stack = new ArrayList<>();
    private int pc = 0;
    private HashMap<String, Var> varTable = new HashMap<>();
    private HashMap<String, Var> globalVarTable = new HashMap<>();
    private HashMap<String, Function> funcTable = new HashMap<>();
    // LabelName : address in stack
    private HashMap<String, Integer> labelTable = new HashMap<>();
    private boolean inGlobal = true;
    private int mainAddress;
    private ArrayList<String> list = new ArrayList<>();
    private boolean oneDimArrInit = false; // 识别是否处于一维数组(变量/常量)赋初值动作
    private int oneDimArrleft = -1; // []中的数字(constExp已经计算的到的结果)
    private int oneDimArrCnt = 0; // 右边{ , ...} 中个数
    private String oneDimArr; // 一维数组名
    private boolean isPush; // 这条指令是不是PUSH

    public PCodeExecutor(ArrayList<PCode> codes) {
        this.codes = codes;
        initialize();
    }

    private void initialize() {
        int size = codes.size();
        for (int i = 0; i < size; i++) {
            PCode code = codes.get(i);
            if (code.getType().equals(MidType.MAIN)) {
                mainAddress = i;
            } else if (code.getType().equals(MidType.LABEL)) {
                labelTable.put((String) code.getValue1(), i);
            } else if (code.getType().equals(MidType.FUNC)) {
                funcTable.put((String) code.getValue1(), (Function) code.getValue2());
                Function func = (Function) code.getValue2();
                func.setIndex(i);
            }
        }
    }

    private void push(int i) {
        stack.add(i);
    }

    private int pop() {
        return stack.remove(stack.size() - 1);
    }

    private Var getVar(String ident) {
        if (varTable.containsKey(ident)) {
            return varTable.get(ident);
        } else {
            return globalVarTable.get(ident);
        }
    }

    public void run() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("pcode.txt"));
        for (PCode s : codes) {
            writer.write(s.getType() + " " + s.getValue1() + " " + s.getValue2() + "\n");
        }
        writer.close();
        Scanner scanner = new Scanner(System.in);
        ArrayList<Integer> rparasIndex = new ArrayList<>(); // 实参地址列表
        int callArgsNum = 0;
        int nowParasNum = 0;
        for (pc = 0; pc < codes.size(); pc++) {

            PCode code = codes.get(pc);
            MidType codeType = code.getType();
            if (isPush && !codeType.equals(MidType.PUSH)) {
                if(oneDimArrleft != oneDimArrCnt) {
                    System.out.println(oneDimArr + "存在一维数组赋初值的元素个数与该数组声明的元素个数不一致问题！");
                }
                oneDimArrInit = false;
                oneDimArrleft = -1;
                oneDimArrCnt = 0;
                oneDimArr = null;
                isPush = false;
            }
            if (codeType.equals(MidType.VAR)) {
                Var var = new Var(stack.size());
                varTable.put((String) code.getValue1(), var);
                if (inGlobal) {
                    globalVarTable.put((String) code.getValue1(), var);
                }
            } else if (codeType.equals(MidType.PUSH)) {
                push((Integer) code.getValue1());
                if (oneDimArrInit) {
                    oneDimArrCnt++;
                    isPush = true;
                }
            } else if (codeType.equals(MidType.POP)) {
                int value = pop();
                int address = pop();
                stack.set(address, value);
            } else if (codeType.equals(MidType.ADD)) {
                int x = pop();
                int y = pop();
                push(x + y);
            } else if (codeType.equals(MidType.SUB)) {
                int y = pop();
                int x = pop();
                push(x - y);
            } else if (codeType.equals(MidType.MUL)) {
                int x = pop();
                int y = pop();
                push(x * y);
            } else if (codeType.equals(MidType.DIV)) {
                int y = pop();
                int x = pop();
                push(x / y);
            } else if (codeType.equals(MidType.MOD)) {
                int y = pop();
                int x = pop();
                push(x % y);
            } else if (codeType.equals(MidType.BITAND)) {
                int y = pop();
                int x = pop();
                push(x & y);
            } else if (codeType.equals(MidType.CMPEQ)) {
                int x = pop();
                int y = pop();
                push(x == y ? 1 : 0);
            } else if (codeType.equals(MidType.CMPNE)) {
                int x = pop();
                int y = pop();
                push(x == y ? 0 : 1);
            } else if (codeType.equals(MidType.CMPLT)) {
                int y = pop();
                int x = pop();
                push(x < y ? 1 : 0);
            } else if (codeType.equals(MidType.CMPLE)) {
                int y = pop();
                int x = pop();
                push(x <= y ? 1 : 0);
            } else if (codeType.equals(MidType.CMPGT)) {
                int y = pop();
                int x = pop();
                push(x > y ? 1 : 0);
            } else if (codeType.equals(MidType.CMPGE)) {
                int y = pop();
                int x = pop();
                push(x >= y ? 1 : 0);
            } else if (codeType.equals(MidType.AND)) {
                boolean y = (pop() != 0);
                boolean x = (pop() != 0);
                push((x && y) ? 1 : 0);
            } else if (codeType.equals(MidType.OR)) {
                boolean y = (pop() != 0);
                boolean x = (pop() != 0);
                push((x || y) ? 1 : 0);
            } else if (codeType.equals(MidType.NOT)) {
                int x = pop();
                push((x == 0) ? 1 : 0);
            } else if (codeType.equals(MidType.NEG)) {
                int x = pop();
                push(-x);
            } else if (codeType.equals(MidType.JZ)) {
                if (stack.get(stack.size() - 1) == 0) {
                    pc = labelTable.get((String) code.getValue1());
                }
            } else if (codeType.equals(MidType.JNZ)) {
                if (stack.get(stack.size() - 1) != 0) {
                    pc = labelTable.get((String) code.getValue1());
                }
            } else if (codeType.equals(MidType.JUMP)) { // 无条件跳转
                pc = labelTable.get((String) code.getValue1());
            } else if (codeType.equals(MidType.FUNC)) {
                pc = mainAddress - 1; // 下一条就执行MAIN指令
            } else if (codeType.equals(MidType.MAIN)) {
                inGlobal = false;
                retInfos.add(new RetInfo(codes.size(), varTable, stack.size() - 1, 0, 0, 0));
                varTable = new HashMap<>();
            } else if (codeType.equals(MidType.PARA)) { //TODO:?????
                Var para = new Var(rparasIndex.get(rparasIndex.size() - callArgsNum + nowParasNum));
                int dimensions = (int) code.getValue2();
                para.setDimension(dimensions);
                if (dimensions == 2) {
                    para.setDim2(pop());
                }
                varTable.put((String) code.getValue1(), para);
                nowParasNum++;
                if (nowParasNum == callArgsNum) {
                    rparasIndex.subList(rparasIndex.size() - callArgsNum, rparasIndex.size()).clear();
                }
            } else if (codeType.equals(MidType.RPARA)) {
                int n = (int) code.getValue1(); // dimensions
                if (n == 0) {
                    rparasIndex.add(stack.size() - 1);
                } else {
                    rparasIndex.add(stack.get(stack.size() - 1)); //要的是数组的地址
                }
            } else if (codeType.equals(MidType.GETINT)) {
                push(scanner.nextInt());
            } else if (codeType.equals(MidType.CALL)) {
                Function func = funcTable.get((String) code.getValue1());
                retInfos.add(new RetInfo(pc, varTable, stack.size() - 1, func.getParasSize(), func.getParasSize(), nowParasNum));
                pc = func.getIndex();
                varTable = new HashMap<>();
                callArgsNum = func.getParasSize(); // 一个函数需要几个参数
                nowParasNum = 0; // 现在有几个参数了？
            } else if (codeType.equals(MidType.RET)) {
                int n = (int) code.getValue1();
                RetInfo info = retInfos.remove(retInfos.size() - 1);
                pc = info.getPC();
                varTable = info.getVarTable();
                // TODO:满足调用函数时参数是对其他函数的调用的情况
                callArgsNum = info.getCallArgsNum();
                nowParasNum = info.getNowParasNum();
                if (n == 1) {
                    // 留下原来栈顶的元素（返回值）
                    stack.subList(info.getStackPtr() + 1 - info.getParaNum(), stack.size() - 1).clear();
                } else {
                    stack.subList(info.getStackPtr() + 1 - info.getParaNum(), stack.size()).clear();
                }
            } else if (codeType.equals(MidType.PLACEHOLDER)) {
                Var var = getVar((String) code.getValue1());
                int dimensions = (int) code.getValue2();
                // for array: 在栈(内存)中连续存放
                // 置0，包括了全局变量不初始化默认为0的要求
                if (dimensions == 0) {
                    push(0);
                }else if (dimensions == 1) {
                    for (int i = 0; i < var.getDim1(); i++) {
                        push(0);
                    }
                } else if (dimensions == 2) {
                    for (int i = 0; i < var.getDim1() * var.getDim2(); i++) {
                        push(0);
                    }
                }
            } else if (codeType.equals(MidType.VALUE)) {
                Var var = getVar((String) code.getValue1());
                int dimensions = (int) code.getValue2();
                int address = getAddress(var, dimensions);
                push(stack.get(address));
            } else if (codeType.equals(MidType.ADDRESS)) {
                Var var = getVar((String) code.getValue1());
                int dimensions = (int) code.getValue2();
                int address = getAddress(var, dimensions);
                push(address);
            } else if (codeType.equals(MidType.PRINT)) {
                String s = (String) code.getValue1();
                int paraNum = (int) code.getValue2();
                StringBuilder builder = new StringBuilder();
                ArrayList<Integer> paras = new ArrayList<>();
                int cnt = paraNum - 1;
                for (int i = 0; i < paraNum; i++) {
                    paras.add(pop());
                }
                for (int i = 0; i < s.length(); i++) {
                    if (i + 1 < s.length()) {
                        if (s.charAt(i) == '%' && s.charAt(i + 1) == 'd') {
                            builder.append(paras.get(cnt--).toString());
                            i++;
                            continue;
                        }
                    }
                    // 不是%d的就原样输出
                    builder.append(s.charAt(i));
                }
                list.add(builder.substring(1, builder.length() - 1));
            } else if (codeType.equals(MidType.DIMVAR)) {
                Var var = getVar((String) code.getValue1());
                int dimensions = (int) code.getValue2();
                var.setDimension(dimensions);
                if (dimensions == 1) {
                    int i = pop();
                    var.setDim1(i);
                    oneDimArrInit = true;
                    oneDimArrleft = i;
                    oneDimArrCnt = 0;
                    oneDimArr = (String) code.getValue1();
                } else if (dimensions == 2) {
                    int j = pop();
                    var.setDim2(j);
                    int i = pop();
                    var.setDim1(i);
                }
            } else if (codeType.equals(MidType.EXIT)) {
                return;
            }
        }
    }

    private int getAddress(Var var, int now) { // now: 现在的[]
        int address = 0;
        // 如果是数组的话，address存的是数组首地址
        int realDimensions = var.getDimension() - now;
        if (realDimensions == 0) {
            address = var.getIndex();
        } else if (realDimensions == 1) {
            int i = pop();
            if (var.getDimension() == 1) {
                address = var.getIndex() + i; // 在栈中的位置
            } else { //int a[][]   a[1]
                address = var.getIndex() + var.getDim2() * i;
            }
        }else if (realDimensions == 2) {
            int j = pop();
            int i = pop();
            address = var.getIndex() + var.getDim2() * i + j;
        }
        return address;
    }

    public void print() throws IOException {
        for (String s : list) {
            System.out.print(s);
        }
    }

    public ArrayList<String> getprintList() {
        return list;
    }
}
