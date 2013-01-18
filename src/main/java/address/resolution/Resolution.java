package address.resolution;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.Loader;

/**
 * 解析历史数据，建立索引
 */
public class Resolution {
    
    static Logger log = Logger.getLogger(Resolution.class);
    
    static String HISTORY_DATA = "history.data";
    
    public static void resolveHistoryData() {
        List<Address> addressList = getHistotyData();
        resolveHistoryData(addressList);
    }
    
    public static void resolveHistoryData(List<Address> addressList) {
        if( addressList.size() > 0 ) {
            LuceneIndexing.createIndex(addressList);
        }
    }
    
    /**
     * 历史数据庞大的话，使用线程池并发处理以改善性能。
     * 在同一时刻，lucene索引中只允许有一个进程对其进行加入文档，删除文档，更新索引等操作。
     * 对历史数据按省市进行分类，分开建索引，则可并发进行。
     * 
     * @param addressList
     */
    public static void resolveHistoryDataII(List<Address> addressList) {
        
        if( addressList.isEmpty() ) return;
        
        Map<Integer, List<Address>> areaMap = new HashMap<Integer, List<Address>>();
        for(Address address : addressList) {
            if(address.addressCN == null || address.addressCN.length() < 4) {
                continue;
            }
            
            String area = address.addressCN.substring(0, 3);
            int areaHashCode = area.hashCode();
            List<Address> areaList = areaMap.get(areaHashCode);
            if (areaList == null) {
                areaMap.put(areaHashCode, areaList = new ArrayList<Address>());
            }
            areaList.add(address);
        }
        
        for(Entry<Integer, List<Address>> entry : areaMap.entrySet()) {
            int areaHashCode = entry.getKey();
            final List<Address> areaList = entry.getValue();
            final String indexPath = LuceneIndexing.INDEX_FILE_PATH + "/" + areaHashCode;
            
            ThreadPool threadpool = ThreadPool.getInstance();
            ThreadPool.COUNT = 0;
            threadpool.excute(new Task() {
                public void excute() {
                    LuceneIndexing.createIndex(areaList, true, indexPath);
                }
            });
        }
    }
    
    public static List<Address> getHistotyData() {
        List<Address> addressList = new ArrayList<Address>();
        
        try {
            URL historyDataURL = getResourceFileUrl(HISTORY_DATA);
            FileInputStream fileInputStream = new FileInputStream(historyDataURL.getFile());
            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream, "UTF-8"));
            String data = null;
            while((data = br.readLine()) != null) {
                String[] infos = data.split("\\|");
                if(infos.length == 2) {
                    Address address = new Address(infos[0].trim(), infos[1].trim());
                    addressList.add(address);
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            log.error("找不到指定的文件", e);
        } catch (IOException e) {
            log.error("读取文件内容时IO异常", e);
        }
        return addressList;
    }
    
    static URL getResourceFileUrl(String file) {
        URL url = Loader.getResource(file);
        if (url == null) {
            url = ClassLoader.class.getResource(file);
        }
        return url;
    }
}
