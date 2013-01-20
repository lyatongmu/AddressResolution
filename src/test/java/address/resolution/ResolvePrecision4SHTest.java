package address.resolution;

import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * 测试解析功能的准确率
 */
public class ResolvePrecision4SHTest extends TestCase {
    
    static Logger log = Logger.getLogger(ResolvePrecision4SHTest.class);
    
    public void setUp() {
        LuceneIndexing.log.setLevel(Level.INFO);
        GoogleTranslator.log.setLevel(Level.INFO);
        
        ThreadPool.THREAD_INIT_NUM = 12;
        Resolution.HISTORY_DATA = "sh_history.data";
        LuceneIndexing.INDEX_FILE_PATH = "D:/temp/address/sh_index";
    }
 
    public void testResolvePrecision() {
        List<Address> addressList = Resolution.getHistotyData();
        if(addressList.isEmpty()) return;
        
        int index = (int)(addressList.size() * 0.9);
        List<Address> historyList = addressList.subList(451310, index); 
        Resolution.resolveHistoryDataII(historyList);
        
        // 等待多线程完成
        while(ThreadPool.COUNT < historyList.size()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        
        List<Address> testList = addressList.subList(index, index + 1000); 
        ResolvePrecisionTest._testQuery(testList);
    }
}
