package address.resolution;

import java.text.DecimalFormat;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

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
        ThreadPool.COUNT = 0;
        for(final Address address : testList) {
            threadpool.excute(new Task() {
                public void excute() {
                    String tempAddress = address.addressGBK;
//                    tempAddress = tempAddress.replaceFirst("市", ""); // 稍做处理
                    String result = LuceneIndexing.query(tempAddress, false);
                   
                    if(address.expressDept != null && address.expressDept.equals(result)) {
                        ThreadPool.addCount();
                        if ((ThreadPool.COUNT > 0 && ThreadPool.COUNT % 100 == 0)) {
                            log.info("已尝试【" + ThreadPool.COUNT + "】个地址。");
                        }
                    } 
                    else {
                        ThreadPool.addFaultCount();
                        log.info("该地址映射不成功 ：" + address.addressGBK + "【历史：" + address.expressDept + ", 匹配：" + result + "】");
                    }
                }
            });
        }
        
        while(ThreadPool.COUNT + ThreadPool.FAULT_COUNT < testList.size()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        
        DecimalFormat df = new DecimalFormat("##.####");
        String percent = df.format(100D * ThreadPool.COUNT / testList.size());
        
        log.info("共尝试了【" + testList.size() + "】个地址，成功匹配了【" + ThreadPool.COUNT + "】个。");
        log.info("解析准确率 = " + percent + "%");
    }
}
