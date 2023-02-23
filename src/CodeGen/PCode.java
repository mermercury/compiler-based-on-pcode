package CodeGen;

public class PCode {
    private MidType type;
    private Object value1 = null;
    private Object value2 = null;

    public PCode(MidType type) {
        this.type = type;
    }

    public PCode(MidType type, Object value1) {
        this.type = type;
        this.value1 = value1;
    }

    public PCode(MidType type, Object value1, Object value2) {
        this.type = type;
        this.value1 = value1;
        this.value2 = value2;
    }

    public MidType getType() {
        return type;
    }

    public void setType(MidType type) {
        this.type = type;
    }

    public Object getValue1() {
        return value1;
    }

    public void setValue1(Object value1) {
        this.value1 = value1;
    }

    public Object getValue2() {
        return value2;
    }

    public void setValue2(Object value2) {
        this.value2 = value2;
    }
}
