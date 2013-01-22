package address.resolution;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

public class ExtractChineseWordsTest extends TestCase {
    
    static Logger log = Logger.getLogger(ExtractChineseWordsTest.class);
    
    public void setUp() {
        ThreadPool.THREAD_INIT_NUM = 12;
    }
    
    public void testExtractChineseWords() {
        
        // 翻译地址
        GoogleTranslator.translateAddrsInFile("D:/temp/address/sh/sh_address.data");
        
        // 抽取中文词
        ExtractChineseWords.extract(GoogleTranslator.RESULT_FILE_DIR);
    }
}
