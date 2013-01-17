package address.resolution;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

public class GoogleTranslateor {
    
    static Logger log = Logger.getLogger(GoogleTranslateor.class);
 
	public static String translate(String addressGBK) {
	    
	    String urlEncodedAddress;
        try {
            urlEncodedAddress = URLEncoder.encode(addressGBK, "UTF8");
        } catch (UnsupportedEncodingException e) {
            log.error("对中文地址进行URLEncoder时异常", e);
            return null;
        }
        
        String googleURI = "http://translate.google.cn/translate_a/t?client=t&text=" + urlEncodedAddress + 
                "&hl=zh-CN&sl=zh-CN&tl=en&ie=UTF-8&oe=UTF-8&multires=1&otf=1&ssel=0&tsel=0&otf=1&pc=1&ssel=0&tsel=0&sc=1";
        
        HttpClient client = new HttpClient(); 
        GetMethod method = new GetMethod(googleURI);
        method.setRequestHeader("REQUEST-TYPE", "text/javascript");
	    
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
                String responseContent = buffer.toString();
//                log.debug("google translate response content: " + responseContent);
                
                int beginIndex = responseContent.indexOf("\"") + 1;
                int endIndex   = responseContent.indexOf("\"", beginIndex);
                if( endIndex > beginIndex ) {
                    String result = responseContent.substring(beginIndex, endIndex);
                    log.debug("google translate result: " + result);
                    return result;
                }
            } 
            else {
                log.error("请求连接失败");
            }
        } catch(Exception e) {
            log.error("调用Google翻译时异常", e);
        } finally {
            method.releaseConnection();
        }
        
        return null;
	}
}
