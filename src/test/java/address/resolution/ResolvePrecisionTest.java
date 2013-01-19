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
        
        ThreadPool.THREAD_INIT_NUM = 50;
        Resolution.HISTORY_DATA = "sh_history.data";
        LuceneIndexing.INDEX_FILE_PATH = "D:/temp/address/sh_index";
    }
 
    public void testResolvePrecision() {
        List<Address> addressList = Resolution.getHistotyData();
        if(addressList.isEmpty()) return;
        
        int index = (int)(addressList.size() * 0.99);
        List<Address> historyList = addressList.subList(0, index); // 取99%的历史地址做下试验
        Resolution.resolveHistoryDataII(historyList);
        
        // 等待多线程完成
        while(ThreadPool.COUNT < historyList.size()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        
        List<Address> testList = addressList.subList(index + 1, addressList.size()); // 取 1%的历史地址做下试验
        ThreadPool threadpool = ThreadPool.getInstance();
        ThreadPool.COUNT = 0;
        for(final Address address : testList) {
            threadpool.excute ( new Task() {
                public void excute() {
                    String tempAddress = address.addressCN;
                    if(tempAddress == null || tempAddress.length() < 4) {
                        return;
                    }
                    
                    String area = tempAddress.substring(0, 3);
                    String result = LuceneIndexing.query(tempAddress, false, area.hashCode());
                   
                    if((address.expressDept != null && address.expressDept.equals(result)) 
                    		|| GoogleTranslateor.REPEAT_ADDR_TAG.equals(result)) {
                    	
                        ThreadPool.addCount();
                        if ((ThreadPool.COUNT > 0 && ThreadPool.COUNT % 100 == 0)) {
                            log.info("已尝试【" + ThreadPool.COUNT + "】个地址。");
                        }
                    } 
                    else {
                        ThreadPool.addFaultCount();
                        log.info("该地址映射不成功 ：" + address.addressCN + "【历史：" + address.expressDept + ",  匹配：" + result + "】");
                    }
                }
            });
        }
        
        // 等待多线程完成
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
