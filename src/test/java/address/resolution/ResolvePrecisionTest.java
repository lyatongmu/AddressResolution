package address.resolution;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import junit.framework.TestCase;

/**
 * 测试解析功能的准确率
 */
public class ResolvePrecisionTest extends TestCase {
    
    static Logger log = Logger.getLogger(ResolvePrecisionTest.class);
    
    public void setUp() {
        LuceneIndexing.log.setLevel(Level.INFO);
        GoogleTranslateor.log.setLevel(Level.INFO);
    }
 
    public void testResolvePrecision() {
        List<Address> addressList = Resolution.getHistotyData();
        if(addressList.isEmpty()) return;
        
        Resolution.resolveHistoryData(addressList);
        
        List<Address> testList = addressList.subList(0, addressList.size() / 10); // 取十分之一的历史地址做下试验
        
        ThreadPool threadpool = ThreadPool.getInstance();
        ThreadPool.count = 0;
        for(final Address address : testList) {
            threadpool.excute(new Task() {
                public void excute() {
                    String tempAddress = address.addressGBK.replaceFirst("市", "");
                    String result = LuceneIndexing.query(tempAddress, false);
                   
                    if(address.expressDept != null && address.expressDept.equals(result)) {
                        ThreadPool.count ++;
                    } 
                    else {
                        log.info("该地址映射不成功 ：" + address.addressGBK + ", " + address.expressDept + ", " + result);
                    }
                }
            });
        }
        
        while(ThreadPool.count < testList.size()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        
        log.info("共尝试了【" + testList.size() + "】个地址，成功匹配了【" + ThreadPool.count + "】个。");
        log.info("解析准确率 = " + (ThreadPool.count / testList.size())*100 + "%");
    }
}
