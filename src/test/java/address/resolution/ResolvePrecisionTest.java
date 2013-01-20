package address.resolution;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
        GoogleTranslator.log.setLevel(Level.INFO);
        
//        ThreadPool.THREAD_INIT_NUM = 12;
//        Resolution.HISTORY_DATA = "sh_history.data";
//        LuceneIndexing.INDEX_FILE_PATH = "D:/temp/address/sh_index";
    }
 
    public void testResolvePrecision() {
        List<Address> addressList = Resolution.getHistotyData();
        if(addressList.isEmpty()) return;
        
        int index = (int)(addressList.size() * 0.9);
        List<Address> historyList = addressList.subList(0, index); // 取90%的历史地址做下试验
        Resolution.resolveHistoryDataII(historyList);
        
        // 等待多线程完成
        while(ThreadPool.COUNT < historyList.size()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        
        List<Address> testList = addressList.subList(index + 1, addressList.size()); // 取 1%的历史地址做下试验
        _testQuery(testList);
    }

	static void _testQuery(List<Address> testList) {
		Map<String, List<Address>> areaMap = Resolution.sortAddressList(testList);
        
        ThreadPool threadpool = ThreadPool.getInstance();
        ThreadPool.COUNT = 0;
        for(Entry<String, List<Address>> entry : areaMap.entrySet()) {
        	final int areaHashCode = entry.getKey().hashCode();
            final List<Address> areaList = entry.getValue();
            
            threadpool.excute(new Task() {
                public void excute() {
                    for(final Address address : areaList) {
                    	// 在area对应的索引目录下检索
                    	String indexPath = LuceneIndexing.INDEX_FILE_PATH + "/" + areaHashCode;
                    	address.addressEN = GoogleTranslator.translate(address.addressCN, true);
                        String result = LuceneIndexing.query(address, indexPath);
                        
                        boolean matchSuccess = address.expressDept != null && address.expressDept.equals(result);
						if(matchSuccess 
								|| GoogleTranslator.REPEAT_ADDR_TAG.equals(result)) {
							
                            ThreadPool.addCount();
                            if ((ThreadPool.COUNT > 0 && ThreadPool.COUNT % 100 == 0)) {
                                log.info("已尝试【" + ThreadPool.COUNT + "】个地址。");
                            }
                            
                            if( matchSuccess ) { // 为成功匹配的新地址建立索引
                            	ThreadPool.addSuccessCount();
                                LuceneIndexing.createIndex(Arrays.asList(address), indexPath);
                            }
                        } 
                        else {
                            ThreadPool.addFaultCount();
                            log.info("该地址映射不成功 ：" + address.addressCN + "【历史：" + address.expressDept + ",  匹配：" + result + "】");
                        }
                    }
                }
            });
        }
        
        // 等待多线程完成
        int total = testList.size();
		while(ThreadPool.COUNT + ThreadPool.FAULT_COUNT < total) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        
        DecimalFormat df = new DecimalFormat("##.####");
        String percent = df.format(100D * (total - ThreadPool.FAULT_COUNT) / total);
        
        log.info("共尝试【" + total + "】个地址，新成功匹配【" + (ThreadPool.SUCCESS_COUNT) + "】个，失败【" + ThreadPool.FAULT_COUNT + "】个。");
        log.info("解析准确率 = " + percent + "%");
	}
}
