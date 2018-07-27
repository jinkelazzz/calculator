package test;

import org.junit.Test;

import java.util.*;

class A {
    private B b;
    private int value = 1;

    public B getB() {
        return b;
    }

    public void setB(B b) {
        this.b = b;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        A a = (A) o;
        return value == a.value &&
                Objects.equals(b, a.b);
    }

    @Override
    public int hashCode() {

        return Objects.hash(b, value);
    }


}

class B {
    private C c;
    private int value = 1;

    public C getC() {
        return c;
    }

    public void setC(C c) {
        this.c = c;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B b = (B) o;
        return value == b.value &&
                Objects.equals(c, b.c);
    }

    @Override
    public int hashCode() {

        return Objects.hash(c, value);
    }


}


class C {
    private int value = 1;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        C c = (C) o;
        return value == c.value;
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }


}

public class MapTest {
    private static B b = new B();
    private static C c = new C();

    private static A getA() {
        b.setC(c);
        A a = new A();
        a.setB(b);
        return a;
    }

    private void printTest(Map<A, String> map, A a) {
        System.out.println("-----test begin-----");
        System.out.println("map size: " + map.size());
        System.out.println("a to string: " + a);
        System.out.println("-----Java 8 loop begin-----");
        map.forEach((key, value) -> {
            System.out.println("-----single loop begin-----");
            System.out.println("keys hash code: " + key.hashCode());
            System.out.println("keys to string: " + key.toString());
            System.out.println("key equals a? : " + key.equals(a));
            System.out.println("value of key: " + map.get(key));
            System.out.println("map contains key? : " + map.containsKey(key));
            System.out.println("value: " + value);
            System.out.println("-----single loop end-----");
        });
        System.out.println("-----Java 8 loop end-----");
        System.out.println("-----old loop begin-----");
        Iterator iter = map.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            System.out.println("-----single loop begin-----");
            System.out.println("keys hash code: " + entry.getKey().hashCode());
            System.out.println("keys to string: " + entry.getKey().toString());
            System.out.println("key equals a? : " + entry.getKey().equals(a));
            System.out.println("value of key: " + map.get(entry.getKey()));
            System.out.println("map contains key? : " + map.containsKey(entry.getKey()));
            System.out.println("-----single loop end-----");
        }
        System.out.println("-----test end-----");
    }

    @Test
    public void test() {
        Map<A, String> map = new HashMap<>(16);
        A a = getA();
        map.put(a, "1");
        printTest(map, a);
        b.setValue(2);
        printTest(map, a);
        map.put(a, "2");
        printTest(map, a);
    }

}
