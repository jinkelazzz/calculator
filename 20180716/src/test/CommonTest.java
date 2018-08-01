package test;

import org.junit.Test;

import java.io.File;
import java.time.LocalDate;

public class CommonTest {
    @Test
    public void test() {
        String fileName = "getWindData.R";
        File path = new File("");
        String filePath = path.getAbsolutePath() + "\\" + fileName;
        System.out.println(new File(filePath).exists());
    }
}
