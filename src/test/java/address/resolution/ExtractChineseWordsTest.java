package address.resolution;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

public class ExtractChineseWordsTest extends TestCase {
    
    static Logger log = Logger.getLogger(ExtractChineseWordsTest.class);
    
    public void setUp() {
        ThreadPool.THREAD_INIT_NUM = 12;
    }
    
    public void testExtractChineseWords() {
        // 翻译地址 并 抽取中文词, 输出文件为 words.data
        ExtractChineseWords.extract("D:/temp/address/hz/address.data");
    }
}
