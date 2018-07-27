package test;

import option.BaseSingleOption;
import org.junit.Test;
import underlying.gbm.BaseUnderlying;

import java.util.ArrayList;
import java.util.List;

public class CommonTest {
    @Test
    public void test() {
        List[] lists = new ArrayList[2];
        List<BaseUnderlying> underlyingList = new ArrayList<>();
        List<BaseSingleOption> optionList = new ArrayList<>();
        lists[0] = underlyingList;
        lists[1] = optionList;
    }
}
