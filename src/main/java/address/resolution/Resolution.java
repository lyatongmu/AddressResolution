package address.resolution;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.Loader;

public class Resolution {
    
    static Logger log = Logger.getLogger(Resolution.class);
    
    public void resolveHistoryData() {
        
        List<Address> addressList = new ArrayList<Address>();
        
        URL historyDataURL = getResourceFileUrl("history.data");
        
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(historyDataURL.getFile())));
            String data = null;
            while((data = br.readLine()) != null) {
                String[] infos = data.split("|");
                if(infos.length == 2) {
                    Address address = new Address(infos[0], infos[1]);
                    addressList.add(address);
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            log.error("找不到指定的文件", e);
        } catch (IOException e) {
            log.error("读取文件内容时IO异常", e);
        }
        
        if( addressList.size() > 0 ) {
            LuceneIndexing.createIndex(addressList);
        }
    }
    
    static URL getResourceFileUrl(String file) {
        URL url = Loader.getResource(file);
        if (url == null) {
            url = ClassLoader.class.getResource(file);
        }
        return url;
    }
    
}
