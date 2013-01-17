package address.resolution;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

public class GoogleTranslateor {
    
    static Logger log = Logger.getLogger(GoogleTranslateor.class);

	public static String translate(String addressGBK) {
		
//	    return " Hangzhou, Zhejiang Tianmu road No. 176 (West Lake Source Software Park), Building 18 ";
	    
	    String encodeAddress = "%E6%B5%99%E6%B1%9F%E7%9C%81%E6%9D%AD%E5%B7%9E%E5%B8%82%20%E5%A4%A9%E7%9B%AE%E4%B8%8A%E8%B7%AF176%E5%8F%B7%EF%BC%88%E8%A5%BF%E6%B9%96%E6%95%B0%E6%BA%90%E8%BD%AF%E4%BB%B6%E5%9B%AD%EF%BC%8918%E5%8F%B7%E6%A5%BC";
	
        HttpClient client = new HttpClient(); 
        GetMethod method = new GetMethod("http://translate.google.cn/translate_a/t?client=t&text=" + encodeAddress + 
        		"&hl=zh-CN&sl=zh-CN&tl=en&ie=UTF-8&oe=UTF-8&multires=1&otf=1&ssel=0&tsel=0&otf=1&pc=1&ssel=0&tsel=0&sc=1");
        method.setRequestHeader("REQUEST-TYPE", "text/javascript");
        
        excuteRequest(client, method);
        
        return null;
	}
	
	static void excuteRequest(HttpClient client, GetMethod method) {
        try {
            client.executeMethod(method);
            int statusCode = method.getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = in.readLine()) != null){
                  buffer.append(line);
                }
                System.out.println("response:" + buffer.toString());
            } else {
                log.error("请求连接失败");
            }
        } catch(Exception e) {
            log.error("执行HTTP请求异常", e);
        } finally {
            method.releaseConnection();
        }
    }

}
