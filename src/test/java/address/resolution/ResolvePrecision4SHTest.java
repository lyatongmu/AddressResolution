package address.resolution;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        LuceneIndexing.INDEX_FILE_PATH = "D:/temp/address/sh_index";
    }
 
    public void testResolvePrecision() {
        // 1. 解析历史数据（一次就行了，以后都用已经解析好的结果）
        if( !new File(LuceneIndexing.INDEX_FILE_PATH).exists() ) {
            List<Address> historyList = Resolution.getHistotyData("sh_history.data");
            
            GoogleTranslator.RESULT_FILE_DIR = "D:/temp/address/sh_en/";
            GoogleTranslator.init();
            
            Resolution.resolveHistoryDataII(historyList);
            
            // 等待多线程完成
            while(ThreadPool.COUNT < historyList.size()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
        
        // 2.翻译测试数据（一次就行了，以后都用已经解析好的结果）
        GoogleTranslator.RESULT_FILE_DIR = "D:/temp/address/sh_en/test"; // 切换目录
        if( !new File(GoogleTranslator.ADDR_MAPPED_PATH()).exists() ) {
            GoogleTranslator.clear();
            
            List<Address> testList = Resolution.getHistotyData("sh_test.data");
            Map<String, List<Address>> areaMap = Resolution.sortAddressList(testList);
            
            ThreadPool.COUNT = 0;
            for(final List<Address> areaList : areaMap.values()) {
                ThreadPool.getInstance().excute(new Task() {
                    public void excute() {
                        for(final Address address : areaList) {
                            ThreadPool.addCount();
                            
                            String addressEN = GoogleTranslator.translate(address.addressCN, true);
                            if(GoogleTranslator.REPEAT_ADDR_TAG.equals(addressEN)) {
                                continue; // 重复的地址不要
                            }
                            
                            String content = address.addressCN + " | " + address.expressDept + " | " + addressEN;
                            GoogleTranslator.output2File(content, GoogleTranslator.ADDR_MAPPED_PATH());
                            
                            if ((ThreadPool.COUNT > 0 && ThreadPool.COUNT % 100 == 0)) {
                                log.info("已尝试翻译【" + ThreadPool.COUNT + "】个地址。");
                            }
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
        }
        
        // 3.将测试数据作为输入进行匹配测试
        List<Address> testList = Resolution.getMappedData(GoogleTranslator.ADDR_MAPPED_PATH());
        testList = testList.subList(0, 10000);
        
        List<Address> faultList = excuteQuery(testList, 1);
        for(final Address address : faultList) {
            address.addressEN = address.addressEN.replaceAll("Room [0-9]*", " ");  // 去掉地址中的房间号
        }
        
        faultList = excuteQuery(faultList, 1);
        
        faultList = excuteQuery(faultList, 2);
        
        faultList = excuteQuery(faultList, 3);
    }

    /**
     * @param testList
     * @param mappedDeptNum TODO 可设置返回结果个数
     * @return
     */
    private List<Address> excuteQuery(List<Address> testList, final int mappedDeptNum) {
        final List<Address> faultList = new ArrayList<Address>();
        
        ThreadPool.COUNT = 0;
        ThreadPool.FAULT_COUNT = 0;
        ThreadPool.SUCCESS_COUNT = 0;
        for(final Address address : testList) {
            String area = address.addressCN.substring(0, 3);
            final int areaHashCode = area.hashCode();
            
            ThreadPool.getInstance().excute(new Task() {
                public void excute() {
                    // 在area对应的索引目录下检索
                    String indexPath = LuceneIndexing.INDEX_FILE_PATH + "/" + areaHashCode; 
                    String result = LuceneIndexing.query(address, indexPath, mappedDeptNum); 
                    List<String> mappedDepts = Arrays.asList( result == null ? new String[] {} : result.split(",") );
                    
                    boolean matchSuccess = mappedDepts.contains(address.expressDept);
                    if(matchSuccess) {
                        ThreadPool.addSuccessCount();
                        ThreadPool.addCount();
                        if ((ThreadPool.COUNT > 0 && ThreadPool.COUNT % 1000 == 0)) {
                            log.info("已尝试映射【" + ThreadPool.COUNT + "】个地址。");
                        }
                    } 
                    else {
                        ThreadPool.addFaultCount();
                        faultList.add(address);
                        log.info("映射不成功 ：" + address.addressCN + "【历史：" + address.expressDept + ",  匹配：" + result + "】");
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
        
        log.info("共尝试【" + total + "】个地址，成功匹配【" + (ThreadPool.SUCCESS_COUNT) + "】个，失败【" + ThreadPool.FAULT_COUNT + "】个。");
        log.info("解析准确率 = " + percent + "%");
        
        return faultList;
    }
}
